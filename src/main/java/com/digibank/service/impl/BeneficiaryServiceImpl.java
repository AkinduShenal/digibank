package com.digibank.service.impl;

import com.digibank.dto.beneficiary.BeneficiaryCreateRequest;
import com.digibank.dto.beneficiary.BeneficiaryDetailsView;
import com.digibank.dto.beneficiary.BeneficiaryListView;
import com.digibank.dto.beneficiary.BeneficiaryReviewView;
import com.digibank.dto.beneficiary.BeneficiarySearchCriteria;
import com.digibank.dto.beneficiary.BeneficiaryUpdateRequest;
import com.digibank.entity.AuditLog;
import com.digibank.entity.BankAccount;
import com.digibank.entity.Beneficiary;
import com.digibank.entity.Customer;
import com.digibank.enums.AccountStatus;
import com.digibank.enums.AccountType;
import com.digibank.enums.BeneficiaryAccountType;
import com.digibank.enums.BeneficiaryStatus;
import com.digibank.enums.BeneficiaryType;
import com.digibank.enums.BeneficiaryVerificationStatus;
import com.digibank.exception.BeneficiaryNotFoundException;
import com.digibank.exception.BeneficiaryVersionConflictException;
import com.digibank.exception.CustomerProfileNotFoundException;
import com.digibank.exception.DuplicateBeneficiaryException;
import com.digibank.exception.InvalidBeneficiaryException;
import com.digibank.exception.InvalidBeneficiaryStateException;
import com.digibank.mapper.BeneficiaryMapper;
import com.digibank.repository.AuditLogRepository;
import com.digibank.repository.BankAccountRepository;
import com.digibank.repository.BeneficiaryRepository;
import com.digibank.repository.CustomerRepository;
import com.digibank.service.BeneficiaryService;
import com.digibank.util.AccountNumberNormalizer;
import com.digibank.util.BankCodeNormalizer;
import com.digibank.util.BeneficiaryConstants;
import com.digibank.util.SensitiveDataMasker;
import jakarta.persistence.criteria.Predicate;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

@Service
public class BeneficiaryServiceImpl implements BeneficiaryService {

	private static final String TARGET_BENEFICIARY = "BENEFICIARY";
	private static final String DUPLICATE_CONSTRAINT = "uk_beneficiaries_customer_active_account";

	private final BeneficiaryRepository beneficiaryRepository;
	private final CustomerRepository customerRepository;
	private final BankAccountRepository bankAccountRepository;
	private final AuditLogRepository auditLogRepository;
	private final AccountNumberNormalizer accountNumberNormalizer;
	private final BankCodeNormalizer bankCodeNormalizer;
	private final SensitiveDataMasker sensitiveDataMasker;
	private final BeneficiaryMapper beneficiaryMapper;

	public BeneficiaryServiceImpl(BeneficiaryRepository beneficiaryRepository, CustomerRepository customerRepository,
			BankAccountRepository bankAccountRepository, AuditLogRepository auditLogRepository,
			AccountNumberNormalizer accountNumberNormalizer, BankCodeNormalizer bankCodeNormalizer,
			SensitiveDataMasker sensitiveDataMasker, BeneficiaryMapper beneficiaryMapper) {
		this.beneficiaryRepository = beneficiaryRepository;
		this.customerRepository = customerRepository;
		this.bankAccountRepository = bankAccountRepository;
		this.auditLogRepository = auditLogRepository;
		this.accountNumberNormalizer = accountNumberNormalizer;
		this.bankCodeNormalizer = bankCodeNormalizer;
		this.sensitiveDataMasker = sensitiveDataMasker;
		this.beneficiaryMapper = beneficiaryMapper;
	}

