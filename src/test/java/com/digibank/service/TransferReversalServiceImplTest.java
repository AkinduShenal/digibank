package com.digibank.service;

import com.digibank.entity.*;
import com.digibank.enums.*;
import com.digibank.exception.TransferException;
import com.digibank.repository.*;
import com.digibank.service.impl.TransferReversalServiceImpl;
import com.digibank.util.SensitiveDataMasker;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class TransferReversalServiceImplTest {
	@Mock FundTransferRepository transferRepository; @Mock BankAccountRepository accountRepository;
	@Mock AccountTransactionRepository transactionRepository; @Mock CustomerNotificationRepository notificationRepository;
	@Mock AuditLogRepository auditRepository;
	TransferReversalServiceImpl service; BankAccount source; BankAccount destination; FundTransfer transfer;

	@BeforeEach void setup() {
		service=new TransferReversalServiceImpl(transferRepository,accountRepository,transactionRepository,
				notificationRepository,auditRepository,new SensitiveDataMasker());
		Customer sender=customer(1L,11L,"sender","Sender");Customer receiver=customer(2L,12L,"receiver","Receiver");
		source=account(sender,21L,"111122223333","700.00");destination=account(receiver,22L,"999988887777","500.00");
		transfer=new FundTransfer(sender,source,null,"TRFTEST",TransferType.INTERNAL,new BigDecimal("300.00"),"Rent",
				"Receiver Customer","DigiBank","********7777");transfer.setDestinationAccount(destination);
		transfer.setStatus(TransferStatus.COMPLETED);transfer.setCompletedAt(LocalDateTime.now());
		when(transferRepository.findByReferenceNumberForUpdate("TRFTEST")).thenReturn(Optional.of(transfer));
		when(accountRepository.findAllByAccountNumberInForUpdate(List.of("111122223333","999988887777"))).thenReturn(List.of(source,destination));
		when(transferRepository.save(any())).thenAnswer(i->i.getArgument(0));
	}

	@Test void internalReversalMovesFundsBackAndCreatesCompensatingLedger() {
		String reference=service.reverse("staff","TRFTEST","Customer confirmed duplicate transfer");
		assertTrue(reference.startsWith("REV"));assertEquals(TransferStatus.REVERSED,transfer.getStatus());
		assertEquals(new BigDecimal("1000.00"),source.getAvailableBalance());
		assertEquals(new BigDecimal("200.00"),destination.getAvailableBalance());
		ArgumentCaptor<AccountTransaction> rows=ArgumentCaptor.forClass(AccountTransaction.class);
		verify(transactionRepository,times(2)).save(rows.capture());
		assertTrue(rows.getAllValues().stream().allMatch(r->r.getTransactionType()==AccountTransactionType.FUND_TRANSFER_REVERSAL));
		verify(notificationRepository,times(2)).save(any());verify(auditRepository).save(any());
	}

	@Test void reversalIsRejectedWhenDestinationFundsAreUnavailable() {
		destination.setAvailableBalance(new BigDecimal("299.99"));
		assertThrows(TransferException.class,()->service.reverse("staff","TRFTEST","Confirmed erroneous transfer"));
		assertEquals(new BigDecimal("700.00"),source.getAvailableBalance());verify(transactionRepository,never()).save(any());
	}

	@Test void completedTransferCanOnlyBeReversedOnce() {
		transfer.setStatus(TransferStatus.REVERSED);
		assertThrows(TransferException.class,()->service.reverse("staff","TRFTEST","Confirmed erroneous transfer"));
		verify(accountRepository,never()).findAllByAccountNumberInForUpdate(any());
	}

	private Customer customer(Long userId,Long customerId,String username,String first){User u=new User(username,username+"@test","hash");setId(u,userId);Customer c=new Customer(u,"CUS"+customerId,first,"Customer",LocalDate.of(1990,1,1),Gender.OTHER,IdentityType.NATIONAL_ID,"NIC"+customerId,"0770000000","Address","Colombo");setId(c,customerId);c.setStatus(CustomerStatus.ACTIVE);return c;}
	private BankAccount account(Customer c,Long id,String number,String balance){BankAccount a=new BankAccount(c,number,AccountType.SAVINGS,"COL001");setId(a,id);a.setAccountStatus(AccountStatus.ACTIVE);a.setCurrencyCode(CurrencyCode.LKR);a.setCurrentBalance(new BigDecimal(balance));a.setAvailableBalance(new BigDecimal(balance));return a;}
	private void setId(Object entity,Long id){try{Method m=entity.getClass().getSuperclass().getDeclaredMethod("setId",Long.class);m.setAccessible(true);m.invoke(entity,id);}catch(Exception ex){throw new IllegalStateException(ex);}}
}
