package com.digibank.service;

import com.digibank.dto.transaction.StatementExport;
import com.digibank.dto.transaction.TransactionSearchCriteria;
import com.digibank.dto.transaction.TransactionStatementView;
import com.digibank.entity.AccountTransaction;
import com.digibank.entity.BankAccount;
import com.digibank.entity.Customer;
import com.digibank.entity.User;
import com.digibank.enums.AccountStatus;
import com.digibank.enums.AccountType;
import com.digibank.enums.CurrencyCode;
import com.digibank.enums.CustomerStatus;
import com.digibank.enums.Gender;
import com.digibank.enums.IdentityType;
import com.digibank.enums.TransactionDirection;
import com.digibank.exception.StatementException;
import com.digibank.exception.TransactionRecordNotFoundException;
import com.digibank.repository.AccountTransactionRepository;
import com.digibank.repository.BankAccountRepository;
import com.digibank.repository.CustomerRepository;
import com.digibank.service.impl.AccountTransactionServiceImpl;
import com.digibank.util.SensitiveDataMasker;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AccountTransactionServiceImplTest {

	@Mock private CustomerRepository customerRepository;
	@Mock private BankAccountRepository bankAccountRepository;
	@Mock private AccountTransactionRepository transactionRepository;

	private AccountTransactionServiceImpl service;
	private Customer customer;
	private BankAccount account;
	private AccountTransaction entry;

	@BeforeEach
	void setUp() {
		service = new AccountTransactionServiceImpl(customerRepository, bankAccountRepository, transactionRepository,
				new SensitiveDataMasker());
		User user = new User("customer", "customer@example.com", "hash");
		setId(user, 10L);
		user.setEnabled(true);
		customer = new Customer(user, "CUS20", "Test", "Customer", LocalDate.of(1990, 1, 1), Gender.OTHER,
				IdentityType.NATIONAL_ID, "NIC20", "+94710000000", "Address", "Colombo");
		setId(customer, 20L);
		customer.setStatus(CustomerStatus.ACTIVE);
		account = new BankAccount(customer, "111122223333", AccountType.SAVINGS, "001");
		setId(account, 30L);
		account.setAccountStatus(AccountStatus.ACTIVE);
		account.setCurrencyCode(CurrencyCode.LKR);
		account.setAvailableBalance(new BigDecimal("900.00"));
		account.setCurrentBalance(new BigDecimal("900.00"));
		entry = new AccountTransaction(account, null, "TRFTEST", TransactionDirection.DEBIT,
				new BigDecimal("100.00"), new BigDecimal("900.00"), "=Unsafe Name", "********7777",
				"Invoice", LocalDateTime.of(2026, 8, 2, 10, 30));
		setId(entry, 40L);
		setCreatedAt(entry, LocalDateTime.of(2026, 8, 2, 10, 30));
		when(customerRepository.findByUserId(10L)).thenReturn(Optional.of(customer));
		when(bankAccountRepository.findByCustomerId(20L)).thenReturn(List.of(account));
	}

	@Test
	void statementMapsOnlyOwnedAccountEntries() {
		when(transactionRepository.search(eq(10L), eq("111122223333"), eq(TransactionDirection.DEBIT), any(), any(),
				eq("Invoice"), any(Pageable.class)))
				.thenReturn(new PageImpl<>(List.of(entry)));
		TransactionSearchCriteria criteria = criteria();

		TransactionStatementView statement = service.getStatement(10L, criteria);

		assertEquals(1, statement.accounts().size());
		assertEquals("********3333", statement.transactions().getContent().get(0).accountMasked());
		assertEquals("TRFTEST", statement.transactions().getContent().get(0).referenceNumber());
	}

	@Test
	void statementRejectsAnotherCustomersAccountAndInvalidDateRange() {
		TransactionSearchCriteria otherAccount = new TransactionSearchCriteria();
		otherAccount.setAccountNumber("999988887777");
		assertThrows(StatementException.class, () -> service.getStatement(10L, otherAccount));

		TransactionSearchCriteria invalidDates = new TransactionSearchCriteria();
		invalidDates.setFromDate(LocalDate.of(2026, 8, 3));
		invalidDates.setToDate(LocalDate.of(2026, 8, 2));
		assertThrows(StatementException.class, () -> service.getStatement(10L, invalidDates));
		verify(transactionRepository, never()).search(any(), any(), any(), any(), any(), any(), any());
	}

	@Test
	void detailsAreRestrictedToAuthenticatedOwner() {
		when(transactionRepository.findByIdAndAccountCustomerUserId(40L, 10L)).thenReturn(Optional.of(entry));
		assertEquals("TRFTEST", service.getTransaction(10L, 40L).referenceNumber());
		assertThrows(TransactionRecordNotFoundException.class, () -> service.getTransaction(10L, 99L));
	}

	@Test
	void csvExportIsDownloadableAndNeutralizesSpreadsheetFormulaCells() {
		when(transactionRepository.export(any(), any(), any(), any(), any(), any())).thenReturn(List.of(entry));

		StatementExport export = service.exportCsv(10L, new TransactionSearchCriteria());
		String csv = new String(export.content(), StandardCharsets.UTF_8);

		assertEquals("text/csv;charset=UTF-8", export.contentType());
		assertTrue(csv.contains("Reference"));
		assertTrue(csv.contains("'=Unsafe Name"));
	}

	@Test
	void pdfExportProducesAValidPdfHeaderAndTrailer() {
		when(transactionRepository.export(any(), any(), any(), any(), any(), any())).thenReturn(List.of(entry));

		StatementExport export = service.exportPdf(10L, new TransactionSearchCriteria());
		String pdf = new String(export.content(), StandardCharsets.ISO_8859_1);

		assertEquals("application/pdf", export.contentType());
		assertTrue(pdf.startsWith("%PDF-1.4"));
		assertTrue(pdf.endsWith("%%EOF"));
	}

	private TransactionSearchCriteria criteria() {
		TransactionSearchCriteria criteria = new TransactionSearchCriteria();
		criteria.setAccountNumber("111122223333");
		criteria.setDirection(TransactionDirection.DEBIT);
		criteria.setFromDate(LocalDate.of(2026, 8, 1));
		criteria.setToDate(LocalDate.of(2026, 8, 2));
		criteria.setKeyword("Invoice");
		return criteria;
	}

	private void setId(Object entity, Long id) {
		try {
			Method method = entity.getClass().getSuperclass().getDeclaredMethod("setId", Long.class);
			method.setAccessible(true);
			method.invoke(entity, id);
		}
		catch (ReflectiveOperationException ex) {
			throw new IllegalStateException(ex);
		}
	}

	private void setCreatedAt(Object entity, LocalDateTime value) {
		try {
			Field field = entity.getClass().getSuperclass().getDeclaredField("createdAt");
			field.setAccessible(true);
			field.set(entity, value);
		}
		catch (ReflectiveOperationException ex) {
			throw new IllegalStateException(ex);
		}
	}
}
