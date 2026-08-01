package com.digibank.service;

import com.digibank.dto.beneficiary.BeneficiaryCreateRequest;
import com.digibank.dto.beneficiary.BeneficiaryDetailsView;
import com.digibank.dto.beneficiary.BeneficiaryListView;
import com.digibank.dto.beneficiary.BeneficiarySearchCriteria;
import com.digibank.dto.beneficiary.BeneficiaryUpdateRequest;
import com.digibank.entity.AuditLog;
import com.digibank.entity.BankAccount;
import com.digibank.entity.Beneficiary;
import com.digibank.entity.Customer;
import com.digibank.entity.User;
import com.digibank.enums.AccountStatus;
import com.digibank.enums.AccountType;
import com.digibank.enums.BeneficiaryAccountType;
import com.digibank.enums.BeneficiaryStatus;
import com.digibank.enums.BeneficiaryType;
import com.digibank.enums.BeneficiaryVerificationStatus;
import com.digibank.enums.Gender;
import com.digibank.enums.IdentityType;
import com.digibank.enums.Role;
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
import com.digibank.service.impl.BeneficiaryServiceImpl;
import com.digibank.util.AccountNumberNormalizer;
import com.digibank.util.BankCodeNormalizer;
import com.digibank.util.SensitiveDataMasker;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@SuppressWarnings("unchecked")
class BeneficiaryServiceImplTest {

	private static final Long USER_ID = 10L;
	private static final Long CUSTOMER_ID = 20L;
	private static final Long OTHER_CUSTOMER_ID = 30L;
	private static final Long BENEFICIARY_ID = 40L;

	@Mock
	private BeneficiaryRepository beneficiaryRepository;

	@Mock
	private CustomerRepository customerRepository;

	@Mock
	private BankAccountRepository bankAccountRepository;

	@Mock
	private AuditLogRepository auditLogRepository;

	private BeneficiaryServiceImpl service;
	private Customer customer;
	private Customer otherCustomer;

	@BeforeEach
	void setUp() {
		SensitiveDataMasker masker = new SensitiveDataMasker();
		service = new BeneficiaryServiceImpl(beneficiaryRepository, customerRepository, bankAccountRepository,
				auditLogRepository, new AccountNumberNormalizer(), new BankCodeNormalizer(), masker,
				new BeneficiaryMapper(masker));
		customer = customer(CUSTOMER_ID, USER_ID, "Lakshitha", "Dilshan");
		otherCustomer = customer(OTHER_CUSTOMER_ID, 99L, "Kasun", "Perera");
		when(customerRepository.findByUserId(USER_ID)).thenReturn(Optional.of(customer));
		when(auditLogRepository.save(any(AuditLog.class))).thenAnswer(invocation -> invocation.getArgument(0));
	}

	@Test
	void createsValidExternalBeneficiaryWithNormalizedValues() {
		whenNoDuplicate();
		whenSavedAssignsId();

		service.createBeneficiary(USER_ID, externalCreateRequest());

		Beneficiary saved = capturedSavedBeneficiary();
		assertEquals("EXB01", saved.getBankCode());
		assertEquals("123456789012", saved.getNormalizedAccountNumber());
		assertEquals(BeneficiaryStatus.ACTIVE, saved.getStatus());
		assertEquals(BeneficiaryVerificationStatus.PENDING, saved.getVerificationStatus());
		assertEquals(new BigDecimal("100000.00"), saved.getTransferLimit());
		assertFalse(saved.isFavourite());
	}

	@Test
	void nullCreateRequestIsRejected() {
		assertThrows(InvalidBeneficiaryException.class, () -> service.createBeneficiary(USER_ID, null));
	}

	@Test
	void missingCustomerProfileIsRejected() {
		when(customerRepository.findByUserId(USER_ID)).thenReturn(Optional.empty());

		assertThrows(CustomerProfileNotFoundException.class,
				() -> service.createBeneficiary(USER_ID, externalCreateRequest()));
	}

	@Test
	void missingBeneficiaryTypeIsRejected() {
		BeneficiaryCreateRequest request = externalCreateRequest();
		request.setBeneficiaryType(null);

		assertThrows(InvalidBeneficiaryException.class, () -> service.createBeneficiary(USER_ID, request));
	}

	@Test
	void externalMissingBeneficiaryNameIsRejected() {
		BeneficiaryCreateRequest request = externalCreateRequest();
		request.setBeneficiaryName(" ");

		assertThrows(InvalidBeneficiaryException.class, () -> service.createBeneficiary(USER_ID, request));
	}

	@Test
	void externalMissingBankNameIsRejected() {
		BeneficiaryCreateRequest request = externalCreateRequest();
		request.setBankName(" ");

		assertThrows(InvalidBeneficiaryException.class, () -> service.createBeneficiary(USER_ID, request));
	}

