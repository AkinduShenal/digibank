package com.digibank.service.impl;

import com.digibank.dto.auth.CustomerRegistrationRequest;
import com.digibank.dto.auth.RegistrationResult;
import com.digibank.entity.BankAccount;
import com.digibank.entity.Customer;
import com.digibank.entity.User;
import com.digibank.enums.AccountStatus;
import com.digibank.enums.AccountType;
import com.digibank.enums.CurrencyCode;
import com.digibank.enums.Role;
import com.digibank.exception.DuplicateEmailException;
import com.digibank.exception.DuplicateIdentityException;
import com.digibank.exception.DuplicateUsernameException;
import com.digibank.exception.InvalidInitialDepositException;
import com.digibank.exception.PasswordMismatchException;
import com.digibank.exception.PinMismatchException;
import com.digibank.exception.RegistrationException;
import com.digibank.exception.UnderageCustomerException;
import com.digibank.repository.BankAccountRepository;
import com.digibank.repository.CustomerRepository;
import com.digibank.repository.UserRepository;
import com.digibank.security.TransactionPinEncoder;
import com.digibank.service.AccountNumberGenerator;
import com.digibank.service.CustomerNumberGenerator;
import com.digibank.service.CustomerRegistrationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.util.Locale;

@Service
public class CustomerRegistrationServiceImpl implements CustomerRegistrationService {

	private static final Logger LOGGER = LoggerFactory.getLogger(CustomerRegistrationServiceImpl.class);

	private static final int MINIMUM_AGE = 18;
	private static final String DEFAULT_COUNTRY = "Sri Lanka";
	private static final BigDecimal SAVINGS_MINIMUM_DEPOSIT = new BigDecimal("1000.00");
	private static final BigDecimal CURRENT_MINIMUM_DEPOSIT = new BigDecimal("5000.00");

	private final UserRepository userRepository;
	private final CustomerRepository customerRepository;
	private final BankAccountRepository bankAccountRepository;
	private final PasswordEncoder passwordEncoder;
	private final TransactionPinEncoder transactionPinEncoder;
	private final CustomerNumberGenerator customerNumberGenerator;
	private final AccountNumberGenerator accountNumberGenerator;

	public CustomerRegistrationServiceImpl(UserRepository userRepository, CustomerRepository customerRepository,
			BankAccountRepository bankAccountRepository, PasswordEncoder passwordEncoder,
			TransactionPinEncoder transactionPinEncoder, CustomerNumberGenerator customerNumberGenerator,
			AccountNumberGenerator accountNumberGenerator) {
		this.userRepository = userRepository;
		this.customerRepository = customerRepository;
		this.bankAccountRepository = bankAccountRepository;
		this.passwordEncoder = passwordEncoder;
		this.transactionPinEncoder = transactionPinEncoder;
		this.customerNumberGenerator = customerNumberGenerator;
		this.accountNumberGenerator = accountNumberGenerator;
	}

	@Override
	@Transactional
	public RegistrationResult register(CustomerRegistrationRequest request) {
		LOGGER.info("Customer registration started.");
		try {
			NormalizedRegistrationData data = normalize(request);

			validateAge(data.dateOfBirth());
			validatePasswordConfirmation(data.password(), data.confirmPassword());
			validatePinConfirmation(data.transactionPin(), data.confirmTransactionPin());
			validateInitialDeposit(data.accountType(), data.initialDeposit());
			validateUniqueness(data.username(), data.email(), data.identityNumber());

			String customerNumber = customerNumberGenerator.generateUniqueCustomerNumber();
			String accountNumber = accountNumberGenerator.generateUniqueAccountNumber();
			LocalDateTime now = LocalDateTime.now();

			User user = new User(data.username(), data.email(), passwordEncoder.encode(data.password()));
			user.setTransactionPinHash(transactionPinEncoder.encode(data.transactionPin()));
			user.setRole(Role.CUSTOMER);
			user.setEnabled(true);
			user.setAccountNonLocked(true);

			Customer customer = new Customer(user, customerNumber, data.firstName(), data.lastName(),
					data.dateOfBirth(), data.gender(), data.identityType(), data.identityNumber(),
					data.mobileNumber(), data.addressLine1(), data.city());
			customer.setAlternativePhone(data.alternativePhone());
			customer.setAddressLine2(data.addressLine2());
			customer.setDistrict(data.district());
			customer.setProvince(data.province());
			customer.setPostalCode(data.postalCode());
			customer.setCountry(data.country());
			customer.setNationality(data.nationality());
			customer.setTermsAcceptedAt(now);
			customer.setPrivacyAcceptedAt(now);

			BankAccount bankAccount = new BankAccount(customer, accountNumber, data.accountType(), data.branchCode());
			bankAccount.setAvailableBalance(data.initialDeposit());
			bankAccount.setCurrentBalance(data.initialDeposit());
			bankAccount.setOpenedAt(now);
			customer.addBankAccount(bankAccount);

			userRepository.save(user);
			customerRepository.save(customer);
			bankAccountRepository.save(bankAccount);

			LOGGER.info("Customer registration succeeded for customer number {}.", customerNumber);
			return new RegistrationResult(customerNumber, accountNumber, bankAccount.getAccountType(),
					CurrencyCode.LKR, AccountStatus.PENDING_ACTIVATION, customer.getFullName());
		}
		catch (RegistrationException ex) {
			LOGGER.warn("Customer registration failed by business validation: {}.", ex.getClass().getSimpleName());
			throw ex;
		}
		catch (RuntimeException ex) {
			LOGGER.error("Customer registration failed by system error: {}.", ex.getClass().getSimpleName());
			throw ex;
		}
	}

