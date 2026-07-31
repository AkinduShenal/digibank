package com.digibank.dto.beneficiary;

import com.digibank.enums.BeneficiaryStatus;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BeneficiarySearchCriteriaTest {

	@Test
	void appliesSafeDefaults() {
		BeneficiarySearchCriteria criteria = new BeneficiarySearchCriteria();

		assertEquals(0, criteria.safePage());
		assertEquals(10, criteria.safeSize());
		assertEquals("beneficiaryName", criteria.safeSort());
		assertEquals("asc", criteria.safeDirection());
	}

	@Test
	void capsPageSizeAndRejectsUnsafeSort() {
		BeneficiarySearchCriteria criteria = new BeneficiarySearchCriteria();
		criteria.setSize(500);
		criteria.setSort("customer.passwordHash");
		criteria.setDirection("DESC");

		assertEquals(50, criteria.safeSize());
		assertEquals("beneficiaryName", criteria.safeSort());
		assertEquals("desc", criteria.safeDirection());
	}

	@Test
	void detectsDeletedStatusRequest() {
		BeneficiarySearchCriteria criteria = new BeneficiarySearchCriteria();
		criteria.setStatus(BeneficiaryStatus.DELETED);

		assertTrue(criteria.isDeletedStatusRequested());
	}
}