	@Test
	void externalMissingAccountTypeIsRejected() {
		BeneficiaryCreateRequest request = externalCreateRequest();
		request.setAccountType(null);

		assertThrows(InvalidBeneficiaryException.class, () -> service.createBeneficiary(USER_ID, request));
	}

	@Test
	void invalidAccountNumberIsRejectedBeforePersistence() {
		BeneficiaryCreateRequest request = externalCreateRequest();
		request.setAccountNumber("ABC-123");

		assertThrows(InvalidBeneficiaryException.class, () -> service.createBeneficiary(USER_ID, request));
		verify(beneficiaryRepository, never()).save(any(Beneficiary.class));
		verify(auditLogRepository, never()).save(any(AuditLog.class));
	}

	@Test
	void createsValidInternalBeneficiaryFromAuthoritativeAccountDetails() {
		whenNoDuplicate();
		when(bankAccountRepository.findByAccountNumber("123456789012")).thenReturn(Optional.of(internalAccount(AccountStatus.ACTIVE)));
		whenSavedAssignsId();

		service.createBeneficiary(USER_ID, internalCreateRequestWithUntrustedBankDetails());

		Beneficiary saved = capturedSavedBeneficiary();
		assertEquals("Kasun Perera", saved.getBeneficiaryName());
		assertEquals("DigiBank", saved.getBankName());
		assertEquals("DIGIBANK", saved.getBankCode());
		assertEquals(BeneficiaryAccountType.CURRENT, saved.getAccountType());
		assertEquals("COL001", saved.getBranchCode());
		assertEquals(BeneficiaryVerificationStatus.VERIFIED, saved.getVerificationStatus());
		assertEquals("SYSTEM", saved.getReviewedBy());
	}

	@Test
	void transferLimitAboveMaximumIsRejected() {
		BeneficiaryCreateRequest request = externalCreateRequest();
		request.setTransferLimit(new BigDecimal("1000000.01"));

		assertThrows(InvalidBeneficiaryException.class, () -> service.createBeneficiary(USER_ID, request));
		verify(beneficiaryRepository, never()).save(any(Beneficiary.class));
	}

	@Test
	void staffCanVerifyPendingExternalBeneficiary() {
		Beneficiary beneficiary = externalBeneficiary(BeneficiaryStatus.ACTIVE);
		when(beneficiaryRepository.findById(BENEFICIARY_ID)).thenReturn(Optional.of(beneficiary));
		when(beneficiaryRepository.save(any(Beneficiary.class))).thenAnswer(invocation -> invocation.getArgument(0));

		service.verifyBeneficiary("staff", BENEFICIARY_ID, "Account details checked");

		assertEquals(BeneficiaryVerificationStatus.VERIFIED, beneficiary.getVerificationStatus());
		assertEquals("staff", beneficiary.getReviewedBy());
		assertNotNull(beneficiary.getReviewedAt());
		assertEquals("Account details checked", beneficiary.getVerificationNote());
		assertEquals("BENEFICIARY_VERIFIED", capturedAuditLog().getAction());
	}

	@Test
	void rejectionRequiresReason() {
		Beneficiary beneficiary = externalBeneficiary(BeneficiaryStatus.ACTIVE);
		when(beneficiaryRepository.findById(BENEFICIARY_ID)).thenReturn(Optional.of(beneficiary));

		assertThrows(InvalidBeneficiaryException.class,
				() -> service.rejectBeneficiary("staff", BENEFICIARY_ID, " "));

		assertEquals(BeneficiaryVerificationStatus.PENDING, beneficiary.getVerificationStatus());
		verify(beneficiaryRepository, never()).save(any(Beneficiary.class));
	}

	@Test
	void changingVerifiedExternalBankDetailsRequiresReverification() {
		Beneficiary beneficiary = externalBeneficiary(BeneficiaryStatus.ACTIVE);
		beneficiary.setVerificationStatus(BeneficiaryVerificationStatus.VERIFIED);
		beneficiary.setReviewedBy("staff");
		whenActiveBeneficiary(beneficiary);
		whenNoDuplicateExcluding();
		when(beneficiaryRepository.save(any(Beneficiary.class))).thenAnswer(invocation -> invocation.getArgument(0));

		service.updateBeneficiary(USER_ID, BENEFICIARY_ID, externalUpdateRequest("New Bank", "NEW01", 1L));

		assertEquals(BeneficiaryVerificationStatus.PENDING, beneficiary.getVerificationStatus());
		assertNull(beneficiary.getReviewedBy());
	}

	@Test
	void internalAccountDoesNotExistIsRejected() {
		when(bankAccountRepository.findByAccountNumber("123456789012")).thenReturn(Optional.empty());

		assertThrows(InvalidBeneficiaryException.class,
				() -> service.createBeneficiary(USER_ID, internalCreateRequestWithUntrustedBankDetails()));
	}

