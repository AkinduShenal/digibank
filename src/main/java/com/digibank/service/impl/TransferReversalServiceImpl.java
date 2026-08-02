package com.digibank.service.impl;

import com.digibank.dto.transfer.StaffTransferView;
import com.digibank.entity.*;
import com.digibank.enums.*;
import com.digibank.exception.TransferException;
import com.digibank.repository.*;
import com.digibank.service.TransferReversalService;
import com.digibank.util.SensitiveDataMasker;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class TransferReversalServiceImpl implements TransferReversalService {
	private final FundTransferRepository transfers; private final BankAccountRepository accounts;
	private final AccountTransactionRepository transactions; private final CustomerNotificationRepository notifications;
	private final AuditLogRepository audits; private final SensitiveDataMasker masker;
	public TransferReversalServiceImpl(FundTransferRepository transfers,BankAccountRepository accounts,
			AccountTransactionRepository transactions,CustomerNotificationRepository notifications,
			AuditLogRepository audits,SensitiveDataMasker masker){this.transfers=transfers;this.accounts=accounts;this.transactions=transactions;this.notifications=notifications;this.audits=audits;this.masker=masker;}

	@Override @Transactional(readOnly=true)
	public Page<StaffTransferView> search(String query,TransferStatus status,int page,int size){
		String q=query==null||query.isBlank()?null:query.trim();int safeSize=Math.min(100,Math.max(5,size));
		return transfers.searchForStaff(q,status,PageRequest.of(Math.max(0,page),safeSize,Sort.by(Sort.Direction.DESC,"createdAt"))).map(this::view);
	}
	@Override @Transactional(readOnly=true)
	public StaffTransferView get(String reference){return transfers.findByReferenceNumber(clean(reference)).map(this::view).orElseThrow(()->new TransferException("Transfer was not found."));}
	@Override @Transactional
	public String reverse(String actor,String reference,String reason){
		String cleanReason=clean(reason);if(cleanReason==null||cleanReason.length()<10)throw new TransferException("Enter a reversal reason of at least 10 characters.");
		if(cleanReason.length()>255)throw new TransferException("Reversal reason cannot exceed 255 characters.");
		FundTransfer transfer=transfers.findByReferenceNumberForUpdate(clean(reference)).orElseThrow(()->new TransferException("Transfer was not found."));
		if(transfer.getStatus()!=TransferStatus.COMPLETED)throw new TransferException("Only a completed transfer can be reversed once.");
		List<String> numbers=new ArrayList<>();numbers.add(transfer.getSourceAccount().getAccountNumber());
		if(transfer.getDestinationAccount()!=null)numbers.add(transfer.getDestinationAccount().getAccountNumber());
		Map<String,BankAccount> locked=new HashMap<>();accounts.findAllByAccountNumberInForUpdate(numbers.stream().distinct().sorted().toList()).forEach(a->locked.put(a.getAccountNumber(),a));
		BankAccount source=locked.get(transfer.getSourceAccount().getAccountNumber());if(source==null||source.getAccountStatus()==AccountStatus.CLOSED)throw new TransferException("Source account cannot receive this reversal.");
		BankAccount destination=transfer.getDestinationAccount()==null?null:locked.get(transfer.getDestinationAccount().getAccountNumber());
		if(destination!=null&&(destination.getAccountStatus()==AccountStatus.CLOSED||destination.getAvailableBalance().compareTo(transfer.getAmount())<0||destination.getCurrentBalance().compareTo(transfer.getAmount())<0))
			throw new TransferException("Destination account has insufficient available funds for reversal.");
		if(destination!=null){destination.setAvailableBalance(destination.getAvailableBalance().subtract(transfer.getAmount()));destination.setCurrentBalance(destination.getCurrentBalance().subtract(transfer.getAmount()));}
		source.setAvailableBalance(source.getAvailableBalance().add(transfer.getAmount()));source.setCurrentBalance(source.getCurrentBalance().add(transfer.getAmount()));accounts.saveAll(locked.values());
		String reversalReference=newReference();LocalDateTime now=LocalDateTime.now();String safeActor=clean(actor)==null?"SYSTEM":clean(actor);
		transfer.reverse(reversalReference,cleanReason,safeActor,now);transfers.save(transfer);
		transactions.save(new AccountTransaction(source,transfer,reversalReference,TransactionDirection.CREDIT,AccountTransactionType.FUND_TRANSFER_REVERSAL,transfer.getAmount(),source.getAvailableBalance(),"Transfer reversal",transfer.getDestinationAccountMasked(),"Reversal of "+transfer.getReferenceNumber(),now));
		if(destination!=null)transactions.save(new AccountTransaction(destination,transfer,reversalReference,TransactionDirection.DEBIT,AccountTransactionType.FUND_TRANSFER_REVERSAL,transfer.getAmount(),destination.getAvailableBalance(),transfer.getCustomer().getFullName(),masker.maskAccountNumber(source.getAccountNumber()),"Reversal of "+transfer.getReferenceNumber(),now));
		notifications.save(new CustomerNotification(transfer.getCustomer().getUser(),NotificationType.ACCOUNT_NOTICE,"Transfer reversed","LKR "+transfer.getAmount().toPlainString()+" was returned. Reversal: "+reversalReference,reversalReference));
		if(destination!=null)notifications.save(new CustomerNotification(destination.getCustomer().getUser(),NotificationType.ACCOUNT_NOTICE,"Incoming transfer reversed","LKR "+transfer.getAmount().toPlainString()+" was reversed by the bank. Reference: "+reversalReference,reversalReference));
		audits.save(new AuditLog(safeActor,"FUND_TRANSFER_REVERSED","FUND_TRANSFER",transfer.getReferenceNumber(),TransferStatus.COMPLETED.name(),TransferStatus.REVERSED.name(),cleanReason+" ["+reversalReference+"]",now));return reversalReference;
	}
	private String newReference(){String r;do{r="REV"+UUID.randomUUID().toString().replace("-","").substring(0,17).toUpperCase(Locale.ROOT);}while(transfers.existsByReversalReference(r));return r;}
	private String clean(String value){return value==null||value.trim().isEmpty()?null:value.trim();}
	private StaffTransferView view(FundTransfer t){return new StaffTransferView(t.getReferenceNumber(),t.getCustomer().getCustomerNumber(),t.getCustomer().getFullName(),masker.maskAccountNumber(t.getSourceAccount().getAccountNumber()),t.getBeneficiaryNameSnapshot(),t.getDestinationBankSnapshot(),t.getDestinationAccountMasked(),t.getAmount(),t.getTransferType(),t.getStatus(),t.getDescription(),t.getCompletedAt(),t.getReversalReference(),t.getReversalReason(),t.getReversedBy(),t.getReversedAt());}
}