	@Override
	@Transactional
	public BeneficiaryDetailsView createBeneficiary(Long authenticatedUserId, BeneficiaryCreateRequest request) {
		if (request == null) {
			throw new InvalidBeneficiaryException("Beneficiary details are required.");
		}
		Customer customer = resolveCustomer(authenticatedUserId);
		String normalizedAccountNumber = accountNumberNormalizer.normalize(request.getAccountNumber());
		BeneficiaryType beneficiaryType = requireBeneficiaryType(request.getBeneficiaryType());
		Beneficiary beneficiary = beneficiaryType == BeneficiaryType.INTERNAL
				? internalBeneficiary(customer, normalizedAccountNumber)
				: externalBeneficiary(customer, request, normalizedAccountNumber);
		beneficiary.setTransferLimit(requireTransferLimit(request.getTransferLimit()));
		ensureNoDuplicate(customer.getId(), beneficiary.getBankCode(), beneficiary.getNormalizedAccountNumber());
		return saveWithDuplicateTranslation(() -> {
			Beneficiary saved = beneficiaryRepository.save(beneficiary);
			audit(authenticatedUserId, "BENEFICIARY_CREATED", saved, null, saved.getStatus(), "Created "
					+ sensitiveDataMasker.maskAccountNumber(saved.getAccountNumber()));
			return beneficiaryMapper.toDetailsView(saved);
		});
	}

	@Override
	@Transactional(readOnly = true)
	public Page<BeneficiaryListView> searchBeneficiaries(Long authenticatedUserId,
			BeneficiarySearchCriteria criteria) {
		Customer customer = resolveCustomer(authenticatedUserId);
		BeneficiarySearchCriteria safeCriteria = criteria == null ? new BeneficiarySearchCriteria() : criteria;
		Pageable pageable = PageRequest.of(safeCriteria.safePage(), safeCriteria.safeSize(), sort(safeCriteria));
		return beneficiaryRepository.findAll(specification(customer.getId(), safeCriteria), pageable)
				.map(beneficiaryMapper::toListView);
	}

	@Override
	@Transactional(readOnly = true)
	public BeneficiaryDetailsView getBeneficiary(Long authenticatedUserId, Long beneficiaryId) {
		Customer customer = resolveCustomer(authenticatedUserId);
		return beneficiaryMapper.toDetailsView(activeBeneficiary(customer.getId(), beneficiaryId));
	}

	@Override
	@Transactional
	public BeneficiaryDetailsView updateBeneficiary(Long authenticatedUserId, Long beneficiaryId,
			BeneficiaryUpdateRequest request) {
		if (request == null) {
			throw new InvalidBeneficiaryException("Beneficiary details are required.");
		}
		Customer customer = resolveCustomer(authenticatedUserId);
		Beneficiary beneficiary = activeBeneficiary(customer.getId(), beneficiaryId);
		requireVersion(beneficiary, request.getVersion());
		requireNotDeleted(beneficiary);
		if (beneficiary.getBeneficiaryType() == BeneficiaryType.INTERNAL) {
			updateInternalBeneficiary(beneficiary, request);
		}
		else {
			updateExternalBeneficiary(customer, beneficiary, request);
		}
		beneficiary.setTransferLimit(requireTransferLimit(request.getTransferLimit()));
		Beneficiary saved = beneficiaryRepository.save(beneficiary);
		audit(authenticatedUserId, "BENEFICIARY_UPDATED", saved, saved.getStatus(), saved.getStatus(),
				"Updated " + sensitiveDataMasker.maskAccountNumber(saved.getAccountNumber()));
		return beneficiaryMapper.toDetailsView(saved);
	}

	@Override
	@Transactional
	public BeneficiaryDetailsView deactivateBeneficiary(Long authenticatedUserId, Long beneficiaryId) {
		Customer customer = resolveCustomer(authenticatedUserId);
		Beneficiary beneficiary = activeBeneficiary(customer.getId(), beneficiaryId);
		if (beneficiary.getStatus() != BeneficiaryStatus.ACTIVE) {
			throw new InvalidBeneficiaryStateException("Only active beneficiaries can be deactivated.");
		}
		BeneficiaryStatus previousStatus = beneficiary.getStatus();
		beneficiary.setStatus(BeneficiaryStatus.INACTIVE);
		beneficiary.setFavourite(false);
		Beneficiary saved = beneficiaryRepository.save(beneficiary);
		audit(authenticatedUserId, "BENEFICIARY_DEACTIVATED", saved, previousStatus, saved.getStatus(),
				"Deactivated " + sensitiveDataMasker.maskAccountNumber(saved.getAccountNumber()));
		return beneficiaryMapper.toDetailsView(saved);
	}

