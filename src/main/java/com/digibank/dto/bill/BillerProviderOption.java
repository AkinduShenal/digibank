package com.digibank.dto.bill;

import com.digibank.enums.BillerCategory;
import com.digibank.enums.BillerProvider;

public record BillerProviderOption(BillerProvider provider, BillerCategory category, String displayName,
		String referenceLabel) {
}
