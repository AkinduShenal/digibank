package com.digibank.validation.beneficiary;

import com.digibank.dto.beneficiary.BeneficiaryCreateRequest;
import com.digibank.enums.BeneficiaryAccountType;
import com.digibank.enums.BeneficiaryType;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BeneficiaryRequestValidatorTest {

	private static ValidatorFactory validatorFactory;
	private static Validator validator;

	@BeforeAll
	static void setUpValidator() {
		validatorFactory = Validation.buildDefaultValidatorFactory();
		validator = validatorFactory.getValidator();
	}

	@AfterAll
	static void closeValidator() {
		validatorFactory.close();
	}

	@Test
	void validInternalRequestPassesWithoutDatabaseAccess() {
		assertTrue(validator.validate(validInternalRequest()).isEmpty());
	}

	@Test
	void validExternalRequestPasses() {
		assertTrue(validator.validate(validExternalRequest()).isEmpty());
	}

	@Test
	void externalRequestWithoutBankNameFailsOnBankName() {
		BeneficiaryCreateRequest request = validExternalRequest();
		request.setBankName(" ");

		assertHasFieldError(validator.validate(request), "bankName");
	}

	@Test
	void externalRequestWithoutBankCodeFailsOnBankCode() {
		BeneficiaryCreateRequest request = validExternalRequest();
		request.setBankCode(null);

		assertHasFieldError(validator.validate(request), "bankCode");
	}

	@Test
	void missingAccountNumberFailsOnAccountNumber() {
		BeneficiaryCreateRequest request = validExternalRequest();
		request.setAccountNumber(" ");

		assertHasFieldError(validator.validate(request), "accountNumber");
	}

	@Test
	void missingBeneficiaryTypeFailsOnBeneficiaryType() {
		BeneficiaryCreateRequest request = validExternalRequest();
		request.setBeneficiaryType(null);

		assertHasFieldError(validator.validate(request), "beneficiaryType");
	}

	@Test
	void invalidAccountNumberFormatFailsOnAccountNumber() {
		BeneficiaryCreateRequest request = validExternalRequest();
		request.setAccountNumber("123ABC");

		assertHasFieldError(validator.validate(request), "accountNumber");
	}

	@Test
	void overlongNameFailsOnBeneficiaryName() {
		BeneficiaryCreateRequest request = validExternalRequest();
		request.setBeneficiaryName("A".repeat(121));

		assertHasFieldError(validator.validate(request), "beneficiaryName");
	}

	@Test
	void whitespaceOnlyNicknameFailsOnNickname() {
		BeneficiaryCreateRequest request = validExternalRequest();
		request.setNickname("   ");

		assertHasFieldError(validator.validate(request), "nickname");
	}

	private BeneficiaryCreateRequest validInternalRequest() {
		return new BeneficiaryCreateRequest("Kasun Perera", "", null, null, null, null,
				"123 456-789", BeneficiaryAccountType.SAVINGS, BeneficiaryType.INTERNAL);
	}

	private BeneficiaryCreateRequest validExternalRequest() {
		return new BeneficiaryCreateRequest("Kasun Perera", "Rent", "Example Bank", "EXB01",
				"Main Branch", "MB_01", "123 456-789", BeneficiaryAccountType.CURRENT,
				BeneficiaryType.EXTERNAL);
	}

	private void assertHasFieldError(Set<ConstraintViolation<BeneficiaryCreateRequest>> violations, String field) {
		assertFalse(violations.isEmpty());
		assertTrue(violations.stream().anyMatch(violation -> field.equals(violation.getPropertyPath().toString())));
	}
}
