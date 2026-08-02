package com.digibank.dto.bill;

import com.digibank.enums.BillerCategory;
import java.util.List;

public record BillPaymentFormView(List<BillAccountOption> accounts, List<SavedBillerView> savedBillers,
		List<BillerProviderOption> providers, BillerCategory[] categories) {
}