	@Override
	@Transactional
	public BeneficiaryDetailsView reactivateBeneficiary(Long authenticatedUserId, Long beneficiaryId) {
		Customer customer = resolveCustomer(authenticatedUserId);
		Beneficiary beneficiary = activeBeneficiary(customer.getId(), beneficiaryId);
		if (beneficiary.getStatus() != BeneficiaryStatus.INACTIVE) {
			throw new InvalidBeneficiaryStateException("Only inactive beneficiaries can be reactivated.");
		}
		if (beneficiary.getBeneficiaryType() == BeneficiaryType.INTERNAL) {
			BankAccount account = internalAccount(beneficiary.getNormalizedAccountNumber());
			requireEligibleInternalAccount(customer, account);
		}
		ensureNoDuplicateExcludingCurrent(customer.getId(), beneficiary.getBankCode(),
				beneficiary.getNormalizedAccountNumber(), beneficiary.getId());
		BeneficiaryStatus previousStatus = beneficiary.getStatus();
		beneficiary.setStatus(BeneficiaryStatus.ACTIVE);
		beneficiary.setFavourite(false);
		return saveWithDuplicateTranslation(() -> {
			Beneficiary saved = beneficiaryRepository.save(beneficiary);
			audit(authenticatedUserId, "BENEFICIARY_REACTIVATED", saved, previousStatus, saved.getStatus(),
					"Reactivated " + sensitiveDataMasker.maskAccountNumber(saved.getAccountNumber()));
			return beneficiaryMapper.toDetailsView(saved);
		});
	}

	@Override
	@Transactional
	public void deleteBeneficiary(Long authenticatedUserId, Long beneficiaryId) {
		Customer customer = resolveCustomer(authenticatedUserId);
		Beneficiary beneficiary = activeBeneficiary(customer.getId(), beneficiaryId);
		if (beneficiary.getStatus() == BeneficiaryStatus.DELETED) {
			throw new BeneficiaryNotFoundException();
		}
		BeneficiaryStatus previousStatus = beneficiary.getStatus();
		beneficiary.setStatus(BeneficiaryStatus.DELETED);
		beneficiary.setFavourite(false);
		Beneficiary saved = beneficiaryRepository.save(beneficiary);
		audit(authenticatedUserId, "BENEFICIARY_DELETED", saved, previousStatus, saved.getStatus(),
				"Deleted " + sensitiveDataMasker.maskAccountNumber(saved.getAccountNumber()));
	}

	@Override
	@Transactional
	public BeneficiaryDetailsView markFavourite(Long authenticatedUserId, Long beneficiaryId) {
		Customer customer = resolveCustomer(authenticatedUserId);
		Beneficiary beneficiary = activeBeneficiary(customer.getId(), beneficiaryId);
		if (beneficiary.getStatus() != BeneficiaryStatus.ACTIVE) {
			throw new InvalidBeneficiaryStateException("Only active beneficiaries can be marked as favourite.");
		}
		if (beneficiary.isFavourite()) {
			return beneficiaryMapper.toDetailsView(beneficiary);
		}
		beneficiary.setFavourite(true);
		Beneficiary saved = beneficiaryRepository.save(beneficiary);
		audit(authenticatedUserId, "BENEFICIARY_FAVOURITED", saved, saved.getStatus(), saved.getStatus(),
				"Favourited " + sensitiveDataMasker.maskAccountNumber(saved.getAccountNumber()));
		return beneficiaryMapper.toDetailsView(saved);
	}

	@Override
	@Transactional
	public BeneficiaryDetailsView removeFavourite(Long authenticatedUserId, Long beneficiaryId) {
		Customer customer = resolveCustomer(authenticatedUserId);
		Beneficiary beneficiary = activeBeneficiary(customer.getId(), beneficiaryId);
		if (!beneficiary.isFavourite()) {
			return beneficiaryMapper.toDetailsView(beneficiary);
		}
		beneficiary.setFavourite(false);
		Beneficiary saved = beneficiaryRepository.save(beneficiary);
		audit(authenticatedUserId, "BENEFICIARY_UNFAVOURITED", saved, saved.getStatus(), saved.getStatus(),
				"Unfavourited " + sensitiveDataMasker.maskAccountNumber(saved.getAccountNumber()));
		return beneficiaryMapper.toDetailsView(saved);
	}

	@Override
	@Transactional(readOnly = true)
	public List<BeneficiaryReviewView> getBeneficiariesForReview(
			BeneficiaryVerificationStatus verificationStatus) {
		BeneficiaryVerificationStatus safeStatus = verificationStatus == null
				? BeneficiaryVerificationStatus.PENDING
				: verificationStatus;
		return beneficiaryRepository.findAllByVerificationStatusAndStatusNotOrderByCreatedAtAsc(safeStatus,
				BeneficiaryStatus.DELETED).stream().map(beneficiaryMapper::toReviewView).toList();
	}

