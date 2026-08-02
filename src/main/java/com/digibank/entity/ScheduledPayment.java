package com.digibank.entity;

import com.digibank.enums.*;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "scheduled_payments", uniqueConstraints = @UniqueConstraint(name = "uk_scheduled_payments_reference", columnNames = "schedule_reference"))
public class ScheduledPayment extends BaseEntity {
	@ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "customer_id", nullable = false) private Customer customer;
	@ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "source_account_id", nullable = false) private BankAccount sourceAccount;
	@ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "beneficiary_id") private Beneficiary beneficiary;
	@ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "saved_biller_id") private SavedBiller savedBiller;
	@Column(name = "schedule_reference", nullable = false, length = 32) private String scheduleReference;
	@Enumerated(EnumType.STRING) @Column(name = "payment_type", nullable = false, length = 30) private ScheduledPaymentType paymentType;
	@Enumerated(EnumType.STRING) @Column(name = "status", nullable = false, length = 20) private ScheduleStatus status;
	@Enumerated(EnumType.STRING) @Column(name = "recurrence", nullable = false, length = 20) private ScheduleRecurrence recurrence;
	@Column(name = "next_execution_at", nullable = false) private LocalDateTime nextExecutionAt;
	@Column(name = "end_date") private LocalDate endDate;
	@Column(name = "amount", nullable = false, precision = 19, scale = 2) private BigDecimal amount;
	@Column(name = "description", length = 140) private String description;
	@Enumerated(EnumType.STRING) @Column(name = "transfer_recipient_type", length = 30) private TransferRecipientType transferRecipientType;
	@Column(name = "destination_account_number", length = 34) private String destinationAccountNumber;
	@Enumerated(EnumType.STRING) @Column(name = "biller_selection_type", length = 30) private BillerSelectionType billerSelectionType;
	@Enumerated(EnumType.STRING) @Column(name = "biller_provider", length = 40) private BillerProvider billerProvider;
	@Column(name = "consumer_reference", length = 50) private String consumerReference;
	@Column(name = "execution_count", nullable = false) private int executionCount;
	@Column(name = "last_execution_reference", length = 32) private String lastExecutionReference;
	@Column(name = "last_executed_at") private LocalDateTime lastExecutedAt;
	@Column(name = "failure_reason", length = 255) private String failureReason;
	@Column(name = "cancelled_at") private LocalDateTime cancelledAt;
	@Version @Column(name = "version", nullable = false) private long version;

	protected ScheduledPayment() { }
	public ScheduledPayment(Customer customer, BankAccount account, String reference, ScheduledPaymentType type,
			ScheduleRecurrence recurrence, LocalDateTime nextExecutionAt, LocalDate endDate, BigDecimal amount, String description) {
		this.customer=customer; this.sourceAccount=account; this.scheduleReference=reference; this.paymentType=type;
		this.recurrence=recurrence; this.nextExecutionAt=nextExecutionAt; this.endDate=endDate; this.amount=amount;
		this.description=description; this.status=ScheduleStatus.SCHEDULED;
	}
	public void configureTransfer(TransferRecipientType type, Beneficiary beneficiary, String destination) {
		this.transferRecipientType=type; this.beneficiary=beneficiary; this.destinationAccountNumber=destination;
	}
	public void configureBill(BillerSelectionType type, SavedBiller saved, BillerProvider provider, String consumerReference) {
		this.billerSelectionType=type; this.savedBiller=saved; this.billerProvider=provider; this.consumerReference=consumerReference;
	}
	public void update(BigDecimal amount, String description, ScheduleRecurrence recurrence, LocalDateTime next, LocalDate end) {
		this.amount=amount; this.description=description; this.recurrence=recurrence; this.nextExecutionAt=next; this.endDate=end;
	}
	public void processing() { this.status=ScheduleStatus.PROCESSING; this.failureReason=null; }
	public void succeeded(String executionReference, LocalDateTime when) {
		this.executionCount++; this.lastExecutionReference=executionReference; this.lastExecutedAt=when; this.failureReason=null;
		LocalDateTime next=nextExecutionAt.plusMonths(1);
		if (recurrence==ScheduleRecurrence.ONCE || (endDate!=null && next.toLocalDate().isAfter(endDate))) status=ScheduleStatus.COMPLETED;
		else { status=ScheduleStatus.SCHEDULED; nextExecutionAt=next; }
	}
	public void failed(String reason, LocalDateTime when) { this.status=ScheduleStatus.FAILED; this.failureReason=reason; this.lastExecutedAt=when; }
	public void cancel(LocalDateTime when) { this.status=ScheduleStatus.CANCELLED; this.cancelledAt=when; }
	public Customer getCustomer(){return customer;} public BankAccount getSourceAccount(){return sourceAccount;}
	public Beneficiary getBeneficiary(){return beneficiary;} public SavedBiller getSavedBiller(){return savedBiller;}
	public String getScheduleReference(){return scheduleReference;} public ScheduledPaymentType getPaymentType(){return paymentType;}
	public ScheduleStatus getStatus(){return status;} public ScheduleRecurrence getRecurrence(){return recurrence;}
	public LocalDateTime getNextExecutionAt(){return nextExecutionAt;} public LocalDate getEndDate(){return endDate;}
	public BigDecimal getAmount(){return amount;} public String getDescription(){return description;}
	public TransferRecipientType getTransferRecipientType(){return transferRecipientType;} public String getDestinationAccountNumber(){return destinationAccountNumber;}
	public BillerSelectionType getBillerSelectionType(){return billerSelectionType;} public BillerProvider getBillerProvider(){return billerProvider;}
	public String getConsumerReference(){return consumerReference;} public int getExecutionCount(){return executionCount;}
	public String getLastExecutionReference(){return lastExecutionReference;} public LocalDateTime getLastExecutedAt(){return lastExecutedAt;}
	public String getFailureReason(){return failureReason;} public LocalDateTime getCancelledAt(){return cancelledAt;}
}
