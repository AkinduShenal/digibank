package com.digibank.service;

import com.digibank.dto.transaction.StatementExport;
import com.digibank.dto.transaction.TransactionSearchCriteria;
import com.digibank.dto.transaction.TransactionStatementView;
import com.digibank.dto.transaction.TransactionView;

import java.util.List;

public interface AccountTransactionService {

	TransactionStatementView getStatement(Long userId, TransactionSearchCriteria criteria);

	TransactionView getTransaction(Long userId, Long transactionId);

	List<TransactionView> getRecentTransactions(Long userId, int limit);

	StatementExport exportCsv(Long userId, TransactionSearchCriteria criteria);

	StatementExport exportPdf(Long userId, TransactionSearchCriteria criteria);
}