	@Override
	@Transactional
	public BeneficiaryDetailsView verifyBeneficiary(String actorUsername, Long beneficiaryId, String note) {
		Beneficiary beneficiary = beneficiaryForReview(beneficiaryId);
		requirePendingVerification(beneficiary);
		String actor = requireActor(actorUsername);
		String safeNote = optionalReviewNote(note);
		BeneficiaryVerificationStatus previousStatus = beneficiary.getVerificationStatus();
		beneficiary.setVerificationStatus(BeneficiaryVerificationStatus.VERIFIED);
		beneficiary.setReviewedBy(actor);
		beneficiary.setReviewedAt(LocalDateTime.now());
		beneficiary.setVerificationNote(safeNote);
		Beneficiary saved = beneficiaryRepository.save(beneficiary);
		auditReview(actor, "BENEFICIARY_VERIFIED", saved, previousStatus, saved.getVerificationStatus(),
				"Beneficiary verification approved.");
		return beneficiaryMapper.toDetailsView(saved);
	}

	@Override
	@Transactional
	public BeneficiaryDetailsView rejectBeneficiary(String actorUsername, Long beneficiaryId, String reason) {
		Beneficiary beneficiary = beneficiaryForReview(beneficiaryId);
		requirePendingVerification(beneficiary);
		String actor = requireActor(actorUsername);
		String safeReason = requiredReviewNote(reason);
		BeneficiaryVerificationStatus previousStatus = beneficiary.getVerificationStatus();
		beneficiary.setVerificationStatus(BeneficiaryVerificationStatus.REJECTED);
		beneficiary.setReviewedBy(actor);
		beneficiary.setReviewedAt(LocalDateTime.now());
		beneficiary.setVerificationNote(safeReason);
		beneficiary.setFavourite(false);
		Beneficiary saved = beneficiaryRepository.save(beneficiary);
		auditReview(actor, "BENEFICIARY_REJECTED", saved, previousStatus, saved.getVerificationStatus(),
				"Beneficiary verification rejected.");
		return beneficiaryMapper.toDetailsView(saved);
	}

	private Customer resolveCustomer(Long authenticatedUserId) {
		if (authenticatedUserId == null) {
			throw new CustomerProfileNotFoundException();
		}
		return customerRepository.findByUserId(authenticatedUserId).orElseThrow(CustomerProfileNotFoundException::new);
	}

	private Beneficiary internalBeneficiary(Customer customer, String normalizedAccountNumber) {
		BankAccount account = internalAccount(normalizedAccountNumber);
		requireEligibleInternalAccount(customer, account);
		Customer targetCustomer = account.getCustomer();
		Beneficiary beneficiary = new Beneficiary(customer, targetCustomer.getFullName(),
				BeneficiaryConstants.DIGIBANK_BANK_NAME, BeneficiaryConstants.DIGIBANK_BANK_CODE,
				account.getAccountNumber(), normalizedAccountNumber, beneficiaryAccountType(account.getAccountType()),
				BeneficiaryType.INTERNAL);
		beneficiary.setBranchCode(trim(account.getBranchCode()));
		beneficiary.setReviewedBy("SYSTEM");
		beneficiary.setReviewedAt(LocalDateTime.now());
		beneficiary.setVerificationNote("Verified from an existing DigiBank account.");
		return beneficiary;
	}

	private Beneficiary externalBeneficiary(Customer customer, BeneficiaryCreateRequest request,
			String normalizedAccountNumber) {
		String bankName = requireText(request.getBankName(), "Bank name is required for an external beneficiary.");
		String bankCode = bankCodeNormalizer.normalize(request.getBankCode());
		Beneficiary beneficiary = new Beneficiary(customer, requireText(request.getBeneficiaryName(), "Beneficiary name is required."),
				bankName, bankCode, normalizedAccountNumber, normalizedAccountNumber,
				requireAccountType(request.getAccountType()), BeneficiaryType.EXTERNAL);
		beneficiary.setNickname(trim(request.getNickname()));
		beneficiary.setBranchName(trim(request.getBranchName()));
		beneficiary.setBranchCode(normalizeOptionalBankCode(request.getBranchCode()));
		return beneficiary;
	}