	@Test
	void internalAccountMustBeActive() {
		when(bankAccountRepository.findByAccountNumber("123456789012"))
				.thenReturn(Optional.of(internalAccount(AccountStatus.FROZEN)));

		assertThrows(InvalidBeneficiaryException.class,
				() -> service.createBeneficiary(USER_ID, internalCreateRequestWithUntrustedBankDetails()));
	}

	@Test
	void selfBeneficiaryIsRejectedByCustomerOwnership() {
		BankAccount ownAccount = new BankAccount(customer, "123456789012", AccountType.SAVINGS, "COL001");
		ownAccount.setAccountStatus(AccountStatus.ACTIVE);
		when(bankAccountRepository.findByAccountNumber("123456789012")).thenReturn(Optional.of(ownAccount));

		assertThrows(InvalidBeneficiaryException.class,
				() -> service.createBeneficiary(USER_ID, internalCreateRequestWithUntrustedBankDetails()));
	}

	@Test
	void duplicateInternalBeneficiaryIsRejected() {
		when(bankAccountRepository.findByAccountNumber("123456789012")).thenReturn(Optional.of(internalAccount(AccountStatus.ACTIVE)));
		when(beneficiaryRepository.existsByCustomerIdAndBankCodeAndNormalizedAccountNumberAndStatusNot(
				CUSTOMER_ID, "DIGIBANK", "123456789012", BeneficiaryStatus.DELETED)).thenReturn(true);

		assertThrows(DuplicateBeneficiaryException.class,
				() -> service.createBeneficiary(USER_ID, internalCreateRequestWithUntrustedBankDetails()));
	}

	@Test
	void duplicateExternalBeneficiaryIsRejected() {
		when(beneficiaryRepository.existsByCustomerIdAndBankCodeAndNormalizedAccountNumberAndStatusNot(
				CUSTOMER_ID, "EXB01", "123456789012", BeneficiaryStatus.DELETED)).thenReturn(true);

		assertThrows(DuplicateBeneficiaryException.class,
				() -> service.createBeneficiary(USER_ID, externalCreateRequest()));
	}

	@Test
	void duplicateExternalBeneficiaryDoesNotPersistOrAudit() {
		when(beneficiaryRepository.existsByCustomerIdAndBankCodeAndNormalizedAccountNumberAndStatusNot(
				CUSTOMER_ID, "EXB01", "123456789012", BeneficiaryStatus.DELETED)).thenReturn(true);

		assertThrows(DuplicateBeneficiaryException.class,
				() -> service.createBeneficiary(USER_ID, externalCreateRequest()));

		verify(beneficiaryRepository, never()).save(any(Beneficiary.class));
		verify(auditLogRepository, never()).save(any(AuditLog.class));
	}

	@Test
	void duplicateDatabaseRaceIsTranslatedSafely() {
		whenNoDuplicate();
		when(beneficiaryRepository.save(any(Beneficiary.class))).thenThrow(
				new DataIntegrityViolationException("uk_beneficiaries_customer_active_account"));

		DuplicateBeneficiaryException ex = assertThrows(DuplicateBeneficiaryException.class,
				() -> service.createBeneficiary(USER_ID, externalCreateRequest()));

		assertFalse(ex.getMessage().contains("123456789012"));
	}

	@Test
	void unrelatedDatabaseErrorIsNotTranslatedToDuplicate() {
		whenNoDuplicate();
		when(beneficiaryRepository.save(any(Beneficiary.class))).thenThrow(
				new DataIntegrityViolationException("some_other_constraint"));

		assertThrows(DataIntegrityViolationException.class,
				() -> service.createBeneficiary(USER_ID, externalCreateRequest()));
	}

	@Test
	void duplicateDatabaseRaceIsTranslatedFromNestedCause() {
		whenNoDuplicate();
		RuntimeException cause = new RuntimeException("uk_beneficiaries_customer_active_account");
		when(beneficiaryRepository.save(any(Beneficiary.class))).thenThrow(
				new DataIntegrityViolationException("constraint failure", cause));

		assertThrows(DuplicateBeneficiaryException.class,
				() -> service.createBeneficiary(USER_ID, externalCreateRequest()));
	}

	@Test
	void createWritesSafeAuditLog() {
		whenNoDuplicate();
		whenSavedAssignsId();

		service.createBeneficiary(USER_ID, externalCreateRequest());

		AuditLog auditLog = capturedAuditLog();
		assertEquals("BENEFICIARY_CREATED", auditLog.getAction());
		assertEquals("USER:" + USER_ID, auditLog.getActorUsername());
		assertTrue(auditLog.getReason().contains("********9012"));
		assertFalse(auditLog.getReason().contains("123456789012"));
	}