	private NormalizedRegistrationData normalize(CustomerRegistrationRequest request) {
		return new NormalizedRegistrationData(
				trim(request.getFirstName()),
				trim(request.getLastName()),
				lowercase(request.getEmail()),
				toSriLankanMobile(trim(request.getMobileNumber())),
				toSriLankanMobile(trim(request.getAlternativePhone())),
				request.getDateOfBirth(),
				request.getGender(),
				request.getIdentityType(),
				uppercase(request.getIdentityNumber()),
				trim(request.getNationality()),
				trim(request.getAddressLine1()),
				trim(request.getAddressLine2()),
				trim(request.getCity()),
				trim(request.getDistrict()),
				trim(request.getProvince()),
				trim(request.getPostalCode()),
				defaultCountry(trim(request.getCountry())),
				request.getAccountType(),
				trim(request.getBranchCode()),
				request.getInitialDeposit(),
				lowercase(request.getUsername()),
				request.getPassword(),
				request.getConfirmPassword(),
				request.getTransactionPin(),
				request.getConfirmTransactionPin());
	}

	private void validateAge(LocalDate dateOfBirth) {
		if (dateOfBirth == null || Period.between(dateOfBirth, LocalDate.now()).getYears() < MINIMUM_AGE) {
			throw new UnderageCustomerException();
		}
	}

	private void validatePasswordConfirmation(String password, String confirmPassword) {
		if (password == null || !password.equals(confirmPassword)) {
			throw new PasswordMismatchException();
		}
	}

	private void validatePinConfirmation(String transactionPin, String confirmTransactionPin) {
		if (transactionPin == null || !transactionPin.equals(confirmTransactionPin)) {
			throw new PinMismatchException();
		}
	}

	private void validateInitialDeposit(AccountType accountType, BigDecimal initialDeposit) {
		if (initialDeposit == null || initialDeposit.compareTo(BigDecimal.ZERO) < 0) {
			throw new InvalidInitialDepositException("Initial deposit cannot be negative.");
		}
		if (accountType == AccountType.SAVINGS && initialDeposit.compareTo(SAVINGS_MINIMUM_DEPOSIT) < 0) {
			throw new InvalidInitialDepositException(
					"An initial deposit of at least LKR 1,000 is required for a savings account.");
		}
		if (accountType == AccountType.CURRENT && initialDeposit.compareTo(CURRENT_MINIMUM_DEPOSIT) < 0) {
			throw new InvalidInitialDepositException(
					"An initial deposit of at least LKR 5,000 is required for a current account.");
		}
	}

	private void validateUniqueness(String username, String email, String identityNumber) {
		if (userRepository.existsByUsernameIgnoreCase(username)) {
			throw new DuplicateUsernameException(username);
		}
		if (userRepository.existsByEmailIgnoreCase(email)) {
			throw new DuplicateEmailException(email);
		}
		if (customerRepository.existsByIdentityNumberIgnoreCase(identityNumber)) {
			throw new DuplicateIdentityException();
		}
	}

	private String trim(String value) {
		if (value == null) {
			return null;
		}
		String trimmed = value.trim();
		return trimmed.isEmpty() ? null : trimmed;
	}

	private String lowercase(String value) {
		String trimmed = trim(value);
		return trimmed == null ? null : trimmed.toLowerCase(Locale.ROOT);
	}

	private String uppercase(String value) {
		String trimmed = trim(value);
		return trimmed == null ? null : trimmed.toUpperCase(Locale.ROOT);
	}

	private String defaultCountry(String country) {
		return country == null ? DEFAULT_COUNTRY : country;
	}

	private String toSriLankanMobile(String mobileNumber) {
		if (mobileNumber == null) {
			return null;
		}
		if (mobileNumber.startsWith("+94")) {
			return mobileNumber;
		}
		if (mobileNumber.startsWith("0")) {
			return "+94" + mobileNumber.substring(1);
		}
		return mobileNumber;
	}

	private record NormalizedRegistrationData(
			String firstName,
			String lastName,
			String email,
			String mobileNumber,
			String alternativePhone,
			LocalDate dateOfBirth,
			com.digibank.enums.Gender gender,
			com.digibank.enums.IdentityType identityType,
			String identityNumber,
			String nationality,
			String addressLine1,
			String addressLine2,
			String city,
			String district,
			String province,
			String postalCode,
			String country,
			AccountType accountType,
			String branchCode,
			BigDecimal initialDeposit,
			String username,
			String password,
			String confirmPassword,
			String transactionPin,
			String confirmTransactionPin) {
	}
}
