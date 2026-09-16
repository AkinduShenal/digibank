package com.digibank.service;

import com.digibank.dto.schedule.*;
import com.digibank.entity.*;
import com.digibank.enums.*;
import com.digibank.exception.ScheduledPaymentException;
import com.digibank.repository.*;
import com.digibank.service.impl.ScheduledPaymentServiceImpl;
import com.digibank.util.SensitiveDataMasker;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class ScheduledPaymentServiceImplTest {
    ScheduledPaymentRepository schedules;
    CustomerRepository customers;
    BankAccountRepository accounts;
    BeneficiaryRepository beneficiaries;
    SavedBillerRepository billers;
    AuditLogRepository audit;
    PasswordEncoder encoder;
    ScheduledPaymentServiceImpl service;
    Customer customer;
    BankAccount source;

    @BeforeEach void setup() {
        schedules=mock(ScheduledPaymentRepository.class); customers=mock(CustomerRepository.class);
        accounts=mock(BankAccountRepository.class); beneficiaries=mock(BeneficiaryRepository.class);
        billers=mock(SavedBillerRepository.class); audit=mock(AuditLogRepository.class); encoder=mock(PasswordEncoder.class);
        service=new ScheduledPaymentServiceImpl(schedules,customers,accounts,beneficiaries,billers,audit,encoder,new SensitiveDataMasker());
        User user=new User("customer","demo@example.com","hash"); user.setTransactionPinHash("pin-hash");
        ReflectionTestUtils.setField(user,"id",7L);
        customer=new Customer(user,"CUS1","Demo","Customer",LocalDate.of(1990,1,1),Gender.OTHER,
                IdentityType.PASSPORT,"DEMO123","0712345678","Demo address","Colombo");
        customer.setStatus(CustomerStatus.ACTIVE); ReflectionTestUtils.setField(customer,"id",10L);
        source=new BankAccount(customer,"111122223333",AccountType.SAVINGS,"COL001");
        ReflectionTestUtils.setField(source,"id",20L); source.setAccountStatus(AccountStatus.ACTIVE);
        source.setCurrencyCode(CurrencyCode.LKR);
        when(customers.findByUserId(7L)).thenReturn(Optional.of(customer));
        when(accounts.findByAccountNumber(source.getAccountNumber())).thenReturn(Optional.of(source));
        when(encoder.matches("2468","pin-hash")).thenReturn(true);
    }
    ScheduledPaymentRequest request(ScheduledPaymentType type) {
        var r=new ScheduledPaymentRequest(); r.setPaymentType(type); r.setSourceAccountNumber(source.getAccountNumber());
        r.setAmount(new BigDecimal("100")); r.setRecurrence(ScheduleRecurrence.ONCE);
        r.setNextExecutionAt(LocalDateTime.now().plusDays(3)); r.setTransactionPin("2468");
        r.setBillerSelectionType(BillerSelectionType.NEW_BILLER); r.setBillerProvider(BillerProvider.CEB); r.setConsumerReference("1234567890");
        r.setTransferRecipientType(TransferRecipientType.OWN_ACCOUNT); r.setOwnDestinationAccountNumber("444455556666");
        return r;
    }
    ScheduledPayment schedule(ScheduledPaymentType type) {
        var s=new ScheduledPayment(customer,source,"SCHTEST",type,ScheduleRecurrence.MONTHLY,
                LocalDateTime.now().plusDays(3),null,new BigDecimal("100"),"Original");
        if(type==ScheduledPaymentType.BILL_PAYMENT) s.configureBill(BillerSelectionType.NEW_BILLER,null,BillerProvider.CEB,"1234567890");
        else s.configureTransfer(TransferRecipientType.DIGIBANK_ACCOUNT,null,"444455556666");
        when(schedules.findOwnedForUpdate("SCHTEST",7L)).thenReturn(Optional.of(s));
        when(schedules.findByScheduleReferenceAndCustomerUserId("SCHTEST",7L)).thenReturn(Optional.of(s));
        return s;
    }
    ScheduledPaymentUpdateRequest update() {
        var r=new ScheduledPaymentUpdateRequest(); r.setAmount(new BigDecimal("250")); r.setRecurrence(ScheduleRecurrence.MONTHLY);
        r.setNextExecutionAt(LocalDateTime.now().plusDays(5)); r.setEndDate(LocalDate.now().plusMonths(6)); r.setDescription("Updated"); return r;
    }
    @ParameterizedTest @EnumSource(ScheduledPaymentType.class)
    void createAndListAreTypeSpecific(ScheduledPaymentType type) {
        BankAccount target=new BankAccount(customer,"444455556666",AccountType.SAVINGS,"COL001");
        ReflectionTestUtils.setField(target,"id",21L); target.setAccountStatus(AccountStatus.ACTIVE); target.setCurrencyCode(CurrencyCode.LKR);
        when(accounts.findByAccountNumber(target.getAccountNumber())).thenReturn(Optional.of(target));
        var created=service.create(7L,"customer",request(type));
        assertEquals(type,created.paymentType()); assertEquals(ScheduleStatus.SCHEDULED,created.status());
        verify(schedules).save(any(ScheduledPayment.class)); verify(audit).save(any());
        var stored=schedule(type);
		when(schedules.findByCustomerUserIdAndPaymentTypeAndStatusNotOrderByCreatedAtDesc(
				7L,type,ScheduleStatus.CANCELLED)).thenReturn(List.of(stored));
		assertEquals(type,service.list(7L,type).getFirst().paymentType());
		verify(schedules).findByCustomerUserIdAndPaymentTypeAndStatusNotOrderByCreatedAtDesc(
				7L,type,ScheduleStatus.CANCELLED);
    }
    @ParameterizedTest @EnumSource(ScheduledPaymentType.class)
    void updateThenCancelPreservesHistoryAndAuditsBoth(ScheduledPaymentType type) {
        var s=schedule(type); service.update(7L,"customer","SCHTEST",update());
        assertEquals(new BigDecimal("250"),s.getAmount()); assertEquals("Updated",s.getDescription());
        service.cancel(7L,"customer","SCHTEST"); assertEquals(ScheduleStatus.CANCELLED,s.getStatus());
        assertNotNull(s.getCancelledAt()); assertEquals(type,s.getPaymentType());
        verify(schedules,times(2)).findOwnedForUpdate("SCHTEST",7L);
        verify(audit,times(2)).save(any()); verify(schedules,never()).delete(any(ScheduledPayment.class));
    }
    @ParameterizedTest @EnumSource(value=ScheduleStatus.class,names={"PROCESSING","COMPLETED","CANCELLED","FAILED"})
    void terminalOrProcessingSchedulesCannotBeEditedOrCancelled(ScheduleStatus status) {
        var s=schedule(ScheduledPaymentType.FUND_TRANSFER); ReflectionTestUtils.setField(s,"status",status);
        assertThrows(ScheduledPaymentException.class,()->service.getUpdateRequest(7L,"SCHTEST"));
        assertThrows(ScheduledPaymentException.class,()->service.update(7L,"customer","SCHTEST",update()));
        assertThrows(ScheduledPaymentException.class,()->service.cancel(7L,"customer","SCHTEST"));
        assertEquals(status,s.getStatus()); verifyNoInteractions(audit);
    }
    @Test void anotherCustomerCannotReadEditOrCancel() {
        schedule(ScheduledPaymentType.FUND_TRANSFER);
        assertThrows(ScheduledPaymentException.class,()->service.get(99L,"SCHTEST"));
        assertThrows(ScheduledPaymentException.class,()->service.update(99L,"other","SCHTEST",update()));
        assertThrows(ScheduledPaymentException.class,()->service.cancel(99L,"other","SCHTEST"));
        verifyNoInteractions(audit);
    }
    @ParameterizedTest @ValueSource(strings={"0.01","9.99","1000001","10.001"})
    void billAmountRulesApplyToBothCreateAndUpdate(String amount) {
        var r=request(ScheduledPaymentType.BILL_PAYMENT); r.setAmount(new BigDecimal(amount));
        assertThrows(ScheduledPaymentException.class,()->service.create(7L,"customer",r));
        var s=schedule(ScheduledPaymentType.BILL_PAYMENT); var change=update(); change.setAmount(new BigDecimal(amount));
        assertThrows(ScheduledPaymentException.class,()->service.update(7L,"customer","SCHTEST",change));
        assertEquals(new BigDecimal("100"),s.getAmount()); verifyNoInteractions(audit);
    }
    @Test void invalidDatesDoNotChangeScheduleAndOneTimeClearsEndDate() {
        var s=schedule(ScheduledPaymentType.BILL_PAYMENT); var change=update(); change.setEndDate(LocalDate.now());
        assertThrows(ScheduledPaymentException.class,()->service.update(7L,"customer","SCHTEST",change));
        change.setEndDate(null); change.setNextExecutionAt(LocalDateTime.now().minusDays(1));
        assertThrows(ScheduledPaymentException.class,()->service.update(7L,"customer","SCHTEST",change));
        assertEquals(new BigDecimal("100"),s.getAmount());
        var oneTime=update(); oneTime.setRecurrence(ScheduleRecurrence.ONCE);
        service.update(7L,"customer","SCHTEST",oneTime); assertNull(s.getEndDate());
    }
    @Test void wrongPinAndMissingSavedSelectionRejectCleanly() {
        var r=request(ScheduledPaymentType.BILL_PAYMENT); r.setTransactionPin("9999");
        assertThrows(ScheduledPaymentException.class,()->service.create(7L,"customer",r));
        r.setTransactionPin("2468"); r.setBillerSelectionType(BillerSelectionType.SAVED_BILLER);
        assertThrows(ScheduledPaymentException.class,()->service.create(7L,"customer",r));
        var transfer=request(ScheduledPaymentType.FUND_TRANSFER); transfer.setTransferRecipientType(TransferRecipientType.SAVED_BENEFICIARY);
        assertThrows(ScheduledPaymentException.class,()->service.create(7L,"customer",transfer));
        verifyNoInteractions(billers,beneficiaries,audit);
    }
    @Test void sameAccountAndUnavailableDirectRecipientRejectCleanly() {
        var r=request(ScheduledPaymentType.FUND_TRANSFER); r.setOwnDestinationAccountNumber(source.getAccountNumber());
        assertThrows(ScheduledPaymentException.class,()->service.create(7L,"customer",r));
        r.setTransferRecipientType(TransferRecipientType.DIGIBANK_ACCOUNT); r.setDestinationAccountNumber("999988887777");
        assertThrows(ScheduledPaymentException.class,()->service.create(7L,"customer",r));
        verifyNoInteractions(audit);
    }
}
