package com.digibank.dto.transfer;

import java.util.List;

public record TransferFormView(List<TransferAccountOption> accounts,
		List<TransferBeneficiaryOption> beneficiaries) {
}
