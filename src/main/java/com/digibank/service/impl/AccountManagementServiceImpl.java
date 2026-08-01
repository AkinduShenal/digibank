package com.digibank.service.impl;

import com.digibank.dto.customer.AccountSummaryView;
import com.digibank.dto.staff.StaffCustomerView;
import com.digibank.entity.AuditLog;
import com.digibank.entity.BankAccount;
import com.digibank.entity.Customer;
import com.digibank.enums.AccountStatus;
import com.digibank.enums.AccountType;
import com.digibank.enums.CustomerStatus;
import com.digibank.exception.AccountAccessDeniedException;
import com.digibank.exception.CustomerProfileNotFoundException;
import com.digibank.exception.InvalidAccountClosureException;
import com.digibank.exception.InvalidAccountStateTransitionException;
import com.digibank.repository.AuditLogRepository;
import com.digibank.repository.BankAccountRepository;
import com.digibank.repository.CustomerRepository;
import com.digibank.service.AccountManagementService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

@Service
public class AccountManagementServiceImpl implements AccountManagementService {

	private static final String TARGET_ACCOUNT = "BANK_ACCOUNT";
	private static final String TARGET_CUSTOMER = "CUSTOMER";

	private final BankAccountRepository bankAccountRepository;
	private final CustomerRepository customerRepository;
	private final AuditLogRepository auditLogRepository;

	public AccountManagementServiceImpl(BankAccountRepository bankAccountRepository,
			CustomerRepository customerRepository, AuditLogRepository auditLogRepository) {
		this.bankAccountRepository = bankAccountRepository;
		this.customerRepository = customerRepository;
		this.auditLogRepository = auditLogRepository;
	}

	@Override
	@Transactional
	public AccountSummaryView activateAccount(String actorUsername, String accountNumber) {
		BankAccount account = account(accountNumber);
		requireStatus(account, AccountStatus.PENDING_ACTIVATION, "Only pending accounts can be activated.");
		Customer customer = account.getCustomer();
		if (customer == null) {
			throw new InvalidAccountStateTransitionException("The account is not linked to a customer profile.");
		}
		if (customer.getStatus() == CustomerStatus.PENDING_VERIFICATION) {
			CustomerStatus previousCustomerStatus = customer.getStatus();
			customer.setStatus(CustomerStatus.ACTIVE);
			customer.getUser().setEnabled(true);
			auditCustomer(actorUsername, customer, "CUSTOMER_VERIFIED", previousCustomerStatus.name(),
					CustomerStatus.ACTIVE.name(), "Verified during initial account activation.");
		}
		return transitionAccount(actorUsername, account, AccountStatus.ACTIVE, "ACCOUNT_ACTIVATED", null);
	}

	@Override
	@Transactional
	public AccountSummaryView freezeAccount(String actorUsername, String accountNumber, String reason) {
		BankAccount account = account(accountNumber);
		requireStatus(account, AccountStatus.ACTIVE, "Only active accounts can be frozen.");
		account.setFrozenAt(LocalDateTime.now());
		return transitionAccount(actorUsername, account, AccountStatus.FROZEN, "ACCOUNT_FROZEN", requiredReason(reason));
	}

	@Override
	@Transactional
	public AccountSummaryView unfreezeAccount(String actorUsername, String accountNumber, String reason) {
		BankAccount account = account(accountNumber);
		requireStatus(account, AccountStatus.FROZEN, "Only frozen accounts can be unfrozen.");
		account.setFrozenAt(null);
		return transitionAccount(actorUsername, account, AccountStatus.ACTIVE, "ACCOUNT_UNFROZEN", requiredReason(reason));
	}

