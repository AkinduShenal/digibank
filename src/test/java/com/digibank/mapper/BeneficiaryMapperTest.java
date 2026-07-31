package com.digibank.mapper;

import com.digibank.dto.beneficiary.BeneficiaryDetailsView;
import com.digibank.dto.beneficiary.BeneficiaryListView;
import com.digibank.dto.beneficiary.BeneficiaryUpdateRequest;
import com.digibank.entity.Beneficiary;
import com.digibank.enums.BeneficiaryAccountType;
import com.digibank.enums.BeneficiaryStatus;
import com.digibank.enums.BeneficiaryType;
import com.digibank.util.SensitiveDataMasker;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BeneficiaryMapperTest {

	private final BeneficiaryMapper mapper = new BeneficiaryMapper(new SensitiveDataMasker());

	@Test
	void listViewMasksAccountNumber() {
		BeneficiaryListView view = mapper.toListView(beneficiary());

		assertEquals("********9012", view.getMaskedAccountNumber());
	}

	@Test
	void detailsViewMasksAccountNumber() {
		BeneficiaryDetailsView view = mapper.toDetailsView(beneficiary());

		assertEquals("********9012", view.getMaskedAccountNumber());
	}

	@Test
	void normalizedAccountNumberIsNotExposed() {
		assertFalse(hasMethod(BeneficiaryListView.class, "getNormalizedAccountNumber"));
		assertFalse(hasMethod(BeneficiaryDetailsView.class, "getNormalizedAccountNumber"));
	}

	@Test
	void correctEnumsAndStatusAreMapped() {
		BeneficiaryDetailsView view = mapper.toDetailsView(beneficiary());

		assertEquals(BeneficiaryAccountType.SAVINGS, view.getAccountType());
		assertEquals(BeneficiaryType.EXTERNAL, view.getBeneficiaryType());
		assertEquals(BeneficiaryStatus.ACTIVE, view.getStatus());
	}

	@Test
	void nullOptionalFieldsAreHandled() {
		Beneficiary beneficiary = beneficiary();
		beneficiary.setNickname(null);
		beneficiary.setBranchName(null);
		beneficiary.setBranchCode(null);

		BeneficiaryDetailsView view = mapper.toDetailsView(beneficiary);

		assertNull(view.getNickname());
		assertNull(view.getBranchName());
		assertNull(view.getBranchCode());
	}

	@Test
	void customerEntityIsNotExposed() {
		assertFalse(hasMethod(BeneficiaryListView.class, "getCustomer"));
		assertFalse(hasMethod(BeneficiaryDetailsView.class, "getCustomer"));
	}

	@Test
	void mapsSafeEditableFieldsToUpdateRequest() {
		BeneficiaryUpdateRequest request = mapper.toUpdateRequest(beneficiary());

		assertEquals("Kasun Perera", request.getBeneficiaryName());
		assertEquals("Example Bank", request.getBankName());
		assertEquals(BeneficiaryAccountType.SAVINGS, request.getAccountType());
		assertFalse(hasMethod(BeneficiaryUpdateRequest.class, "getAccountNumber"));
	}

	private Beneficiary beneficiary() {
		Beneficiary beneficiary = new Beneficiary(null, "Kasun Perera", "Example Bank", "EXB01",
				"123456789012", "123456789012", BeneficiaryAccountType.SAVINGS, BeneficiaryType.EXTERNAL);
		beneficiary.setNickname("Rent");
		beneficiary.setBranchName("Main Branch");
		beneficiary.setBranchCode("MB01");
		beneficiary.setStatus(BeneficiaryStatus.ACTIVE);
		beneficiary.setFavourite(true);
		beneficiary.setVersion(3L);
		return beneficiary;
	}

	private boolean hasMethod(Class<?> type, String methodName) {
		return Arrays.stream(type.getMethods()).map(Method::getName).anyMatch(methodName::equals);
	}
}
