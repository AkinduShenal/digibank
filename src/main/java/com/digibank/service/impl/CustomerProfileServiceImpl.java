package com.digibank.service.impl;

import com.digibank.dto.customer.AccountSummaryView;
import com.digibank.dto.customer.CustomerDashboardView;
import com.digibank.dto.customer.CustomerProfileUpdateRequest;
import com.digibank.dto.customer.CustomerProfileView;
import com.digibank.entity.BankAccount;
import com.digibank.entity.Customer;
import com.digibank.enums.Gender;
import com.digibank.exception.AccountAccessDeniedException;
import com.digibank.exception.CustomerProfileNotFoundException;
import com.digibank.repository.BankAccountRepository;
import com.digibank.repository.CustomerRepository;
import com.digibank.security.CustomUserDetails;
import com.digibank.service.CustomerProfileService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.Period;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

@Service
public class CustomerProfileServiceImpl implements CustomerProfileService {

	private final CustomerRepository customerRepository;
	private final BankAccountRepository bankAccountRepository;
	private static final String BOY_AVATAR_PATH = "/images/avatar/boy-avatar.svg";
	private static final String GIRL_AVATAR_PATH = "/images/avatar/girl-avatar.svg";
	private static final String NEUTRAL_AVATAR_PATH = "/images/avatar/neutral-avatar.svg";

	public CustomerProfileServiceImpl(CustomerRepository customerRepository, BankAccountRepository bankAccountRepository) {
		this.customerRepository = customerRepository;
		this.bankAccountRepository = bankAccountRepository;
	}

	@Override
	@Transactional(readOnly = true)
	public CustomerDashboardView getDashboard(CustomUserDetails userDetails) {
		Customer customer = currentCustomer(userDetails);
		List<AccountSummaryView> accounts = accountViews(customer);
		AccountSummaryView primaryAccount = accounts.isEmpty() ? null : accounts.get(0);
		return new CustomerDashboardView(customer.getFullName(), customer.getCustomerNumber(), initials(customer),
				profileImagePath(customer), customer.getUser().getLastLoginAt(), primaryAccount);
	}

	@Override
	@Transactional(readOnly = true)
	public CustomerProfileView getProfile(CustomUserDetails userDetails) {
		return profileView(currentCustomer(userDetails));
	}

	@Override
	@Transactional(readOnly = true)
	public CustomerProfileUpdateRequest getProfileUpdateRequest(CustomUserDetails userDetails) {
		Customer customer = currentCustomer(userDetails);
		return new CustomerProfileUpdateRequest(customer.getFirstName(), customer.getLastName(),
				customer.getMobileNumber(), customer.getAlternativePhone(), customer.getAddressLine1(),
				customer.getAddressLine2(), customer.getCity(), customer.getDistrict(), customer.getProvince(),
				customer.getPostalCode(), customer.getNationality());
	}

	@Override
	@Transactional
	public CustomerProfileView updateProfile(CustomUserDetails userDetails, CustomerProfileUpdateRequest request) {
		Customer customer = currentCustomer(userDetails);
		customer.setFirstName(trim(request.getFirstName()));
		customer.setLastName(trim(request.getLastName()));
		customer.setMobileNumber(toSriLankanMobile(trim(request.getMobileNumber())));
		customer.setAlternativePhone(toSriLankanMobile(trim(request.getAlternativePhone())));
		customer.setAddressLine1(trim(request.getAddressLine1()));
		customer.setAddressLine2(trim(request.getAddressLine2()));
		customer.setCity(trim(request.getCity()));
		customer.setDistrict(trim(request.getDistrict()));
		customer.setProvince(trim(request.getProvince()));
		customer.setPostalCode(trim(request.getPostalCode()));
		customer.setNationality(trim(request.getNationality()));
		Customer saved = customerRepository.save(customer);
		return profileView(saved);
	}

	@Override
	@Transactional(readOnly = true)
	public AccountSummaryView getAccountDetails(CustomUserDetails userDetails, String accountNumber) {
		Customer customer = currentCustomer(userDetails);
		BankAccount account = bankAccountRepository.findByAccountNumber(accountNumber)
				.orElseThrow(AccountAccessDeniedException::new);
		if (account.getCustomer() == null || !Objects.equals(account.getCustomer().getId(), customer.getId())) {
			throw new AccountAccessDeniedException();
		}
		return accountView(account);
	}