	@Test
	void ownerCanViewBeneficiary() {
		Beneficiary beneficiary = externalBeneficiary(BeneficiaryStatus.ACTIVE);
		when(beneficiaryRepository.findByIdAndCustomerIdAndStatusNot(BENEFICIARY_ID, CUSTOMER_ID,
				BeneficiaryStatus.DELETED)).thenReturn(Optional.of(beneficiary));

		BeneficiaryDetailsView view = service.getBeneficiary(USER_ID, BENEFICIARY_ID);

		assertEquals("********9012", view.getMaskedAccountNumber());
	}

	@Test
	void otherCustomerCannotViewBeneficiary() {
		when(beneficiaryRepository.findByIdAndCustomerIdAndStatusNot(BENEFICIARY_ID, CUSTOMER_ID,
				BeneficiaryStatus.DELETED)).thenReturn(Optional.empty());

		assertThrows(BeneficiaryNotFoundException.class, () -> service.getBeneficiary(USER_ID, BENEFICIARY_ID));
	}

	@Test
	void missingBeneficiaryReturnsNotFound() {
		assertThrows(BeneficiaryNotFoundException.class, () -> service.getBeneficiary(USER_ID, BENEFICIARY_ID));
	}

	@Test
	void searchAppliesOwnershipDeletedExclusionPageLimitAndSafeSort() {
		BeneficiarySearchCriteria criteria = new BeneficiarySearchCriteria();
		criteria.setSize(999);
		criteria.setSort("customer.user.passwordHash");
		criteria.setDirection("desc");
		when(beneficiaryRepository.findAll(any(Specification.class), any(Pageable.class)))
				.thenReturn(new PageImpl<>(List.of(externalBeneficiary(BeneficiaryStatus.ACTIVE))));

		Page<BeneficiaryListView> page = service.searchBeneficiaries(USER_ID, criteria);

		assertEquals(1, page.getTotalElements());
		ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
		verify(beneficiaryRepository).findAll(any(Specification.class), pageableCaptor.capture());
		assertEquals(50, pageableCaptor.getValue().getPageSize());
		assertTrue(pageableCaptor.getValue().getSort().toString().contains("beneficiaryName"));
	}