	@Override
	@Transactional
	public AccountSummaryView deactivateAccount(String actorUsername, String accountNumber, String reason) {
		String safeReason = requiredReason(reason);
		BankAccount account = account(accountNumber);
		if (account.getAccountStatus() != AccountStatus.ACTIVE && account.getAccountStatus() != AccountStatus.FROZEN) {
			throw new InvalidAccountStateTransitionException("Only active or frozen accounts can be deactivated.");
		}
		Customer customer = account.getCustomer();
		com.digibank.enums.CustomerStatus previousCustomerStatus = customer.getStatus();
		customer.setStatus(com.digibank.enums.CustomerStatus.DEACTIVATED);
		customer.setDeactivatedAt(LocalDateTime.now());
		customer.getUser().setEnabled(false);
		auditCustomer(actorUsername, customer, "CUSTOMER_DEACTIVATED", previousCustomerStatus.name(), "DEACTIVATED",
				safeReason);
		return transitionAccount(actorUsername, account, AccountStatus.DEACTIVATED, "ACCOUNT_DEACTIVATED", safeReason);
	}

	@Override
	@Transactional
	public AccountSummaryView closeAccount(String actorUsername, String customerNumber, String accountNumber,
			String reason) {
		String safeReason = requiredReason(reason);
		Customer customer = customerRepository.findByCustomerNumber(customerNumber)
				.orElseThrow(CustomerProfileNotFoundException::new);
		BankAccount account = account(accountNumber);
		if (account.getCustomer() == null || !Objects.equals(account.getCustomer().getId(), customer.getId())) {
			throw new AccountAccessDeniedException();
		}
		if (account.getAccountStatus() != AccountStatus.ACTIVE && account.getAccountStatus() != AccountStatus.FROZEN) {
			throw new InvalidAccountStateTransitionException("Only active or frozen accounts can be closed.");
		}
		if (isPositive(account.getAvailableBalance()) || isPositive(account.getCurrentBalance())) {
			throw new InvalidAccountClosureException("Account balance must be zero before closure.");
		}
		account.setClosedAt(LocalDateTime.now());
		account.setClosureReason(safeReason);
		return transitionAccount(actorUsername, account, AccountStatus.CLOSED, "ACCOUNT_CLOSED", safeReason);
	}

	@Override
	@Transactional
	public AccountSummaryView reactivateEligibleAccount(String actorUsername, String accountNumber, String reason) {
		String safeReason = requiredReason(reason);
		BankAccount account = account(accountNumber);
		requireStatus(account, AccountStatus.DEACTIVATED, "Only deactivated accounts can be reactivated.");
		if (account.getClosedAt() != null) {
			throw new InvalidAccountStateTransitionException("Closed accounts cannot be reactivated.");
		}
		Customer customer = account.getCustomer();
		com.digibank.enums.CustomerStatus previousCustomerStatus = customer.getStatus();
		customer.setStatus(com.digibank.enums.CustomerStatus.ACTIVE);
		customer.setDeactivatedAt(null);
		customer.getUser().setEnabled(true);
		auditCustomer(actorUsername, customer, "CUSTOMER_REACTIVATED", previousCustomerStatus.name(), "ACTIVE",
				safeReason);
		return transitionAccount(actorUsername, account, AccountStatus.ACTIVE, "ACCOUNT_REACTIVATED", safeReason);
	}

	@Override
	@Transactional
	public AccountSummaryView changeAccountType(String actorUsername, String accountNumber, AccountType accountType,
			String reason) {
		BankAccount account = account(accountNumber);
		if (account.getAccountStatus() != AccountStatus.PENDING_ACTIVATION) {
			throw new InvalidAccountStateTransitionException("Account type can only change before activation.");
		}
		AccountType previousType = account.getAccountType();
		account.setAccountType(accountType);
		BankAccount saved = bankAccountRepository.save(account);
		audit(actorUsername, "ACCOUNT_TYPE_CHANGED", TARGET_ACCOUNT, saved.getAccountNumber(), previousType.name(),
				accountType.name(), requiredReason(reason));
		return accountView(saved);
	}