	private BankAccount internalAccount(String normalizedAccountNumber) {
		return bankAccountRepository.findByAccountNumber(normalizedAccountNumber)
				.orElseThrow(() -> new InvalidBeneficiaryException("Internal beneficiary account is not eligible."));
	}

	private void requireEligibleInternalAccount(Customer currentCustomer, BankAccount account) {
		if (account.getAccountStatus() != AccountStatus.ACTIVE || account.getCustomer() == null) {
			throw new InvalidBeneficiaryException("Internal beneficiary account is not eligible.");
		}
		if (Objects.equals(account.getCustomer().getId(), currentCustomer.getId())) {
			throw new InvalidBeneficiaryException("You cannot add your own account as a beneficiary.");
		}
	}

	private void updateInternalBeneficiary(Beneficiary beneficiary, BeneficiaryUpdateRequest request) {
		rejectIfChanged(request.getBeneficiaryName(), beneficiary.getBeneficiaryName(), "Internal beneficiary name cannot be changed.");
		rejectIfChanged(request.getBankName(), beneficiary.getBankName(), "Internal bank name cannot be changed.");
		rejectIfChanged(bankCodeOrNull(request.getBankCode()), beneficiary.getBankCode(), "Internal bank code cannot be changed.");
		rejectIfChanged(trim(request.getBranchName()), beneficiary.getBranchName(), "Internal branch name cannot be changed.");
		rejectIfChanged(trim(request.getBranchCode()), beneficiary.getBranchCode(), "Internal branch code cannot be changed.");
		if (request.getAccountType() != null && request.getAccountType() != beneficiary.getAccountType()) {
			throw new InvalidBeneficiaryException("Internal account type cannot be changed.");
		}
		beneficiary.setNickname(trim(request.getNickname()));
	}

	private void updateExternalBeneficiary(Customer customer, Beneficiary beneficiary, BeneficiaryUpdateRequest request) {
		boolean verificationDataChanged = externalVerificationDataChanged(beneficiary, request);
		String bankCode = bankCodeNormalizer.normalize(request.getBankCode());
		if (!bankCode.equals(beneficiary.getBankCode())) {
			ensureNoDuplicateExcludingCurrent(customer.getId(), bankCode, beneficiary.getNormalizedAccountNumber(),
					beneficiary.getId());
		}
		beneficiary.setBeneficiaryName(requireText(request.getBeneficiaryName(), "Beneficiary name is required."));
		beneficiary.setNickname(trim(request.getNickname()));
		beneficiary.setBankName(requireText(request.getBankName(), "Bank name is required."));
		beneficiary.setBankCode(bankCode);
		beneficiary.setBranchName(trim(request.getBranchName()));
		beneficiary.setBranchCode(normalizeOptionalBankCode(request.getBranchCode()));
		beneficiary.setAccountType(requireAccountType(request.getAccountType()));
		if (verificationDataChanged) {
			beneficiary.setVerificationStatus(BeneficiaryVerificationStatus.PENDING);
			beneficiary.setReviewedBy(null);
			beneficiary.setReviewedAt(null);
			beneficiary.setVerificationNote(null);
		}
	}

	private boolean externalVerificationDataChanged(Beneficiary beneficiary, BeneficiaryUpdateRequest request) {
		return !Objects.equals(trim(request.getBeneficiaryName()), trim(beneficiary.getBeneficiaryName()))
				|| !Objects.equals(trim(request.getBankName()), trim(beneficiary.getBankName()))
				|| !Objects.equals(bankCodeOrNull(request.getBankCode()), beneficiary.getBankCode())
				|| !Objects.equals(trim(request.getBranchName()), trim(beneficiary.getBranchName()))
				|| !Objects.equals(bankCodeOrNull(request.getBranchCode()), beneficiary.getBranchCode())
				|| request.getAccountType() != beneficiary.getAccountType();
	}

	private void ensureNoDuplicate(Long customerId, String bankCode, String normalizedAccountNumber) {
		if (beneficiaryRepository.existsByCustomerIdAndBankCodeAndNormalizedAccountNumberAndStatusNot(customerId,
				bankCode, normalizedAccountNumber, BeneficiaryStatus.DELETED)) {
			throw new DuplicateBeneficiaryException();
		}
	}

