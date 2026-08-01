package com.digibank.service;

import com.digibank.dto.auth.CustomerRegistrationRequest;
import com.digibank.dto.auth.RegistrationResult;
import com.digibank.entity.BankAccount;
import com.digibank.entity.Customer;
import com.digibank.entity.User;
import com.digibank.enums.AccountStatus;
import com.digibank.enums.AccountType;
import com.digibank.enums.CurrencyCode;
import com.digibank.enums.Gender;
import com.digibank.enums.IdentityType;
import com.digibank.exception.DuplicateEmailException;
import com.digibank.exception.DuplicateIdentityException;
import com.digibank.exception.DuplicateUsernameException;
import com.digibank.exception.InvalidInitialDepositException;
import com.digibank.exception.PasswordMismatchException;
import com.digibank.exception.PinMismatchException;
import com.digibank.exception.UnderageCustomerException;
import com.digibank.repository.BankAccountRepository;
import com.digibank.repository.CustomerRepository;
import com.digibank.repository.UserRepository;
import com.digibank.security.TransactionPinEncoder;
import com.digibank.service.impl.CustomerRegistrationServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CustomerRegistrationServiceImplTest {

	private FakeUserRepository userRepository;
	private FakeCustomerRepository customerRepository;
	private FakeBankAccountRepository bankAccountRepository;
	private CustomerRegistrationService registrationService;

	@BeforeEach
	void setUp() {
		userRepository = new FakeUserRepository();
		customerRepository = new FakeCustomerRepository();
		bankAccountRepository = new FakeBankAccountRepository();

		PasswordEncoder passwordEncoder = new TestPasswordEncoder();
		TransactionPinEncoder transactionPinEncoder = rawTransactionPin -> "encoded-pin:" + rawTransactionPin;
		CustomerNumberGenerator customerNumberGenerator = new CustomerNumberGenerator(customerRepository.proxy()) {
			@Override
			public String generateUniqueCustomerNumber() {
				return "CUS2026000123";
			}
		};
		AccountNumberGenerator accountNumberGenerator = new AccountNumberGenerator(bankAccountRepository.proxy()) {
			@Override
			public String generateUniqueAccountNumber() {
				return "123456789012";
			}
		};

		registrationService = new CustomerRegistrationServiceImpl(userRepository.proxy(), customerRepository.proxy(),
				bankAccountRepository.proxy(), passwordEncoder, transactionPinEncoder, customerNumberGenerator,
				accountNumberGenerator);
	}

	@Test
	void registerCreatesSavingsCustomerAccount() {
		CustomerRegistrationRequest request = validRequest(AccountType.SAVINGS, new BigDecimal("1000.00"));

		RegistrationResult result = registrationService.register(request);

		assertEquals("CUS2026000123", result.getCustomerNumber());
		assertEquals("123456789012", result.getAccountNumber());
		assertEquals(AccountType.SAVINGS, result.getAccountType());
		assertEquals(CurrencyCode.LKR, result.getCurrency());
		assertEquals(AccountStatus.PENDING_ACTIVATION, result.getAccountStatus());
		assertEquals("Lakshitha Dilshan", result.getCustomerFullName());

		User savedUser = userRepository.savedUsers.getFirst();
		Customer savedCustomer = customerRepository.savedCustomers.getFirst();
		BankAccount savedAccount = bankAccountRepository.savedAccounts.getFirst();
		assertEquals("lakshitha", savedUser.getUsername());
		assertEquals("student@example.com", savedUser.getEmail());
		assertEquals("encoded-password:Password@123", savedUser.getPasswordHash());
		assertEquals("encoded-pin:2468", savedUser.getTransactionPinHash());
		assertFalse(savedUser.isEnabled());
		assertTrue(savedUser.isAccountNonLocked());
		assertEquals(com.digibank.enums.CustomerStatus.PENDING_VERIFICATION, savedCustomer.getStatus());
		assertEquals("+94712345678", savedCustomer.getMobileNumber());
		assertEquals("123456789V", savedCustomer.getIdentityNumber());
		assertEquals("Sri Lanka", savedCustomer.getCountry());
		assertEquals(new BigDecimal("1000.00"), savedAccount.getAvailableBalance());
		assertEquals(new BigDecimal("1000.00"), savedAccount.getCurrentBalance());
		assertEquals(savedCustomer, savedAccount.getCustomer());
	}

	@Test
	void registerCreatesCurrentCustomerAccount() {
		CustomerRegistrationRequest request = validRequest(AccountType.CURRENT, new BigDecimal("5000.00"));

		RegistrationResult result = registrationService.register(request);

		assertEquals(AccountType.CURRENT, result.getAccountType());
		BankAccount savedAccount = bankAccountRepository.savedAccounts.getFirst();
		assertEquals(AccountType.CURRENT, savedAccount.getAccountType());
		assertEquals(new BigDecimal("5000.00"), savedAccount.getCurrentBalance());
	}

	@Test
	void registerRejectsUnderageCustomer() {
		CustomerRegistrationRequest request = validRequest(AccountType.SAVINGS, new BigDecimal("1000.00"));
		request.setDateOfBirth(LocalDate.now().minusYears(18).plusDays(1));

		assertThrows(UnderageCustomerException.class, () -> registrationService.register(request));

		assertTrue(userRepository.savedUsers.isEmpty());
	}

	@Test
	void registerRejectsDuplicateUsername() {
		CustomerRegistrationRequest request = validRequest(AccountType.SAVINGS, new BigDecimal("1000.00"));
		userRepository.duplicateUsernames.add("lakshitha");

		assertThrows(DuplicateUsernameException.class, () -> registrationService.register(request));

		assertTrue(userRepository.savedUsers.isEmpty());
	}

	@Test
	void registerRejectsDuplicateEmail() {
		CustomerRegistrationRequest request = validRequest(AccountType.SAVINGS, new BigDecimal("1000.00"));
		userRepository.duplicateEmails.add("student@example.com");

		assertThrows(DuplicateEmailException.class, () -> registrationService.register(request));

		assertTrue(userRepository.savedUsers.isEmpty());
	}

	@Test
	void registerRejectsDuplicateIdentity() {
		CustomerRegistrationRequest request = validRequest(AccountType.SAVINGS, new BigDecimal("1000.00"));
		customerRepository.duplicateIdentityNumbers.add("123456789v");

		assertThrows(DuplicateIdentityException.class, () -> registrationService.register(request));

		assertTrue(userRepository.savedUsers.isEmpty());
	}

	@Test
	void registerRejectsInsufficientSavingsDeposit() {
		CustomerRegistrationRequest request = validRequest(AccountType.SAVINGS, new BigDecimal("999.99"));

		assertThrows(InvalidInitialDepositException.class, () -> registrationService.register(request));

		assertEquals(0, userRepository.usernameChecks);
	}

	@Test
	void registerRejectsInsufficientCurrentDeposit() {
		CustomerRegistrationRequest request = validRequest(AccountType.CURRENT, new BigDecimal("4999.99"));

		assertThrows(InvalidInitialDepositException.class, () -> registrationService.register(request));

		assertEquals(0, userRepository.usernameChecks);
	}

	@Test
	void registerRejectsPasswordMismatch() {
		CustomerRegistrationRequest request = validRequest(AccountType.SAVINGS, new BigDecimal("1000.00"));
		request.setConfirmPassword("Different@123");

		assertThrows(PasswordMismatchException.class, () -> registrationService.register(request));

		assertTrue(userRepository.savedUsers.isEmpty());
	}

	@Test
	void registerRejectsPinMismatch() {
		CustomerRegistrationRequest request = validRequest(AccountType.SAVINGS, new BigDecimal("1000.00"));
		request.setConfirmTransactionPin("1357");

		assertThrows(PinMismatchException.class, () -> registrationService.register(request));

		assertTrue(userRepository.savedUsers.isEmpty());
	}

	private CustomerRegistrationRequest validRequest(AccountType accountType, BigDecimal initialDeposit) {
		CustomerRegistrationRequest request = new CustomerRegistrationRequest();
		request.setFirstName(" Lakshitha ");
		request.setLastName(" Dilshan ");
		request.setEmail(" Student@Example.COM ");
		request.setMobileNumber("0712345678");
		request.setAlternativePhone("+94787654321");
		request.setDateOfBirth(LocalDate.now().minusYears(20));
		request.setGender(Gender.MALE);
		request.setIdentityType(IdentityType.NATIONAL_ID);
		request.setIdentityNumber("123456789v");
		request.setNationality("Sri Lankan");
		request.setAddressLine1("No 1 Main Street");
		request.setAddressLine2("Apartment 4");
		request.setCity("Colombo");
		request.setDistrict("Colombo");
		request.setProvince("Western");
		request.setPostalCode("00100");
		request.setCountry(" ");
		request.setAccountType(accountType);
		request.setBranchCode("BR001");
		request.setInitialDeposit(initialDeposit);
		request.setUsername(" Lakshitha ");
		request.setPassword("Password@123");
		request.setConfirmPassword("Password@123");
		request.setTransactionPin("2468");
		request.setConfirmTransactionPin("2468");
		request.setTermsAccepted(true);
		request.setPrivacyAccepted(true);
		return request;
	}

	private static class TestPasswordEncoder implements PasswordEncoder {

		@Override
		public String encode(CharSequence rawPassword) {
			return "encoded-password:" + rawPassword;
		}

		@Override
		public boolean matches(CharSequence rawPassword, String encodedPassword) {
			return encodedPassword.equals(encode(rawPassword));
		}
	}

	private static class FakeUserRepository implements InvocationHandler {

		private final List<User> savedUsers = new ArrayList<>();
		private final Set<String> duplicateUsernames = new HashSet<>();
		private final Set<String> duplicateEmails = new HashSet<>();
		private int usernameChecks;

		UserRepository proxy() {
			return createProxy(UserRepository.class, this);
		}

		@Override
		public Object invoke(Object proxy, java.lang.reflect.Method method, Object[] args) {
			return switch (method.getName()) {
				case "existsByUsernameIgnoreCase" -> {
					usernameChecks++;
					yield duplicateUsernames.contains(normalize((String) args[0]));
				}
				case "existsByEmailIgnoreCase" -> duplicateEmails.contains(normalize((String) args[0]));
				case "findByUsernameIgnoreCase", "findByEmailIgnoreCase" -> Optional.empty();
				case "save" -> {
					savedUsers.add((User) args[0]);
					yield args[0];
				}
				default -> defaultValue(method.getReturnType());
			};
		}
	}

	private static class FakeCustomerRepository implements InvocationHandler {

		private final List<Customer> savedCustomers = new ArrayList<>();
		private final Set<String> duplicateIdentityNumbers = new HashSet<>();

		CustomerRepository proxy() {
			return createProxy(CustomerRepository.class, this);
		}

		@Override
		public Object invoke(Object proxy, java.lang.reflect.Method method, Object[] args) {
			return switch (method.getName()) {
				case "existsByIdentityNumberIgnoreCase" -> duplicateIdentityNumbers.contains(normalize((String) args[0]));
				case "findByCustomerNumber", "findByUserId", "findByIdentityNumberIgnoreCase" -> Optional.empty();
				case "save" -> {
					savedCustomers.add((Customer) args[0]);
					yield args[0];
				}
				default -> defaultValue(method.getReturnType());
			};
		}
	}

	private static class FakeBankAccountRepository implements InvocationHandler {

		private final List<BankAccount> savedAccounts = new ArrayList<>();

		BankAccountRepository proxy() {
			return createProxy(BankAccountRepository.class, this);
		}

		@Override
		public Object invoke(Object proxy, java.lang.reflect.Method method, Object[] args) {
			return switch (method.getName()) {
				case "existsByAccountNumber" -> false;
				case "findByAccountNumber" -> Optional.empty();
				case "findByCustomerId" -> List.of();
				case "save" -> {
					savedAccounts.add((BankAccount) args[0]);
					yield args[0];
				}
				default -> defaultValue(method.getReturnType());
			};
		}
	}

	private static <T> T createProxy(Class<T> type, InvocationHandler invocationHandler) {
		return type.cast(Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[] {type}, invocationHandler));
	}

	private static String normalize(String value) {
		return value == null ? null : value.trim().toLowerCase();
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
