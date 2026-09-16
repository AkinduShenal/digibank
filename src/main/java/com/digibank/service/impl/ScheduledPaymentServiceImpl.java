package com.digibank.service.impl;

import com.digibank.dto.schedule.*;
import com.digibank.entity.*;
import com.digibank.enums.*;
import com.digibank.exception.ScheduledPaymentException;
import com.digibank.repository.*;
import com.digibank.service.ScheduledPaymentService;
import com.digibank.util.SensitiveDataMasker;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;
import java.util.*;

@Service
public class ScheduledPaymentServiceImpl implements ScheduledPaymentService {
	private final ScheduledPaymentRepository schedules;
	private final CustomerRepository customers;
	private final BankAccountRepository accounts;
	private final BeneficiaryRepository beneficiaries;
	private final SavedBillerRepository billers;
	private final AuditLogRepository auditLogs;
	private final PasswordEncoder passwordEncoder;
	private final SensitiveDataMasker masker;

	public ScheduledPaymentServiceImpl(ScheduledPaymentRepository schedules, CustomerRepository customers,
			BankAccountRepository accounts, BeneficiaryRepository beneficiaries, SavedBillerRepository billers,
			AuditLogRepository auditLogs, PasswordEncoder passwordEncoder, SensitiveDataMasker masker) {
		this.schedules=schedules; this.customers=customers; this.accounts=accounts; this.beneficiaries=beneficiaries;
		this.billers=billers; this.auditLogs=auditLogs; this.passwordEncoder=passwordEncoder; this.masker=masker;
	}

	@Override @Transactional
	public ScheduledPaymentView create(Long userId, String actor, ScheduledPaymentRequest request) {
		Customer customer=customer(userId);
		validateAmount(request.getAmount(), request.getPaymentType());
		validateDates(request.getNextExecutionAt(), request.getEndDate(), request.getRecurrence());
		if (!passwordEncoder.matches(request.getTransactionPin(), customer.getUser().getTransactionPinHash()))
			throw new ScheduledPaymentException("Incorrect transaction PIN.");
		BankAccount source=account(customer, request.getSourceAccountNumber());
		String reference=newReference();
		ScheduledPayment schedule=new ScheduledPayment(customer, source, reference, request.getPaymentType(),
				request.getRecurrence(), request.getNextExecutionAt(), request.getRecurrence()==ScheduleRecurrence.MONTHLY ? request.getEndDate() : null, request.getAmount(), clean(request.getDescription()));
		if (request.getPaymentType()==ScheduledPaymentType.FUND_TRANSFER) configureTransfer(schedule, customer, source, request);
		else if (request.getPaymentType()==ScheduledPaymentType.BILL_PAYMENT) configureBill(schedule, customer, request);
		else throw new ScheduledPaymentException("Select a scheduled payment type.");
		schedules.save(schedule);
		auditLogs.save(new AuditLog(actor, "SCHEDULED_PAYMENT_CREATED", "SCHEDULED_PAYMENT", reference,
				null, ScheduleStatus.SCHEDULED.name(), null, LocalDateTime.now()));
		return view(schedule);
	}

	@Override @Transactional(readOnly=true)
	public List<ScheduledPaymentView> list(Long userId) {
		return schedules.findByCustomerUserIdOrderByCreatedAtDesc(userId).stream().map(this::view).toList();
	}

	@Override @Transactional(readOnly=true)
	public ScheduledPaymentView get(Long userId, String reference) { return view(schedule(userId, reference)); }

	@Override @Transactional(readOnly=true)
	public List<ScheduledPaymentView> list(Long userId, ScheduledPaymentType type) {
		return schedules.findByCustomerUserIdAndPaymentTypeAndStatusNotOrderByCreatedAtDesc(
				userId, type, ScheduleStatus.CANCELLED).stream().map(this::view).toList();
	}

	@Override @Transactional(readOnly=true)
	public ScheduledPaymentUpdateRequest getUpdateRequest(Long userId, String reference) {
		ScheduledPayment s=schedule(userId, reference); requireEditable(s);
		ScheduledPaymentUpdateRequest r=new ScheduledPaymentUpdateRequest();
		r.setAmount(s.getAmount()); r.setRecurrence(s.getRecurrence()); r.setNextExecutionAt(s.getNextExecutionAt());
		r.setEndDate(s.getEndDate()); r.setDescription(s.getDescription()); return r;
	}

