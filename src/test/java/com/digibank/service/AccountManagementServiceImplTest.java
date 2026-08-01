package com.digibank.service;

import com.digibank.entity.AuditLog;
import com.digibank.entity.BankAccount;
import com.digibank.entity.Customer;
import com.digibank.entity.User;
import com.digibank.enums.AccountStatus;
import com.digibank.enums.AccountType;
import com.digibank.enums.CustomerStatus;
import com.digibank.enums.Gender;
import com.digibank.enums.IdentityType;
import com.digibank.enums.Role;
import com.digibank.exception.AccountAccessDeniedException;
import com.digibank.exception.InvalidAccountClosureException;
import com.digibank.exception.InvalidAccountStateTransitionException;
import com.digibank.repository.AuditLogRepository;
import com.digibank.repository.BankAccountRepository;
import com.digibank.repository.CustomerRepository;
import com.digibank.service.impl.AccountManagementServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AccountManagementServiceImplTest {

	private FakeBankAccountRepository bankAccountRepository;
	private FakeCustomerRepository customerRepository;
	private FakeAuditLogRepository auditLogRepository;
	private AccountManagementServiceImpl accountManagementService;
	private Customer customer;
	private BankAccount account;

	@BeforeEach
	void setUp() {
		bankAccountRepository = new FakeBankAccountRepository();
		customerRepository = new FakeCustomerRepository();
		auditLogRepository = new FakeAuditLogRepository();
		accountManagementService = new AccountManagementServiceImpl(bankAccountRepository.proxy(),
				customerRepository.proxy(), auditLogRepository.proxy());

		User user = new User("customer", "customer@example.com", "encoded-password");
		user.setRole(Role.CUSTOMER);
		setId(user, 1L);
		customer = new Customer(user, "CUS2026000123", "Lakshitha", "Dilshan", LocalDate.of(1998, 1, 1),
				Gender.MALE, IdentityType.NATIONAL_ID, "123456789V", "+94712345678", "No 1 Main Street",
				"Colombo");
		customer.setStatus(CustomerStatus.ACTIVE);
		customer.setCountry("Sri Lanka");
		setId(customer, 2L);

		account = new BankAccount(customer, "123456789012", AccountType.SAVINGS, "COL001");
		account.setAvailableBalance(BigDecimal.ZERO);
		account.setCurrentBalance(BigDecimal.ZERO);
		setId(account, 3L);

		customerRepository.customersByNumber.put(customer.getCustomerNumber(), customer);
		bankAccountRepository.accountsByNumber.put(account.getAccountNumber(), account);
		bankAccountRepository.accountsByCustomerId.put(customer.getId(), List.of(account));
	}

	@Test
	void staffCanActivatePendingAccount() {
		account.setAccountStatus(AccountStatus.PENDING_ACTIVATION);

		accountManagementService.activateAccount("staff", account.getAccountNumber());

		assertEquals(AccountStatus.ACTIVE, account.getAccountStatus());
		assertAuditCreated("ACCOUNT_ACTIVATED");
	}

	@Test
	void initialActivationVerifiesCustomerAndEnablesLogin() {
		account.setAccountStatus(AccountStatus.PENDING_ACTIVATION);
		customer.setStatus(CustomerStatus.PENDING_VERIFICATION);
		customer.getUser().setEnabled(false);

		accountManagementService.activateAccount("staff", account.getAccountNumber());

		assertEquals(CustomerStatus.ACTIVE, customer.getStatus());
		assertTrue(customer.getUser().isEnabled());
		assertAuditCreated("CUSTOMER_VERIFIED");
		assertAuditCreated("ACCOUNT_ACTIVATED");
	}

	@Test
	void staffCanFreezeActiveAccount() {
		account.setAccountStatus(AccountStatus.ACTIVE);

		accountManagementService.freezeAccount("staff", account.getAccountNumber(), "Suspicious activity");

		assertEquals(AccountStatus.FROZEN, account.getAccountStatus());
		assertAuditCreated("ACCOUNT_FROZEN");
	}

	@Test
	void staffCanUnfreezeFrozenAccount() {
		account.setAccountStatus(AccountStatus.FROZEN);

		accountManagementService.unfreezeAccount("staff", account.getAccountNumber(), "Verified");

		assertEquals(AccountStatus.ACTIVE, account.getAccountStatus());
		assertAuditCreated("ACCOUNT_UNFROZEN");
	}

	@Test
	void closedAccountCannotReactivate() {
		account.setAccountStatus(AccountStatus.CLOSED);

		assertThrows(InvalidAccountStateTransitionException.class,
				() -> accountManagementService.reactivateEligibleAccount("staff", account.getAccountNumber(),
						"Approved"));
	}

	@Test
	void accountWithPositiveBalanceCannotClose() {
		account.setAccountStatus(AccountStatus.ACTIVE);
		account.setAvailableBalance(new BigDecimal("100.00"));

		assertThrows(InvalidAccountClosureException.class,
				() -> accountManagementService.closeAccount("staff", customer.getCustomerNumber(),
						account.getAccountNumber(), "Requested"));
	}

	@Test
	void closureReasonRequired() {
		account.setAccountStatus(AccountStatus.ACTIVE);

		assertThrows(InvalidAccountClosureException.class,
				() -> accountManagementService.closeAccount("staff", customer.getCustomerNumber(),
						account.getAccountNumber(), " "));
	}

	@Test
	void deactivateDisablesCustomerAccess() {
		account.setAccountStatus(AccountStatus.ACTIVE);

		accountManagementService.deactivateAccount("staff", account.getAccountNumber(), "Dormant account");

		assertEquals(AccountStatus.DEACTIVATED, account.getAccountStatus());
		assertEquals(CustomerStatus.DEACTIVATED, customer.getStatus());
		assertFalse(customer.getUser().isEnabled());
		assertAuditCreated("CUSTOMER_DEACTIVATED");
		assertAuditCreated("ACCOUNT_DEACTIVATED");
	}

	@Test
	void accountMustBelongToSelectedCustomerBeforeClose() {
		account.setAccountStatus(AccountStatus.ACTIVE);
		User otherUser = new User("other", "other@example.com", "encoded-password");
		Customer otherCustomer = new Customer(otherUser, "CUS2026000999", "Other", "Customer",
				LocalDate.of(1990, 1, 1), Gender.OTHER, IdentityType.NATIONAL_ID, "987654321V", "+94799999999",
				"Other Address", "Galle");
		setId(otherCustomer, 99L);
		customerRepository.customersByNumber.put(otherCustomer.getCustomerNumber(), otherCustomer);

		assertThrows(AccountAccessDeniedException.class,
				() -> accountManagementService.closeAccount("staff", otherCustomer.getCustomerNumber(),
						account.getAccountNumber(), "Requested"));
	}

	@Test
	void pendingAccountCannotBeFrozen() {
		account.setAccountStatus(AccountStatus.PENDING_ACTIVATION);

		assertThrows(InvalidAccountStateTransitionException.class,
				() -> accountManagementService.freezeAccount("staff", account.getAccountNumber(), "No"));
	}

	@Test
	void auditRecordCreatedForClosureRequest() {
		account.setAccountStatus(AccountStatus.ACTIVE);

		accountManagementService.requestAccountClosure("customer", account.getAccountNumber(), "Moving bank");

		assertAuditCreated("ACCOUNT_CLOSURE_REQUESTED");
	}

	private void assertAuditCreated(String action) {
		assertTrue(auditLogRepository.logs.stream().anyMatch(log -> action.equals(log.getAction())));
	}

	static class FakeBankAccountRepository implements InvocationHandler {

		private final Map<String, BankAccount> accountsByNumber = new HashMap<>();
		private final Map<Long, List<BankAccount>> accountsByCustomerId = new HashMap<>();

		BankAccountRepository proxy() {
			return createProxy(BankAccountRepository.class, this);
		}

		@Override
		public Object invoke(Object proxy, Method method, Object[] args) {
			return switch (method.getName()) {
				case "findByAccountNumber" -> Optional.ofNullable(accountsByNumber.get((String) args[0]));
				case "findByCustomerId" -> accountsByCustomerId.getOrDefault((Long) args[0], List.of());
				case "save" -> args[0];
				default -> defaultValue(method.getReturnType());
			};
		}
	}

	static class FakeCustomerRepository implements InvocationHandler {

		private final Map<String, Customer> customersByNumber = new HashMap<>();

		CustomerRepository proxy() {
			return createProxy(CustomerRepository.class, this);
		}

		@Override
		public Object invoke(Object proxy, Method method, Object[] args) {
			return switch (method.getName()) {
				case "findByCustomerNumber" -> Optional.ofNullable(customersByNumber.get((String) args[0]));
				case "findAll" -> new ArrayList<>(customersByNumber.values());
				case "save" -> args[0];
				default -> defaultValue(method.getReturnType());
			};
		}
	}

	static class FakeAuditLogRepository implements InvocationHandler {

		private final List<AuditLog> logs = new ArrayList<>();

		AuditLogRepository proxy() {
			return createProxy(AuditLogRepository.class, this);
		}

		@Override
		public Object invoke(Object proxy, Method method, Object[] args) {
			if ("save".equals(method.getName())) {
				logs.add((AuditLog) args[0]);
				return args[0];
			}
			return defaultValue(method.getReturnType());
		}
	}

	private static <T> T createProxy(Class<T> type, InvocationHandler invocationHandler) {
		return type.cast(Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[] {type}, invocationHandler));
	}

	private static Object defaultValue(Class<?> returnType) {
		if (returnType == Boolean.TYPE) {
			return false;
		}
		if (returnType == Integer.TYPE) {
			return 0;
		}
		if (returnType == Long.TYPE) {
			return 0L;
		}
		if (returnType == Void.TYPE) {
			return null;
		}
		if (returnType == Optional.class) {
			return Optional.empty();
		}
		if (returnType == List.class) {
			return List.of();
		}
		return null;
	}

	private static void setId(Object entity, Long id) {
		try {
			Method method = entity.getClass().getSuperclass().getDeclaredMethod("setId", Long.class);
			method.setAccessible(true);
			method.invoke(entity, id);
		}
		catch (ReflectiveOperationException ex) {
			throw new IllegalStateException("Could not set test entity id.", ex);
		}
	}
}