	private void ensureNoDuplicateExcludingCurrent(Long customerId, String bankCode, String normalizedAccountNumber,
			Long beneficiaryId) {
		if (beneficiaryRepository.existsByCustomerIdAndBankCodeAndNormalizedAccountNumberAndStatusNotAndIdNot(customerId,
				bankCode, normalizedAccountNumber, BeneficiaryStatus.DELETED, beneficiaryId)) {
			throw new DuplicateBeneficiaryException();
		}
	}

	private Beneficiary activeBeneficiary(Long customerId, Long beneficiaryId) {
		if (beneficiaryId == null) {
			throw new BeneficiaryNotFoundException();
		}
		return beneficiaryRepository.findByIdAndCustomerIdAndStatusNot(beneficiaryId, customerId,
				BeneficiaryStatus.DELETED).orElseThrow(BeneficiaryNotFoundException::new);
	}

	private Beneficiary beneficiaryForReview(Long beneficiaryId) {
		if (beneficiaryId == null) {
			throw new BeneficiaryNotFoundException();
		}
		Beneficiary beneficiary = beneficiaryRepository.findById(beneficiaryId)
				.orElseThrow(BeneficiaryNotFoundException::new);
		requireNotDeleted(beneficiary);
		return beneficiary;
	}

	private void requirePendingVerification(Beneficiary beneficiary) {
		if (beneficiary.getVerificationStatus() != BeneficiaryVerificationStatus.PENDING) {
			throw new InvalidBeneficiaryStateException("Only pending beneficiaries can be reviewed.");
		}
	}

	private void requireVersion(Beneficiary beneficiary, Long version) {
		if (version == null || !Objects.equals(beneficiary.getVersion(), version)) {
			throw new BeneficiaryVersionConflictException();
		}
	}

	private void requireNotDeleted(Beneficiary beneficiary) {
		if (beneficiary.getStatus() == BeneficiaryStatus.DELETED) {
			throw new BeneficiaryNotFoundException();
		}
	}

	private Specification<Beneficiary> specification(Long customerId, BeneficiarySearchCriteria criteria) {
		return (root, query, builder) -> {
			List<Predicate> predicates = new ArrayList<>();
			predicates.add(builder.equal(root.get("customer").get("id"), customerId));
			predicates.add(builder.notEqual(root.get("status"), BeneficiaryStatus.DELETED));
			if (criteria.getType() != null) {
				predicates.add(builder.equal(root.get("beneficiaryType"), criteria.getType()));
			}
			if (criteria.getStatus() != null && criteria.getStatus() != BeneficiaryStatus.DELETED) {
				predicates.add(builder.equal(root.get("status"), criteria.getStatus()));
			}
			if (criteria.getFavourite() != null) {
				predicates.add(builder.equal(root.get("favourite"), criteria.getFavourite()));
			}
			String search = trim(criteria.getQuery());
			if (search != null) {
				String like = "%" + search.toLowerCase(Locale.ROOT) + "%";
				predicates.add(builder.or(
						builder.like(builder.lower(root.get("beneficiaryName")), like),
						builder.like(builder.lower(root.get("nickname")), like),
						builder.like(builder.lower(root.get("bankName")), like),
						builder.like(builder.lower(root.get("bankCode")), like)
				));
			}
			return builder.and(predicates.toArray(Predicate[]::new));
		};
	}

	private Sort sort(BeneficiarySearchCriteria criteria) {
		String property = sortProperty(criteria.safeSort());
		Sort.Direction direction = "desc".equals(criteria.safeDirection()) ? Sort.Direction.DESC : Sort.Direction.ASC;
		return Sort.by(direction, property);
	}

	private String sortProperty(String requestedSort) {
		return switch (requestedSort) {
			case "bankName" -> "bankName";
			case "beneficiaryType" -> "beneficiaryType";
			case "status" -> "status";
			case "favourite" -> "favourite";
			case "updatedAt" -> "updatedAt";
			case "createdAt" -> "createdAt";
			default -> "beneficiaryName";
		};
	}

	private BeneficiaryType requireBeneficiaryType(BeneficiaryType beneficiaryType) {
		if (beneficiaryType == null) {
			throw new InvalidBeneficiaryException("Beneficiary type is required.");
		}
		return beneficiaryType;
	}

	private BeneficiaryAccountType requireAccountType(BeneficiaryAccountType accountType) {
		if (accountType == null) {
			throw new InvalidBeneficiaryException("Account type is required.");
		}
		return accountType;
	}

