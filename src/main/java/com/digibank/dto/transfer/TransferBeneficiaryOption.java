package com.digibank.dto.transfer;

import com.digibank.enums.BeneficiaryType;

import java.math.BigDecimal;

public record TransferBeneficiaryOption(Long id, String name, String bankName, String maskedAccountNumber,
		BeneficiaryType beneficiaryType, BigDecimal transferLimit) {
}