	@Override @Transactional
	public void update(Long userId, String actor, String reference, ScheduledPaymentUpdateRequest request) {
		ScheduledPayment s=lockedSchedule(userId, reference); requireEditable(s);
		validateAmount(request.getAmount(), s.getPaymentType());
		validateDates(request.getNextExecutionAt(), request.getEndDate(), request.getRecurrence());
		s.update(request.getAmount(), clean(request.getDescription()), request.getRecurrence(), request.getNextExecutionAt(),
				request.getRecurrence()==ScheduleRecurrence.MONTHLY ? request.getEndDate() : null);
		auditLogs.save(new AuditLog(actor, "SCHEDULED_PAYMENT_UPDATED", "SCHEDULED_PAYMENT", reference,
				ScheduleStatus.SCHEDULED.name(), ScheduleStatus.SCHEDULED.name(), null, LocalDateTime.now()));
	}

	@Override @Transactional
	public void cancel(Long userId, String actor, String reference) {
		ScheduledPayment s=lockedSchedule(userId, reference); requireEditable(s); s.cancel(LocalDateTime.now());
		auditLogs.save(new AuditLog(actor, "SCHEDULED_PAYMENT_CANCELLED", "SCHEDULED_PAYMENT", reference,
				ScheduleStatus.SCHEDULED.name(), ScheduleStatus.CANCELLED.name(), "Cancelled by customer", LocalDateTime.now()));
	}

	private void configureTransfer(ScheduledPayment s, Customer c, BankAccount source, ScheduledPaymentRequest r) {
		TransferRecipientType type=r.getTransferRecipientType();
		if (type==null) throw new ScheduledPaymentException("Select a transfer recipient.");
		Beneficiary b=null; String destination;
		if (type==TransferRecipientType.SAVED_BENEFICIARY) {
			if (r.getBeneficiaryId()==null) throw new ScheduledPaymentException("Select a saved beneficiary.");
			b=beneficiaries.findByIdAndCustomerIdAndStatusNot(r.getBeneficiaryId(), c.getId(), BeneficiaryStatus.DELETED)
					.orElseThrow(() -> new ScheduledPaymentException("Saved beneficiary was not found."));
			if (b.getStatus()!=BeneficiaryStatus.ACTIVE || b.getVerificationStatus()!=BeneficiaryVerificationStatus.VERIFIED)
				throw new ScheduledPaymentException("Beneficiary must be active and verified.");
			destination=b.getNormalizedAccountNumber();
		} else if (type==TransferRecipientType.OWN_ACCOUNT) {
			destination=normalizeAccount(r.getOwnDestinationAccountNumber());
			BankAccount target=account(c, destination);
			if (target.getId().equals(source.getId())) throw new ScheduledPaymentException("Choose a different destination account.");
		} else {
			destination=normalizeAccount(r.getDestinationAccountNumber());
			if (!destination.matches("\\d{12}")) throw new ScheduledPaymentException("Enter a valid 12-digit DigiBank account number.");
			BankAccount target=accounts.findByAccountNumber(destination)
					.orElseThrow(() -> new ScheduledPaymentException("Recipient account is not available for transfers."));
			if (target.getAccountStatus()!=AccountStatus.ACTIVE || target.getCurrencyCode()!=CurrencyCode.LKR
					|| target.getCustomer().getStatus()!=CustomerStatus.ACTIVE)
				throw new ScheduledPaymentException("Recipient account is not available for transfers.");
			if (target.getCustomer().getId().equals(c.getId()))
				throw new ScheduledPaymentException("Use Between my accounts to transfer to your own account.");
		}
		s.configureTransfer(type, b, destination);
	}