	@Test
	void searchNullCriteriaUsesDefaultPaginationAndSort() {
		when(beneficiaryRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(Page.empty());

		service.searchBeneficiaries(USER_ID, null);

		ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
		verify(beneficiaryRepository).findAll(any(Specification.class), pageableCaptor.capture());
		assertEquals(0, pageableCaptor.getValue().getPageNumber());
		assertEquals(10, pageableCaptor.getValue().getPageSize());
		assertTrue(pageableCaptor.getValue().getSort().toString().contains("beneficiaryName"));
	}

	@Test
	void searchNegativePageFallsBackToFirstPage() {
		BeneficiarySearchCriteria criteria = new BeneficiarySearchCriteria();
		criteria.setPage(-5);
		when(beneficiaryRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(Page.empty());

		service.searchBeneficiaries(USER_ID, criteria);

		ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
		verify(beneficiaryRepository).findAll(any(Specification.class), pageableCaptor.capture());
		assertEquals(0, pageableCaptor.getValue().getPageNumber());
	}

	@Test
	void searchAllowsWhitelistedUpdatedAtSortOnly() {
		BeneficiarySearchCriteria criteria = new BeneficiarySearchCriteria();
		criteria.setSort("updatedAt");
		criteria.setDirection("desc");
		when(beneficiaryRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(Page.empty());

		service.searchBeneficiaries(USER_ID, criteria);

		ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
		verify(beneficiaryRepository).findAll(any(Specification.class), pageableCaptor.capture());
		assertTrue(pageableCaptor.getValue().getSort().toString().contains("updatedAt"));
		assertTrue(pageableCaptor.getValue().getSort().toString().contains("DESC"));
	}

	@Test
	void searchRequestingDeletedStatusDoesNotExposeDeletedRecords() {
		BeneficiarySearchCriteria criteria = new BeneficiarySearchCriteria();
		criteria.setStatus(BeneficiaryStatus.DELETED);
		when(beneficiaryRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(Page.empty());

		Page<BeneficiaryListView> page = service.searchBeneficiaries(USER_ID, criteria);

		assertEquals(0, page.getTotalElements());
		verify(beneficiaryRepository).findAll(any(Specification.class), any(Pageable.class));
	}

	@Test
	void validExternalUpdateChangesAllowedFields() {
		Beneficiary beneficiary = externalBeneficiary(BeneficiaryStatus.ACTIVE);
		whenActiveBeneficiary(beneficiary);
		whenNoDuplicateExcluding();
		when(beneficiaryRepository.save(any(Beneficiary.class))).thenAnswer(invocation -> invocation.getArgument(0));

		service.updateBeneficiary(USER_ID, BENEFICIARY_ID, externalUpdateRequest("New Bank", "NEW01", 1L));

		assertEquals("NEW01", beneficiary.getBankCode());
		assertEquals("New Bank", beneficiary.getBankName());
		assertEquals("Main", beneficiary.getBranchName());
		assertEquals(BeneficiaryAccountType.CURRENT, beneficiary.getAccountType());
	}

	@Test
	void externalUpdateWithDuplicateAfterBankCodeChangeIsRejected() {
		Beneficiary beneficiary = externalBeneficiary(BeneficiaryStatus.ACTIVE);
		whenActiveBeneficiary(beneficiary);
		when(beneficiaryRepository.existsByCustomerIdAndBankCodeAndNormalizedAccountNumberAndStatusNotAndIdNot(
				CUSTOMER_ID, "NEW01", "123456789012", BeneficiaryStatus.DELETED, BENEFICIARY_ID)).thenReturn(true);

		assertThrows(DuplicateBeneficiaryException.class,
				() -> service.updateBeneficiary(USER_ID, BENEFICIARY_ID, externalUpdateRequest("New Bank", "NEW01", 1L)));
	}

	@Test
	void staleVersionIsRejected() {
		Beneficiary beneficiary = externalBeneficiary(BeneficiaryStatus.ACTIVE);
		whenActiveBeneficiary(beneficiary);

		assertThrows(BeneficiaryVersionConflictException.class,
				() -> service.updateBeneficiary(USER_ID, BENEFICIARY_ID, externalUpdateRequest("Bank", "EXB01", 99L)));
	}

	@Test
	void internalNicknameOnlyUpdateIsAllowed() {
		Beneficiary beneficiary = internalBeneficiary(BeneficiaryStatus.ACTIVE);
		whenActiveBeneficiary(beneficiary);
		when(beneficiaryRepository.save(any(Beneficiary.class))).thenAnswer(invocation -> invocation.getArgument(0));

		service.updateBeneficiary(USER_ID, BENEFICIARY_ID, internalUpdateRequest("New Nick", 1L));

		assertEquals("New Nick", beneficiary.getNickname());
	}

	@Test
	void internalAuthoritativeFieldModificationRejected() {
		Beneficiary beneficiary = internalBeneficiary(BeneficiaryStatus.ACTIVE);
		whenActiveBeneficiary(beneficiary);
		BeneficiaryUpdateRequest request = internalUpdateRequest("Nick", 1L);
		request.setBankName("Other Bank");

		assertThrows(InvalidBeneficiaryException.class,
				() -> service.updateBeneficiary(USER_ID, BENEFICIARY_ID, request));
	}

	@Test
	void updateCreatesAuditLog() {
		Beneficiary beneficiary = externalBeneficiary(BeneficiaryStatus.ACTIVE);
		whenActiveBeneficiary(beneficiary);
		when(beneficiaryRepository.save(any(Beneficiary.class))).thenAnswer(invocation -> invocation.getArgument(0));

		service.updateBeneficiary(USER_ID, BENEFICIARY_ID, externalUpdateRequest("Example Bank", "EXB01", 1L));

		assertEquals("BENEFICIARY_UPDATED", capturedAuditLog().getAction());
	}

	@Test
	void activeBeneficiaryCanDeactivateAndClearsFavourite() {
		Beneficiary beneficiary = externalBeneficiary(BeneficiaryStatus.ACTIVE);
		beneficiary.setFavourite(true);
		whenActiveBeneficiary(beneficiary);
		when(beneficiaryRepository.save(any(Beneficiary.class))).thenAnswer(invocation -> invocation.getArgument(0));

		service.deactivateBeneficiary(USER_ID, BENEFICIARY_ID);

		assertEquals(BeneficiaryStatus.INACTIVE, beneficiary.getStatus());
		assertFalse(beneficiary.isFavourite());
	}

	@Test
	void deactivationCreatesAuditLog() {
		Beneficiary beneficiary = externalBeneficiary(BeneficiaryStatus.ACTIVE);
		whenActiveBeneficiary(beneficiary);
		when(beneficiaryRepository.save(any(Beneficiary.class))).thenAnswer(invocation -> invocation.getArgument(0));

		service.deactivateBeneficiary(USER_ID, BENEFICIARY_ID);

		assertEquals("BENEFICIARY_DEACTIVATED", capturedAuditLog().getAction());
	}

	@Test
	void inactiveBeneficiaryCannotDeactivateAgain() {
		whenActiveBeneficiary(externalBeneficiary(BeneficiaryStatus.INACTIVE));

		assertThrows(InvalidBeneficiaryStateException.class,
				() -> service.deactivateBeneficiary(USER_ID, BENEFICIARY_ID));
	}

	@Test
	void inactiveBeneficiaryCanReactivate() {
		Beneficiary beneficiary = externalBeneficiary(BeneficiaryStatus.INACTIVE);
		whenActiveBeneficiary(beneficiary);
		whenNoDuplicateExcluding();
		when(beneficiaryRepository.save(any(Beneficiary.class))).thenAnswer(invocation -> invocation.getArgument(0));

		service.reactivateBeneficiary(USER_ID, BENEFICIARY_ID);

		assertEquals(BeneficiaryStatus.ACTIVE, beneficiary.getStatus());
		assertFalse(beneficiary.isFavourite());
	}

	@Test
	void reactivationWithDuplicateActiveBeneficiaryIsRejected() {
		Beneficiary beneficiary = externalBeneficiary(BeneficiaryStatus.INACTIVE);
		whenActiveBeneficiary(beneficiary);
		when(beneficiaryRepository.existsByCustomerIdAndBankCodeAndNormalizedAccountNumberAndStatusNotAndIdNot(
				CUSTOMER_ID, "EXB01", "123456789012", BeneficiaryStatus.DELETED, BENEFICIARY_ID)).thenReturn(true);

		assertThrows(DuplicateBeneficiaryException.class,
				() -> service.reactivateBeneficiary(USER_ID, BENEFICIARY_ID));
	}

	@Test
	void reactivationCreatesAuditLog() {
		Beneficiary beneficiary = externalBeneficiary(BeneficiaryStatus.INACTIVE);
		whenActiveBeneficiary(beneficiary);
		whenNoDuplicateExcluding();
		when(beneficiaryRepository.save(any(Beneficiary.class))).thenAnswer(invocation -> invocation.getArgument(0));

		service.reactivateBeneficiary(USER_ID, BENEFICIARY_ID);

		assertEquals("BENEFICIARY_REACTIVATED", capturedAuditLog().getAction());
	}

	@Test
	void activeBeneficiaryCannotReactivateAgain() {
		whenActiveBeneficiary(externalBeneficiary(BeneficiaryStatus.ACTIVE));

		assertThrows(InvalidBeneficiaryStateException.class,
				() -> service.reactivateBeneficiary(USER_ID, BENEFICIARY_ID));
	}

	@Test
	void internalReactivationRequiresEligibleTargetAccount() {
		Beneficiary beneficiary = internalBeneficiary(BeneficiaryStatus.INACTIVE);
		whenActiveBeneficiary(beneficiary);
		when(bankAccountRepository.findByAccountNumber("123456789012"))
				.thenReturn(Optional.of(internalAccount(AccountStatus.CLOSED)));

		assertThrows(InvalidBeneficiaryException.class,
				() -> service.reactivateBeneficiary(USER_ID, BENEFICIARY_ID));
	}

	@Test
	void deleteSoftDeletesAndClearsFavourite() {
		Beneficiary beneficiary = externalBeneficiary(BeneficiaryStatus.ACTIVE);
		beneficiary.setFavourite(true);
		whenActiveBeneficiary(beneficiary);
		when(beneficiaryRepository.save(any(Beneficiary.class))).thenAnswer(invocation -> invocation.getArgument(0));

		service.deleteBeneficiary(USER_ID, BENEFICIARY_ID);

		assertEquals(BeneficiaryStatus.DELETED, beneficiary.getStatus());
		assertFalse(beneficiary.isFavourite());
		verify(beneficiaryRepository, never()).delete(any(Beneficiary.class));
	}

	@Test
	void inactiveBeneficiaryCanBeSoftDeleted() {
		Beneficiary beneficiary = externalBeneficiary(BeneficiaryStatus.INACTIVE);
		whenActiveBeneficiary(beneficiary);
		when(beneficiaryRepository.save(any(Beneficiary.class))).thenAnswer(invocation -> invocation.getArgument(0));

		service.deleteBeneficiary(USER_ID, BENEFICIARY_ID);

		assertEquals(BeneficiaryStatus.DELETED, beneficiary.getStatus());
	}

	@Test
	void deletedBeneficiaryIsHiddenByOwnershipSafeLookup() {
		when(beneficiaryRepository.findByIdAndCustomerIdAndStatusNot(BENEFICIARY_ID, CUSTOMER_ID,
				BeneficiaryStatus.DELETED)).thenReturn(Optional.empty());

		assertThrows(BeneficiaryNotFoundException.class, () -> service.markFavourite(USER_ID, BENEFICIARY_ID));
	}

	@Test
	void deleteCreatesAuditLog() {
		Beneficiary beneficiary = externalBeneficiary(BeneficiaryStatus.INACTIVE);
		whenActiveBeneficiary(beneficiary);
		when(beneficiaryRepository.save(any(Beneficiary.class))).thenAnswer(invocation -> invocation.getArgument(0));

		service.deleteBeneficiary(USER_ID, BENEFICIARY_ID);

		assertEquals("BENEFICIARY_DELETED", capturedAuditLog().getAction());
	}

	@Test
	void activeBeneficiaryCanBeFavourited() {
		Beneficiary beneficiary = externalBeneficiary(BeneficiaryStatus.ACTIVE);
		whenActiveBeneficiary(beneficiary);
		when(beneficiaryRepository.save(any(Beneficiary.class))).thenAnswer(invocation -> invocation.getArgument(0));

		service.markFavourite(USER_ID, BENEFICIARY_ID);

		assertTrue(beneficiary.isFavourite());
	}

	@Test
	void favouriteCreatesAuditLog() {
		Beneficiary beneficiary = externalBeneficiary(BeneficiaryStatus.ACTIVE);
		whenActiveBeneficiary(beneficiary);
		when(beneficiaryRepository.save(any(Beneficiary.class))).thenAnswer(invocation -> invocation.getArgument(0));

		service.markFavourite(USER_ID, BENEFICIARY_ID);

		assertEquals("BENEFICIARY_FAVOURITED", capturedAuditLog().getAction());
	}

	@Test
	void inactiveBeneficiaryCannotBeFavourited() {
		whenActiveBeneficiary(externalBeneficiary(BeneficiaryStatus.INACTIVE));

		assertThrows(InvalidBeneficiaryStateException.class,
				() -> service.markFavourite(USER_ID, BENEFICIARY_ID));
	}

	@Test
	void alreadyFavouriteIsIdempotentWithoutAudit() {
		Beneficiary beneficiary = externalBeneficiary(BeneficiaryStatus.ACTIVE);
		beneficiary.setFavourite(true);
		whenActiveBeneficiary(beneficiary);

		service.markFavourite(USER_ID, BENEFICIARY_ID);

		verify(auditLogRepository, never()).save(any(AuditLog.class));
	}

	@Test
	void unfavouriteWorks() {
		Beneficiary beneficiary = externalBeneficiary(BeneficiaryStatus.ACTIVE);
		beneficiary.setFavourite(true);
		whenActiveBeneficiary(beneficiary);
		when(beneficiaryRepository.save(any(Beneficiary.class))).thenAnswer(invocation -> invocation.getArgument(0));

		service.removeFavourite(USER_ID, BENEFICIARY_ID);

		assertFalse(beneficiary.isFavourite());
	}

	@Test
	void unfavouriteCreatesAuditLog() {
		Beneficiary beneficiary = externalBeneficiary(BeneficiaryStatus.ACTIVE);
		beneficiary.setFavourite(true);
		whenActiveBeneficiary(beneficiary);
		when(beneficiaryRepository.save(any(Beneficiary.class))).thenAnswer(invocation -> invocation.getArgument(0));

		service.removeFavourite(USER_ID, BENEFICIARY_ID);

		assertEquals("BENEFICIARY_UNFAVOURITED", capturedAuditLog().getAction());
	}

	@Test
	void alreadyUnfavouriteIsIdempotentWithoutAudit() {
		Beneficiary beneficiary = externalBeneficiary(BeneficiaryStatus.ACTIVE);
		beneficiary.setFavourite(false);
		whenActiveBeneficiary(beneficiary);

		service.removeFavourite(USER_ID, BENEFICIARY_ID);

		verify(auditLogRepository, never()).save(any(AuditLog.class));
	}

	@Test
	void ownershipAlwaysDerivedFromAuthenticatedUser() {
		when(beneficiaryRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(Page.empty());

		service.searchBeneficiaries(USER_ID, new BeneficiarySearchCriteria());

		verify(customerRepository).findByUserId(USER_ID);
	}

	@Test
	void requestDtoDoesNotExposeCustomerId() {
		assertFalse(hasMethod(BeneficiaryCreateRequest.class, "getCustomerId"));
		assertFalse(hasMethod(BeneficiaryUpdateRequest.class, "getCustomerId"));
	}

	@Test
	void exceptionsDoNotContainFullAccountNumber() {
		when(beneficiaryRepository.existsByCustomerIdAndBankCodeAndNormalizedAccountNumberAndStatusNot(
				CUSTOMER_ID, "EXB01", "123456789012", BeneficiaryStatus.DELETED)).thenReturn(true);

		DuplicateBeneficiaryException ex = assertThrows(DuplicateBeneficiaryException.class,
				() -> service.createBeneficiary(USER_ID, externalCreateRequest()));

		assertFalse(ex.getMessage().contains("123456789012"));
	}

	private BeneficiaryCreateRequest externalCreateRequest() {
		return new BeneficiaryCreateRequest("Kasun Perera", "Rent", "Example Bank", "exb 01",
				"Main", "mb 01", "123 456-789012", BeneficiaryAccountType.SAVINGS,
				BeneficiaryType.EXTERNAL);
	}

	private BeneficiaryCreateRequest internalCreateRequestWithUntrustedBankDetails() {
		return new BeneficiaryCreateRequest("Wrong Name", "Friend", "Wrong Bank", "WRONG",
				"Wrong Branch", "BAD", "123 456-789012", BeneficiaryAccountType.SAVINGS,
				BeneficiaryType.INTERNAL);
	}

	private BeneficiaryUpdateRequest externalUpdateRequest(String bankName, String bankCode, Long version) {
		return new BeneficiaryUpdateRequest("Updated Name", "Updated", bankName, bankCode, "Main",
				"MB01", BeneficiaryAccountType.CURRENT, version);
	}

	private BeneficiaryUpdateRequest internalUpdateRequest(String nickname, Long version) {
		return new BeneficiaryUpdateRequest("Kasun Perera", nickname, "DigiBank", "DIGIBANK",
				null, "COL001", BeneficiaryAccountType.CURRENT, version);
	}

	private Beneficiary externalBeneficiary(BeneficiaryStatus status) {
		Beneficiary beneficiary = new Beneficiary(customer, "Kasun Perera", "Example Bank", "EXB01",
				"123456789012", "123456789012", BeneficiaryAccountType.SAVINGS, BeneficiaryType.EXTERNAL);
		beneficiary.setStatus(status);
		beneficiary.setVersion(1L);
		setId(beneficiary, BENEFICIARY_ID);
		return beneficiary;
	}

	private Beneficiary internalBeneficiary(BeneficiaryStatus status) {
		Beneficiary beneficiary = new Beneficiary(customer, "Kasun Perera", "DigiBank", "DIGIBANK",
				"123456789012", "123456789012", BeneficiaryAccountType.CURRENT, BeneficiaryType.INTERNAL);
		beneficiary.setBranchCode("COL001");
		beneficiary.setStatus(status);
		beneficiary.setVersion(1L);
		setId(beneficiary, BENEFICIARY_ID);
		return beneficiary;
	}

	private BankAccount internalAccount(AccountStatus status) {
		BankAccount account = new BankAccount(otherCustomer, "123456789012", AccountType.CURRENT, "COL001");
		account.setAccountStatus(status);
		return account;
	}

	private Customer customer(Long customerId, Long userId, String firstName, String lastName) {
		User user = new User("user" + userId, "user" + userId + "@example.com", "encoded");
		user.setRole(Role.CUSTOMER);
		setId(user, userId);
		Customer created = new Customer(user, "CUS" + customerId, firstName, lastName,
				LocalDate.of(1995, 1, 1), Gender.MALE, IdentityType.NATIONAL_ID, "123456789V",
				"+94712345678", "No 1", "Colombo");
		setId(created, customerId);
		return created;
	}

	private void whenNoDuplicate() {
		when(beneficiaryRepository.existsByCustomerIdAndBankCodeAndNormalizedAccountNumberAndStatusNot(
				eq(CUSTOMER_ID), any(String.class), eq("123456789012"), eq(BeneficiaryStatus.DELETED)))
				.thenReturn(false);
	}

	private void whenNoDuplicateExcluding() {
		when(beneficiaryRepository.existsByCustomerIdAndBankCodeAndNormalizedAccountNumberAndStatusNotAndIdNot(
				eq(CUSTOMER_ID), any(String.class), eq("123456789012"), eq(BeneficiaryStatus.DELETED),
				eq(BENEFICIARY_ID))).thenReturn(false);
	}

	private void whenSavedAssignsId() {
		when(beneficiaryRepository.save(any(Beneficiary.class))).thenAnswer(invocation -> {
			Beneficiary beneficiary = invocation.getArgument(0);
			setId(beneficiary, BENEFICIARY_ID);
			beneficiary.setVersion(1L);
			return beneficiary;
		});
	}

	private void whenActiveBeneficiary(Beneficiary beneficiary) {
		when(beneficiaryRepository.findByIdAndCustomerIdAndStatusNot(BENEFICIARY_ID, CUSTOMER_ID,
				BeneficiaryStatus.DELETED)).thenReturn(Optional.of(beneficiary));
	}

	private Beneficiary capturedSavedBeneficiary() {
		ArgumentCaptor<Beneficiary> captor = ArgumentCaptor.forClass(Beneficiary.class);
		verify(beneficiaryRepository).save(captor.capture());
		return captor.getValue();
	}

	private AuditLog capturedAuditLog() {
		ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
		verify(auditLogRepository).save(captor.capture());
		return captor.getValue();
	}

	private boolean hasMethod(Class<?> type, String methodName) {
		for (Method method : type.getMethods()) {
			if (methodName.equals(method.getName())) {
				return true;
			}
		}
		return false;
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
}