	private BigDecimal requireTransferLimit(BigDecimal transferLimit) {
		if (transferLimit == null || transferLimit.compareTo(BeneficiaryConstants.MIN_TRANSFER_LIMIT) < 0
				|| transferLimit.compareTo(BeneficiaryConstants.MAX_TRANSFER_LIMIT) > 0
				|| transferLimit.scale() > 2) {
			throw new InvalidBeneficiaryException("Transfer limit must be between LKR 0.01 and LKR 1,000,000.00 with at most two decimal places.");
		}
		return transferLimit;
	}

	private String requireActor(String actorUsername) {
		String actor = trim(actorUsername);
		if (actor == null) {
			throw new InvalidBeneficiaryException("Authenticated staff member is required.");
		}
		return actor;
	}

	private String optionalReviewNote(String note) {
		String value = trim(note);
		if (value != null && value.length() > 255) {
			throw new InvalidBeneficiaryException("Verification note must not exceed 255 characters.");
		}
		return value;
	}

	private String requiredReviewNote(String note) {
		String value = optionalReviewNote(note);
		if (value == null) {
			throw new InvalidBeneficiaryException("A rejection reason is required.");
		}
		return value;
	}

	private BeneficiaryAccountType beneficiaryAccountType(AccountType accountType) {
		if (accountType == AccountType.CURRENT) {
			return BeneficiaryAccountType.CURRENT;
		}
		return BeneficiaryAccountType.SAVINGS;
	}

	private String normalizeOptionalBankCode(String value) {
		String trimmed = trim(value);
		return trimmed == null ? null : bankCodeNormalizer.normalize(trimmed);
	}

	private String bankCodeOrNull(String value) {
		String trimmed = trim(value);
		return trimmed == null ? null : bankCodeNormalizer.normalize(trimmed);
	}

	private String requireText(String value, String message) {
		String trimmed = trim(value);
		if (trimmed == null) {
			throw new InvalidBeneficiaryException(message);
		}
		return trimmed;
	}

	private void rejectIfChanged(String requested, String current, String message) {
		String trimmedRequested = trim(requested);
		String trimmedCurrent = trim(current);
		if (trimmedRequested != null && !Objects.equals(trimmedRequested, trimmedCurrent)) {
			throw new InvalidBeneficiaryException(message);
		}
	}

	private void audit(Long authenticatedUserId, String action, Beneficiary beneficiary,
			BeneficiaryStatus previousStatus, BeneficiaryStatus newStatus, String reason) {
		AuditLog auditLog = new AuditLog("USER:" + authenticatedUserId, action, TARGET_BENEFICIARY,
				"BENEFICIARY:" + beneficiary.getId(), statusName(previousStatus), statusName(newStatus),
				reason, LocalDateTime.now());
		auditLogRepository.save(auditLog);
	}

	private void auditReview(String actorUsername, String action, Beneficiary beneficiary,
			BeneficiaryVerificationStatus previousStatus, BeneficiaryVerificationStatus newStatus, String reason) {
		AuditLog auditLog = new AuditLog(requireActor(actorUsername), action, TARGET_BENEFICIARY,
				"BENEFICIARY:" + beneficiary.getId(), previousStatus.name(), newStatus.name(), reason,
				LocalDateTime.now());
		auditLogRepository.save(auditLog);
	}

	private String statusName(BeneficiaryStatus status) {
		return status == null ? null : status.name();
	}

	private <T> T saveWithDuplicateTranslation(SupplierWithResult<T> supplier) {
		try {
			return supplier.get();
		}
		catch (DataIntegrityViolationException ex) {
			if (isDuplicateConstraint(ex)) {
				throw new DuplicateBeneficiaryException();
			}
			throw ex;
		}
	}

	private boolean isDuplicateConstraint(Throwable throwable) {
		Throwable current = throwable;
		while (current != null) {
			if (current.getMessage() != null && current.getMessage().contains(DUPLICATE_CONSTRAINT)) {
				return true;
			}
			current = current.getCause();
		}
		return false;
	}

	private String trim(String value) {
		if (value == null) {
			return null;
		}
		String trimmed = value.trim();
		return trimmed.isEmpty() ? null : trimmed;
	}

	@FunctionalInterface
	private interface SupplierWithResult<T> {

		T get();
	}
}
