package com.digibank.dto.bill;

import com.digibank.enums.BillerCategory;
import com.digibank.enums.BillerProvider;

public record SavedBillerView(Long id, String nickname, BillerProvider provider, BillerCategory category,
		String providerName, String maskedReference) {
}
