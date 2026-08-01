package com.digibank.service.impl;

import com.digibank.dto.transfer.InternalAccountLookupView;
import com.digibank.dto.transfer.TransferAccountOption;
import com.digibank.dto.transfer.TransferBeneficiaryOption;
import com.digibank.dto.transfer.TransferDetailsView;
import com.digibank.dto.transfer.TransferFormView;
import com.digibank.dto.transfer.TransferListView;
import com.digibank.dto.transfer.TransferRequest;
import com.digibank.entity.AuditLog;
import com.digibank.entity.BankAccount;
import com.digibank.entity.Beneficiary;
import com.digibank.entity.Customer;
import com.digibank.entity.FundTransfer;
import com.digibank.enums.AccountStatus;
import com.digibank.enums.BeneficiaryStatus;
import com.digibank.enums.BeneficiaryType;
import com.digibank.enums.BeneficiaryVerificationStatus;
import com.digibank.enums.CurrencyCode;
import com.digibank.enums.CustomerStatus;
import com.digibank.enums.TransferStatus;
import com.digibank.enums.TransferRecipientType;
import com.digibank.enums.TransferType;
import com.digibank.exception.CustomerProfileNotFoundException;
import com.digibank.exception.TransferException;
import com.digibank.exception.TransferNotFoundException;
import com.digibank.repository.AuditLogRepository;
import com.digibank.repository.BankAccountRepository;
import com.digibank.repository.BeneficiaryRepository;
import com.digibank.repository.CustomerRepository;
import com.digibank.repository.FundTransferRepository;
import com.digibank.service.TransferService;
import com.digibank.util.BeneficiaryConstants;
import com.digibank.util.SensitiveDataMasker;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class TransferServiceImpl implements TransferService {

	private static final int MAX_DESCRIPTION_LENGTH = 140;
	private static final int DIGIBANK_ACCOUNT_NUMBER_LENGTH = 12;

	private final CustomerRepository customerRepository;
	private final BankAccountRepository bankAccountRepository;
	private final BeneficiaryRepository beneficiaryRepository;
	private final FundTransferRepository fundTransferRepository;
	private final AuditLogRepository auditLogRepository;
	private final PasswordEncoder passwordEncoder;
	private final SensitiveDataMasker dataMasker;

	public TransferServiceImpl(CustomerRepository customerRepository, BankAccountRepository bankAccountRepository,
			BeneficiaryRepository beneficiaryRepository, FundTransferRepository fundTransferRepository,
			AuditLogRepository auditLogRepository, PasswordEncoder passwordEncoder, SensitiveDataMasker dataMasker) {
		this.customerRepository = customerRepository;
		this.bankAccountRepository = bankAccountRepository;
		this.beneficiaryRepository = beneficiaryRepository;
		this.fundTransferRepository = fundTransferRepository;
		this.auditLogRepository = auditLogRepository;
		this.passwordEncoder = passwordEncoder;
		this.dataMasker = dataMasker;
	}

	@Override
	@Transactional(readOnly = true)
	public TransferFormView getTransferForm(Long authenticatedUserId) {
		Customer customer = customer(authenticatedUserId);
		List<TransferAccountOption> accounts = bankAccountRepository.findByCustomerId(customer.getId()).stream()
				.filter(account -> account.getAccountStatus() == AccountStatus.ACTIVE)
				.sorted(Comparator.comparing(BankAccount::getAccountNumber))
				.map(account -> new TransferAccountOption(account.getAccountNumber(),
						dataMasker.maskAccountNumber(account.getAccountNumber()), account.getAccountType(),
						account.getAvailableBalance()))
				.toList();
		List<TransferBeneficiaryOption> beneficiaries = beneficiaryRepository
				.findAllByCustomerIdAndStatusAndVerificationStatusOrderByBeneficiaryNameAsc(customer.getId(),
						BeneficiaryStatus.ACTIVE, BeneficiaryVerificationStatus.VERIFIED)
				.stream()
				.map(beneficiary -> new TransferBeneficiaryOption(beneficiary.getId(), beneficiary.getBeneficiaryName(),
						beneficiary.getBankName(), dataMasker.maskAccountNumber(beneficiary.getAccountNumber()),
						beneficiary.getBeneficiaryType(), beneficiary.getTransferLimit()))
				.toList();
		return new TransferFormView(accounts, beneficiaries);
	}

	@Override
	@Transactional(readOnly = true)
	public InternalAccountLookupView lookupInternalAccount(Long authenticatedUserId, String accountNumber) {
		Customer customer = customer(authenticatedUserId);
		requireActiveCustomer(customer);
		try {
			String normalized = normalizeInternalAccountNumber(accountNumber);
			return bankAccountRepository.findByAccountNumber(normalized)
					.filter(account -> isEligibleDirectDestination(customer, account))
					.map(account -> new InternalAccountLookupView(true, accountHolderName(account),
							dataMasker.maskAccountNumber(account.getAccountNumber())))
					.orElseGet(InternalAccountLookupView::unavailable);
		}
		catch (TransferException ex) {
			return InternalAccountLookupView.unavailable();
		}
	}

	@Override
	@Transactional
	public TransferDetailsView transfer(Long authenticatedUserId, String actorUsername, TransferRequest request) {
		Customer customer = customer(authenticatedUserId);
		requireActiveCustomer(customer);
		requireRequest(request);

		Beneficiary beneficiary = null;
		String destinationNumber = null;
		BigDecimal transferLimit;
		if (request.getRecipientType() == TransferRecipientType.SAVED_BENEFICIARY) {
			beneficiary = beneficiaryRepository
					.findByIdAndCustomerIdAndStatusNot(request.getBeneficiaryId(), customer.getId(),
							BeneficiaryStatus.DELETED)
					.orElseThrow(() -> new TransferException("The selected beneficiary is not available."));
			requireTransferableBeneficiary(beneficiary);
			transferLimit = beneficiary.getTransferLimit();
			if (beneficiary.getBeneficiaryType() == BeneficiaryType.INTERNAL) {
				destinationNumber = normalizeInternalAccountNumber(beneficiary.getNormalizedAccountNumber());
			}
		}
		else if (request.getRecipientType() == TransferRecipientType.DIGIBANK_ACCOUNT) {
			destinationNumber = normalizeInternalAccountNumber(request.getDestinationAccountNumber());
			transferLimit = BeneficiaryConstants.MAX_TRANSFER_LIMIT;
		}
		else {
			throw new TransferException("Select a valid transfer recipient type.");
		}

		BigDecimal amount = requireAmount(request.getAmount(), transferLimit);
		requireTransactionPin(customer, request.getTransactionPin());

		String sourceNumber = trim(request.getSourceAccountNumber());
		List<String> accountNumbers = destinationNumber == null
				? List.of(sourceNumber)
				: List.of(sourceNumber, destinationNumber).stream().distinct().sorted().toList();
		Map<String, BankAccount> lockedAccounts = bankAccountRepository
				.findAllByAccountNumberInForUpdate(accountNumbers).stream()
				.collect(Collectors.toMap(BankAccount::getAccountNumber, Function.identity()));
		BankAccount source = lockedAccounts.get(sourceNumber);
		requireSourceAccount(customer, source);
		requireSufficientBalance(source, amount);

		TransferType transferType = request.getRecipientType() == TransferRecipientType.DIGIBANK_ACCOUNT
				|| beneficiary.getBeneficiaryType() == BeneficiaryType.INTERNAL
						? TransferType.INTERNAL : TransferType.EXTERNAL;
		BankAccount destination = transferType == TransferType.INTERNAL ? lockedAccounts.get(destinationNumber) : null;
		if (destination != null || transferType == TransferType.INTERNAL) {
			requireDestinationAccount(customer, source, destination);
		}
		String recipientName = beneficiary == null ? accountHolderName(destination) : beneficiary.getBeneficiaryName();
		String destinationBank = beneficiary == null ? BeneficiaryConstants.DIGIBANK_BANK_NAME : beneficiary.getBankName();
		String destinationAccount = beneficiary == null ? destination.getAccountNumber() : beneficiary.getAccountNumber();
		FundTransfer transfer = new FundTransfer(customer, source, beneficiary, referenceNumber(), transferType, amount,
				trimToLength(request.getDescription(), MAX_DESCRIPTION_LENGTH), recipientName,
				destinationBank, dataMasker.maskAccountNumber(destinationAccount));

		source.setAvailableBalance(source.getAvailableBalance().subtract(amount));
		source.setCurrentBalance(source.getCurrentBalance().subtract(amount));
		if (transferType == TransferType.INTERNAL) {
			destination.setAvailableBalance(destination.getAvailableBalance().add(amount));
			destination.setCurrentBalance(destination.getCurrentBalance().add(amount));
			transfer.setDestinationAccount(destination);
		}
		bankAccountRepository.saveAll(lockedAccounts.values());
		transfer.setSourceBalanceAfter(source.getAvailableBalance());
		transfer.setStatus(TransferStatus.COMPLETED);
		transfer.setCompletedAt(LocalDateTime.now());
		FundTransfer saved = fundTransferRepository.save(transfer);
		audit(actorUsername, saved);
		return details(saved);
	}

	@Override
	@Transactional(readOnly = true)
	public List<TransferListView> getTransferHistory(Long authenticatedUserId) {
		Customer customer = customer(authenticatedUserId);
		return fundTransferRepository.findTop50ByCustomerIdOrderByCreatedAtDesc(customer.getId()).stream()
				.map(this::summary)
				.toList();
	}

	@Override
	@Transactional(readOnly = true)
	public TransferDetailsView getTransfer(Long authenticatedUserId, String referenceNumber) {
		Customer customer = customer(authenticatedUserId);
		return fundTransferRepository.findByReferenceNumberAndCustomerId(trim(referenceNumber), customer.getId())
				.map(this::details)
				.orElseThrow(TransferNotFoundException::new);
	}

	private Customer customer(Long authenticatedUserId) {
		if (authenticatedUserId == null) {
			throw new CustomerProfileNotFoundException();
		}
		return customerRepository.findByUserId(authenticatedUserId)
				.orElseThrow(CustomerProfileNotFoundException::new);
	}

	private void requireActiveCustomer(Customer customer) {
		if (customer.getStatus() != CustomerStatus.ACTIVE || !customer.getUser().isEnabled()) {
			throw new TransferException("Customer access must be active before making a transfer.");
		}
	}

	private void requireRequest(TransferRequest request) {
		if (request == null || trim(request.getSourceAccountNumber()) == null || request.getRecipientType() == null) {
			throw new TransferException("Source account and recipient type are required.");
		}
		if (request.getRecipientType() == TransferRecipientType.SAVED_BENEFICIARY
				&& request.getBeneficiaryId() == null) {
			throw new TransferException("Select a saved beneficiary.");
		}
		if (request.getRecipientType() == TransferRecipientType.DIGIBANK_ACCOUNT
				&& trim(request.getDestinationAccountNumber()) == null) {
			throw new TransferException("Enter the recipient's DigiBank account number.");
		}
	}

	private void requireTransferableBeneficiary(Beneficiary beneficiary) {
		if (beneficiary.getStatus() != BeneficiaryStatus.ACTIVE) {
			throw new TransferException("The selected beneficiary is inactive.");
		}
		if (beneficiary.getVerificationStatus() != BeneficiaryVerificationStatus.VERIFIED) {
			throw new TransferException("The selected beneficiary must be verified before making a transfer.");
		}
	}

	private BigDecimal requireAmount(BigDecimal amount, BigDecimal transferLimit) {
		if (amount == null || amount.signum() <= 0 || amount.scale() > 2) {
			throw new TransferException("Transfer amount must be positive and have at most two decimal places.");
		}
		if (transferLimit == null || amount.compareTo(transferLimit) > 0) {
			throw new TransferException("Transfer amount exceeds the beneficiary transfer limit.");
		}
		return amount.setScale(2);
	}

	private void requireTransactionPin(Customer customer, String transactionPin) {
		String pin = trim(transactionPin);
		String hash = customer.getUser().getTransactionPinHash();
		if (pin == null || hash == null || !passwordEncoder.matches(pin, hash)) {
			throw new TransferException("Transaction PIN is incorrect.");
		}
	}

	private void requireSourceAccount(Customer customer, BankAccount source) {
		if (source == null || source.getCustomer() == null
				|| !Objects.equals(source.getCustomer().getId(), customer.getId())) {
			throw new TransferException("The selected source account is not available.");
		}
		if (source.getAccountStatus() != AccountStatus.ACTIVE) {
			throw new TransferException("The selected source account must be active.");
		}
		if (source.getCurrencyCode() != CurrencyCode.LKR) {
			throw new TransferException("Only LKR accounts are supported for this transfer.");
		}
	}

	private void requireSufficientBalance(BankAccount source, BigDecimal amount) {
		if (source.getAvailableBalance() == null || source.getCurrentBalance() == null
				|| source.getAvailableBalance().compareTo(amount) < 0
				|| source.getCurrentBalance().compareTo(amount) < 0) {
			throw new TransferException("Insufficient account balance for this transfer.");
		}
	}

	private void requireDestinationAccount(Customer customer, BankAccount source, BankAccount destination) {
		if (destination == null || Objects.equals(source.getId(), destination.getId())) {
			throw new TransferException("The internal destination account is not available.");
		}
		if (destination.getCustomer() == null || Objects.equals(customer.getId(), destination.getCustomer().getId())) {
			throw new TransferException("Use another customer's DigiBank account as the recipient.");
		}
		if (destination.getAccountStatus() != AccountStatus.ACTIVE || destination.getCurrencyCode() != CurrencyCode.LKR) {
			throw new TransferException("The internal destination account must be an active LKR account.");
		}
		if (destination.getCustomer().getStatus() != CustomerStatus.ACTIVE
				|| destination.getCustomer().getUser() == null || !destination.getCustomer().getUser().isEnabled()) {
			throw new TransferException("The internal destination customer is not active.");
		}
	}

	private boolean isEligibleDirectDestination(Customer customer, BankAccount account) {
		return account.getCustomer() != null
				&& !Objects.equals(customer.getId(), account.getCustomer().getId())
				&& account.getAccountStatus() == AccountStatus.ACTIVE
				&& account.getCurrencyCode() == CurrencyCode.LKR
				&& account.getCustomer().getStatus() == CustomerStatus.ACTIVE
				&& account.getCustomer().getUser() != null
				&& account.getCustomer().getUser().isEnabled();
	}

	private String accountHolderName(BankAccount account) {
		if (account == null || account.getCustomer() == null) {
			throw new TransferException("The internal destination account is not available.");
		}
		return (account.getCustomer().getFirstName() + " " + account.getCustomer().getLastName()).trim();
	}

	private String normalizeInternalAccountNumber(String value) {
		String normalized = trim(value);
		if (normalized != null) {
			normalized = normalized.replace(" ", "").replace("-", "");
		}
		if (normalized == null || !normalized.matches("^\\d{" + DIGIBANK_ACCOUNT_NUMBER_LENGTH + "}$")) {
			throw new TransferException("Enter a valid 12-digit DigiBank account number.");
		}
		return normalized;
	}

	private void audit(String actorUsername, FundTransfer transfer) {
		String reason = String.format(Locale.ROOT, "LKR %s sent to %s (%s).", transfer.getAmount().toPlainString(),
				transfer.getBeneficiaryNameSnapshot(), transfer.getDestinationAccountMasked());
		auditLogRepository.save(new AuditLog(requireActor(actorUsername), "FUND_TRANSFER_COMPLETED", "FUND_TRANSFER",
				transfer.getReferenceNumber(), TransferStatus.PENDING.name(), TransferStatus.COMPLETED.name(), reason,
				LocalDateTime.now()));
	}

	private TransferListView summary(FundTransfer transfer) {
		return new TransferListView(transfer.getReferenceNumber(), transfer.getBeneficiaryNameSnapshot(),
				transfer.getDestinationBankSnapshot(), transfer.getDestinationAccountMasked(), transfer.getAmount(),
				transfer.getCurrencyCode(), transfer.getTransferType(), transfer.getStatus(), transfer.getCreatedAt());
	}

	private TransferDetailsView details(FundTransfer transfer) {
		return new TransferDetailsView(transfer.getReferenceNumber(),
				dataMasker.maskAccountNumber(transfer.getSourceAccount().getAccountNumber()),
				transfer.getBeneficiaryNameSnapshot(), transfer.getDestinationBankSnapshot(),
				transfer.getDestinationAccountMasked(), transfer.getAmount(), transfer.getCurrencyCode(),
				transfer.getTransferType(), transfer.getStatus(), transfer.getDescription(),
				transfer.getSourceBalanceAfter(), transfer.getCreatedAt(), transfer.getCompletedAt());
	}

	private String referenceNumber() {
		return "TRF" + UUID.randomUUID().toString().replace("-", "").substring(0, 20).toUpperCase(Locale.ROOT);
	}

	private String requireActor(String actorUsername) {
		String actor = trim(actorUsername);
		return actor == null ? "UNKNOWN" : actor;
	}

	private String trimToLength(String value, int maxLength) {
		String trimmed = trim(value);
		return trimmed == null || trimmed.length() <= maxLength ? trimmed : trimmed.substring(0, maxLength);
	}

	private String trim(String value) {
		if (value == null) {
			return null;
		}
		String trimmed = value.trim();
		return trimmed.isEmpty() ? null : trimmed;
	}
}
