package com.digibank.service.impl;

import com.digibank.dto.bill.BillPaymentRequest;
import com.digibank.dto.transfer.TransferRequest;
import com.digibank.entity.*;
import com.digibank.enums.*;
import com.digibank.repository.*;
import com.digibank.service.*;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Component
@ConditionalOnBean(ScheduledPaymentRepository.class)
public class ScheduledPaymentProcessor {
	private final ScheduledPaymentRepository schedules;
	private final TransferService transfers;
	private final BillPaymentService bills;
	private final CustomerNotificationRepository notifications;
	private final AuditLogRepository auditLogs;

	public ScheduledPaymentProcessor(ScheduledPaymentRepository schedules, TransferService transfers,
			BillPaymentService bills, CustomerNotificationRepository notifications, AuditLogRepository auditLogs) {
		this.schedules=schedules; this.transfers=transfers; this.bills=bills; this.notifications=notifications; this.auditLogs=auditLogs;
	}

	@Transactional(propagation=Propagation.REQUIRES_NEW)
	public void execute(Long id) {
		ScheduledPayment s=schedules.findByIdForUpdate(id).orElse(null);
		if(s==null || s.getStatus()!=ScheduleStatus.SCHEDULED || s.getNextExecutionAt().isAfter(LocalDateTime.now()))return;
		s.processing();
		try {
			String executionReference=s.getPaymentType()==ScheduledPaymentType.FUND_TRANSFER
					?transfers.executeScheduledTransfer(s.getCustomer().getUser().getId(), transferRequest(s)).referenceNumber()
					:bills.executeScheduledPayment(s.getCustomer().getUser().getId(), billRequest(s)).referenceNumber();
			s.succeeded(executionReference,LocalDateTime.now());
			notifications.save(new CustomerNotification(s.getCustomer().getUser(),NotificationType.ACCOUNT_NOTICE,
					"Scheduled payment completed","Scheduled payment "+s.getScheduleReference()+" was completed successfully.",executionReference));
			auditLogs.save(new AuditLog("system-scheduler","SCHEDULED_PAYMENT_COMPLETED","SCHEDULED_PAYMENT",
					s.getScheduleReference(),ScheduleStatus.PROCESSING.name(),s.getStatus().name(),executionReference,LocalDateTime.now()));
		} catch(RuntimeException ex) {
			String reason=ex.getMessage()==null?"Payment execution failed.":ex.getMessage();
			if(reason.length()>255)reason=reason.substring(0,255);
			s.failed(reason,LocalDateTime.now());
			notifications.save(new CustomerNotification(s.getCustomer().getUser(),NotificationType.ACCOUNT_NOTICE,
					"Scheduled payment failed","Scheduled payment "+s.getScheduleReference()+" failed: "+reason,s.getScheduleReference()));
			auditLogs.save(new AuditLog("system-scheduler","SCHEDULED_PAYMENT_FAILED","SCHEDULED_PAYMENT",
					s.getScheduleReference(),ScheduleStatus.PROCESSING.name(),ScheduleStatus.FAILED.name(),reason,LocalDateTime.now()));
		}
	}

	private TransferRequest transferRequest(ScheduledPayment s){
		TransferRequest r=new TransferRequest(); r.setSourceAccountNumber(s.getSourceAccount().getAccountNumber());
		r.setRecipientType(s.getTransferRecipientType()); r.setBeneficiaryId(s.getBeneficiary()==null?null:s.getBeneficiary().getId());
		if(s.getTransferRecipientType()==TransferRecipientType.OWN_ACCOUNT)r.setOwnDestinationAccountNumber(s.getDestinationAccountNumber());
		else r.setDestinationAccountNumber(s.getDestinationAccountNumber());
		r.setAmount(s.getAmount()); r.setDescription(s.getDescription()); return r;
	}
	private BillPaymentRequest billRequest(ScheduledPayment s){
		BillPaymentRequest r=new BillPaymentRequest(); r.setSourceAccountNumber(s.getSourceAccount().getAccountNumber());
		r.setSelectionType(s.getBillerSelectionType()); r.setSavedBillerId(s.getSavedBiller()==null?null:s.getSavedBiller().getId());
		r.setProvider(s.getBillerProvider()); r.setConsumerReference(s.getConsumerReference()); r.setAmount(s.getAmount());
		r.setDescription(s.getDescription()); return r;
	}
}
