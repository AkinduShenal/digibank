package com.digibank.service;

import com.digibank.dto.customer.CustomerProfileUpdateRequest;
import com.digibank.dto.customer.CustomerProfileView;
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
import com.digibank.repository.BankAccountRepository;
import com.digibank.repository.CustomerRepository;
import com.digibank.security.CustomUserDetails;
import com.digibank.service.impl.CustomerProfileServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CustomerProfileServiceImplTest {

	private FakeCustomerRepository customerRepository;
	private FakeBankAccountRepository bankAccountRepository;
	private CustomerProfileServiceImpl profileService;
	private User user;
	private Customer customer;
	private BankAccount account;
	private CustomUserDetails userDetails;

	@BeforeEach
	void setUp() {
		customerRepository = new FakeCustomerRepository();
		bankAccountRepository = new FakeBankAccountRepository();
		profileService = new CustomerProfileServiceImpl(customerRepository.proxy(), bankAccountRepository.proxy());

		user = new User("customer", "customer@example.com", "encoded-password");
		user.setRole(Role.CUSTOMER);
		user.setLastLoginAt(LocalDateTime.of(2026, 7, 30, 13, 45));
		setId(user, 10L);

		customer = new Customer(user, "CUS2026000123", "Lakshitha", "Dilshan", LocalDate.of(1998, 1, 1),
				Gender.MALE, IdentityType.NATIONAL_ID, "123456789V", "+94712345678", "No 1 Main Street",
				"Colombo");
		customer.setStatus(CustomerStatus.ACTIVE);
		customer.setCountry("Sri Lanka");
		customer.setNationality("Sri Lankan");
		setId(customer, 20L);
		setCreatedAt(customer, LocalDateTime.of(2026, 7, 30, 12, 0));

		account = new BankAccount(customer, "123456789012", AccountType.SAVINGS, "COL001");
		account.setAccountStatus(AccountStatus.PENDING_ACTIVATION);
		account.setAvailableBalance(new BigDecimal("1000.00"));
		account.setCurrentBalance(new BigDecimal("1000.00"));
		account.setOpenedAt(LocalDateTime.of(2026, 7, 30, 12, 5));
		setId(account, 30L);

		customerRepository.customersByUserId.put(10L, customer);
		bankAccountRepository.accountsByCustomerId.put(20L, List.of(account));
		bankAccountRepository.accountsByNumber.put("123456789012", account);
		userDetails = new CustomUserDetails(user, CustomerStatus.ACTIVE);
	}

	@Test
	void profileMasksIdentityAndAccountNumbers() {
		CustomerProfileView profile = profileService.getProfile(userDetails);

		assertEquals("*******89V", profile.getMaskedIdentityNumber());
		assertEquals("********9012", profile.getAccounts().get(0).getMaskedAccountNumber());
	}

	@Test
	void dashboardUsesOnlyAuthenticatedCustomerData() {
		assertEquals("CUS2026000123", profileService.getDashboard(userDetails).getCustomerNumber());
		assertEquals("********9012", profileService.getDashboard(userDetails).getPrimaryAccount().getMaskedAccountNumber());
	}

	@Test
	void profileUpdateOnlyChangesAllowedCustomerFields() {
		CustomerProfileUpdateRequest request = new CustomerProfileUpdateRequest("New", "Name", "0711111111",
				"+94722222222", "New Address", "Lane 2", "Kandy", "Kandy", "Central", "20000", "Sri Lankan");

		profileService.updateProfile(userDetails, request);

		assertEquals("New", customer.getFirstName());
		assertEquals("Name", customer.getLastName());
		assertEquals("+94711111111", customer.getMobileNumber());
		assertEquals("customer@example.com", user.getEmail());
		assertEquals("123456789V", customer.getIdentityNumber());
		assertEquals("123456789012", account.getAccountNumber());
		assertEquals(AccountStatus.PENDING_ACTIVATION, account.getAccountStatus());
		assertEquals(new BigDecimal("1000.00"), account.getAvailableBalance());
	}

	@Test
	void customerCannotViewAnotherCustomersAccount() {
		Customer otherCustomer = new Customer(user, "CUS2026000999", "Other", "Customer", LocalDate.of(1990, 1, 1),
				Gender.OTHER, IdentityType.NATIONAL_ID, "987654321V", "+94799999999", "Other Address", "Galle");
		setId(otherCustomer, 99L);
		BankAccount otherAccount = new BankAccount(otherCustomer, "999999999999", AccountType.CURRENT, "GAL001");
		setId(otherAccount, 88L);
		bankAccountRepository.accountsByNumber.put("999999999999", otherAccount);

		assertThrows(AccountAccessDeniedException.class,
				() -> profileService.getAccountDetails(userDetails, "999999999999"));
	}

	@Test
	void ownAccountDetailsAreReturned() {
		assertTrue(profileService.getAccountDetails(userDetails, "123456789012").getMaskedAccountNumber()
				.endsWith("9012"));
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

	private static void setCreatedAt(Object entity, LocalDateTime createdAt) {
		try {
			java.lang.reflect.Field field = entity.getClass().getSuperclass().getDeclaredField("createdAt");
			field.setAccessible(true);
			field.set(entity, createdAt);
		}
		catch (ReflectiveOperationException ex) {
			throw new IllegalStateException("Could not set test createdAt.", ex);
		}
	}

	static class FakeCustomerRepository implements InvocationHandler {

		private final Map<Long, Customer> customersByUserId = new HashMap<>();

		CustomerRepository proxy() {
			return createProxy(CustomerRepository.class, this);
		}

		@Override
		public Object invoke(Object proxy, Method method, Object[] args) {
			return switch (method.getName()) {
				case "findByUserId" -> Optional.ofNullable(customersByUserId.get((Long) args[0]));
				case "save" -> args[0];
				default -> defaultValue(method.getReturnType());
			};
		}
	}

	static class FakeBankAccountRepository implements InvocationHandler {

		private final Map<Long, List<BankAccount>> accountsByCustomerId = new HashMap<>();
		private final Map<String, BankAccount> accountsByNumber = new HashMap<>();

		BankAccountRepository proxy() {
			return createProxy(BankAccountRepository.class, this);
		}

		@Override
		public Object invoke(Object proxy, Method method, Object[] args) {
			return switch (method.getName()) {
				case "findByCustomerId" -> accountsByCustomerId.getOrDefault((Long) args[0], List.of());
				case "findByAccountNumber" -> Optional.ofNullable(accountsByNumber.get((String) args[0]));
				default -> defaultValue(method.getReturnType());
			};
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
}