	private void configureBill(ScheduledPayment s, Customer c, ScheduledPaymentRequest r) {
		BillerSelectionType type=r.getBillerSelectionType();
		if (type==null) throw new ScheduledPaymentException("Select a biller.");
		SavedBiller saved=null; BillerProvider provider; String consumer;
		if (type==BillerSelectionType.SAVED_BILLER) {
			if (r.getSavedBillerId()==null) throw new ScheduledPaymentException("Select a saved biller.");
			saved=billers.findByIdAndCustomerIdAndStatus(r.getSavedBillerId(), c.getId(), SavedBillerStatus.ACTIVE)
					.orElseThrow(() -> new ScheduledPaymentException("Saved biller was not found."));
			provider=saved.getProvider(); consumer=saved.getConsumerReference();
		} else { provider=r.getBillerProvider(); consumer=clean(r.getConsumerReference()); }
		if (provider==null || consumer==null || !consumer.toUpperCase(Locale.ROOT).matches("[A-Z0-9+\\-/]{5,50}"))
			throw new ScheduledPaymentException("Enter valid biller details.");
		s.configureBill(type, saved, provider, consumer.toUpperCase(Locale.ROOT));
	}

	private Customer customer(Long userId) {
		Customer c=customers.findByUserId(userId).orElseThrow(() -> new ScheduledPaymentException("Customer profile not found."));
		if (c.getStatus()!=CustomerStatus.ACTIVE) throw new ScheduledPaymentException("Customer profile must be active."); return c;
	}
	private BankAccount account(Customer c, String number) {
		BankAccount a=accounts.findByAccountNumber(normalizeAccount(number)).orElseThrow(() -> new ScheduledPaymentException("Account not found."));
		if (!a.getCustomer().getId().equals(c.getId()) || a.getAccountStatus()!=AccountStatus.ACTIVE || a.getCurrencyCode()!=CurrencyCode.LKR)
			throw new ScheduledPaymentException("You cannot use this account."); return a;
	}
	private ScheduledPayment schedule(Long userId,String ref){return schedules.findByScheduleReferenceAndCustomerUserId(ref,userId)
			.orElseThrow(()->new ScheduledPaymentException("Scheduled payment not found."));}
	private ScheduledPayment lockedSchedule(Long userId,String ref){return schedules.findOwnedForUpdate(ref,userId)
			.orElseThrow(()->new ScheduledPaymentException("Scheduled payment not found."));}
	private void requireEditable(ScheduledPayment s){if(s.getStatus()!=ScheduleStatus.SCHEDULED)throw new ScheduledPaymentException("Only scheduled payments can be changed.");}
	private void validateAmount(java.math.BigDecimal amount, ScheduledPaymentType type) {
		java.math.BigDecimal minimum=new java.math.BigDecimal(type==ScheduledPaymentType.BILL_PAYMENT ? "10.00" : "0.01");
		if(amount==null || amount.compareTo(minimum)<0 || amount.compareTo(new java.math.BigDecimal("1000000.00"))>0 || amount.stripTrailingZeros().scale()>2)
			throw new ScheduledPaymentException("Amount must be between LKR "+minimum+" and LKR 1,000,000.00, with no more than two decimal places.");
	}
	private void validateDates(LocalDateTime next, LocalDate end, ScheduleRecurrence recurrence){
		if(recurrence==null)throw new ScheduledPaymentException("Choose how often this payment should repeat.");
		if(next==null||!next.isAfter(LocalDateTime.now()))throw new ScheduledPaymentException("Execution time must be in the future.");
		if(recurrence==ScheduleRecurrence.MONTHLY && end!=null && end.isBefore(next.toLocalDate()))throw new ScheduledPaymentException("End date cannot be before the first payment.");
	}
	private String newReference(){String r;do{r="SCH"+UUID.randomUUID().toString().replace("-","").substring(0,16).toUpperCase(Locale.ROOT);}while(schedules.existsByScheduleReference(r));return r;}
	private String normalizeAccount(String v){return v==null?"":v.replaceAll("\\s+","");}
	private String clean(String v){return v==null?null:v.trim();}
	private ScheduledPaymentView view(ScheduledPayment s){
		String destination=s.getPaymentType()==ScheduledPaymentType.FUND_TRANSFER
				?masker.maskAccountNumber(s.getDestinationAccountNumber())
				:s.getBillerProvider().getDisplayName()+" · "+s.getConsumerReference();
		return new ScheduledPaymentView(s.getScheduleReference(),s.getPaymentType(),s.getStatus(),s.getRecurrence(),
				masker.maskAccountNumber(s.getSourceAccount().getAccountNumber()),destination,s.getAmount(),s.getDescription(),
				s.getNextExecutionAt(),s.getEndDate(),s.getExecutionCount(),s.getLastExecutionReference(),s.getLastExecutedAt(),s.getFailureReason());
	}
}