	private Customer currentCustomer(CustomUserDetails userDetails) {
		if (userDetails == null || userDetails.getUserId() == null) {
			throw new CustomerProfileNotFoundException();
		}
		return customerRepository.findByUserId(userDetails.getUserId())
				.orElseThrow(CustomerProfileNotFoundException::new);
	}

	private CustomerProfileView profileView(Customer customer) {
		return new CustomerProfileView(customer.getFullName(), customer.getCustomerNumber(),
				maskIdentity(customer.getIdentityNumber()), customer.getIdentityType(), customer.getDateOfBirth(),
				age(customer.getDateOfBirth()), customer.getGender(), valueOrDash(customer.getNationality()),
				customer.getUser().getEmail(), customer.getMobileNumber(), valueOrDash(customer.getAlternativePhone()),
				fullAddress(customer), customer.getStatus(), customer.getCreatedAt(), initials(customer),
				profileImagePath(customer), accountViews(customer));
	}

	private List<AccountSummaryView> accountViews(Customer customer) {
		return bankAccountRepository.findByCustomerId(customer.getId()).stream()
				.sorted(Comparator.comparing(BankAccount::getOpenedAt, Comparator.nullsLast(Comparator.naturalOrder())))
				.map(this::accountView)
				.toList();
	}

	private AccountSummaryView accountView(BankAccount account) {
		return new AccountSummaryView(account.getAccountNumber(), maskAccountNumber(account.getAccountNumber()),
				account.getAccountType(), account.getAccountStatus(), account.getCurrencyCode(),
				account.getAvailableBalance(), account.getCurrentBalance(), account.getBranchCode(),
				account.getOpenedAt());
	}

	private int age(LocalDate dateOfBirth) {
		if (dateOfBirth == null) {
			return 0;
		}
		return Period.between(dateOfBirth, LocalDate.now()).getYears();
	}

	private String initials(Customer customer) {
		String first = firstLetter(customer.getFirstName());
		String last = firstLetter(customer.getLastName());
		String initials = first + last;
		return initials.isBlank() ? "DB" : initials.toUpperCase();
	}

	private String profileImagePath(Customer customer) {
		String uploadedPath = trim(customer.getProfileImagePath());
		if (uploadedPath != null) {
			return uploadedPath;
		}
		Gender gender = customer.getGender();
		if (gender == Gender.MALE) {
			return BOY_AVATAR_PATH;
		}
		if (gender == Gender.FEMALE) {
			return GIRL_AVATAR_PATH;
		}
		return NEUTRAL_AVATAR_PATH;
	}

	private String firstLetter(String value) {
		String trimmed = trim(value);
		return trimmed == null ? "" : trimmed.substring(0, 1);
	}

	private String fullAddress(Customer customer) {
		return Stream.of(customer.getAddressLine1(), customer.getAddressLine2(), customer.getCity(),
						customer.getDistrict(), customer.getProvince(), customer.getPostalCode(), customer.getCountry())
				.map(this::trim)
				.filter(Objects::nonNull)
				.reduce((left, right) -> left + ", " + right)
				.orElse("-");
	}

	private String maskIdentity(String identityNumber) {
		return maskTrailing(identityNumber, 3);
	}

	private String maskAccountNumber(String accountNumber) {
		return maskTrailing(accountNumber, 4);
	}

	private String maskTrailing(String value, int visibleDigits) {
		String trimmed = trim(value);
		if (trimmed == null || trimmed.length() <= visibleDigits) {
			return "****";
		}
		return "*".repeat(trimmed.length() - visibleDigits) + trimmed.substring(trimmed.length() - visibleDigits);
	}

	private String valueOrDash(String value) {
		String trimmed = trim(value);
		return trimmed == null ? "-" : trimmed;
	}

	private String trim(String value) {
		if (value == null) {
			return null;
		}
		String trimmed = value.trim();
		return trimmed.isEmpty() ? null : trimmed;
	}

	private String toSriLankanMobile(String mobileNumber) {
		if (mobileNumber == null || mobileNumber.startsWith("+94")) {
			return mobileNumber;
		}
		if (mobileNumber.startsWith("0")) {
			return "+94" + mobileNumber.substring(1);
		}
		return mobileNumber;
	}
}