	@Override
	@Transactional
	public AccountSummaryView requestAccountClosure(String actorUsername, String accountNumber, String reason) {
		BankAccount account = account(accountNumber);
		audit(actorUsername, "ACCOUNT_CLOSURE_REQUESTED", TARGET_ACCOUNT, account.getAccountNumber(),
				account.getAccountStatus().name(), account.getAccountStatus().name(), requiredReason(reason));
		return accountView(account);
	}

	@Override
	@Transactional(readOnly = true)
	public List<StaffCustomerView> getCustomerRecords() {
		return customerRepository.findAll().stream()
				.sorted(Comparator.comparing(Customer::getCustomerNumber))
				.map(this::staffCustomerView)
				.toList();
	}

	@Override
	@Transactional(readOnly = true)
	public StaffCustomerView getCustomerDetails(String customerNumber) {
		return customerRepository.findByCustomerNumber(customerNumber)
				.map(this::staffCustomerView)
				.orElseThrow(CustomerProfileNotFoundException::new);
	}

	private AccountSummaryView transitionAccount(String actorUsername, BankAccount account, AccountStatus newStatus,
			String action, String reason) {
		AccountStatus previousStatus = account.getAccountStatus();
		account.setAccountStatus(newStatus);
		BankAccount saved = bankAccountRepository.save(account);
		audit(actorUsername, action, TARGET_ACCOUNT, saved.getAccountNumber(), previousStatus.name(), newStatus.name(),
				reason);
		return accountView(saved);
	}

	private void auditCustomer(String actorUsername, Customer customer, String action, String previousStatus,
			String newStatus, String reason) {
		customerRepository.save(customer);
		audit(actorUsername, action, TARGET_CUSTOMER, customer.getCustomerNumber(), previousStatus, newStatus, reason);
	}

	private void audit(String actorUsername, String action, String targetType, String targetIdentifier,
			String previousStatus, String newStatus, String reason) {
		AuditLog auditLog = new AuditLog(actorUsername, action, targetType, targetIdentifier, previousStatus,
				newStatus, reason, LocalDateTime.now());
		auditLogRepository.save(auditLog);
	}

	private BankAccount account(String accountNumber) {
		return bankAccountRepository.findByAccountNumber(accountNumber)
				.orElseThrow(AccountAccessDeniedException::new);
	}

	private void requireStatus(BankAccount account, AccountStatus expectedStatus, String message) {
		if (account.getAccountStatus() != expectedStatus) {
			throw new InvalidAccountStateTransitionException(message);
		}
	}

	private boolean isPositive(BigDecimal value) {
		return value != null && value.compareTo(BigDecimal.ZERO) > 0;
	}

	private String requiredReason(String reason) {
		String trimmed = trim(reason);
		if (trimmed == null) {
			throw new InvalidAccountClosureException("A documented reason is required.");
		}
		return trimmed;
	}

	private StaffCustomerView staffCustomerView(Customer customer) {
		return new StaffCustomerView(customer.getCustomerNumber(), customer.getFullName(), customer.getUser().getEmail(),
				customer.getMobileNumber(), customer.getStatus(), customer.getUser().isEnabled(),
				customer.getCreatedAt(), bankAccountRepository.findByCustomerId(customer.getId()).stream()
						.map(this::accountView)
						.toList());
	}

	private AccountSummaryView accountView(BankAccount account) {
		return new AccountSummaryView(account.getAccountNumber(), maskAccountNumber(account.getAccountNumber()),
				account.getAccountType(), account.getAccountStatus(), account.getCurrencyCode(),
				account.getAvailableBalance(), account.getCurrentBalance(), account.getBranchCode(),
				account.getOpenedAt());
	}

	private String maskAccountNumber(String accountNumber) {
		String trimmed = trim(accountNumber);
		if (trimmed == null || trimmed.length() <= 4) {
			return "****";
		}
		return "*".repeat(trimmed.length() - 4) + trimmed.substring(trimmed.length() - 4);
	}

	private String trim(String value) {
		if (value == null) {
			return null;
		}
		String trimmed = value.trim();
		return trimmed.isEmpty() ? null : trimmed;
	}
}
