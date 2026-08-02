package com.digibank.dto.transaction;

import org.springframework.data.domain.Page;

import java.util.List;

public record TransactionStatementView(
		List<StatementAccountOption> accounts,
		Page<TransactionView> transactions) {
}
