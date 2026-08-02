package com.digibank.service.impl;

import com.digibank.dto.transaction.StatementAccountOption;
import com.digibank.dto.transaction.StatementExport;
import com.digibank.dto.transaction.TransactionSearchCriteria;
import com.digibank.dto.transaction.TransactionStatementView;
import com.digibank.dto.transaction.TransactionView;
import com.digibank.entity.AccountTransaction;
import com.digibank.entity.BankAccount;
import com.digibank.entity.Customer;
import com.digibank.exception.CustomerProfileNotFoundException;
import com.digibank.exception.StatementException;
import com.digibank.exception.TransactionRecordNotFoundException;
import com.digibank.repository.AccountTransactionRepository;
import com.digibank.repository.BankAccountRepository;
import com.digibank.repository.CustomerRepository;
import com.digibank.service.AccountTransactionService;
import com.digibank.util.SensitiveDataMasker;
import com.digibank.util.SimplePdfDocument;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

@Service
public class AccountTransactionServiceImpl implements AccountTransactionService {

	private static final int PAGE_SIZE = 15;
	private static final int MAX_EXPORT_ROWS = 5000;
	private static final DateTimeFormatter EXPORT_DATE_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

	private final CustomerRepository customerRepository;
	private final BankAccountRepository bankAccountRepository;
	private final AccountTransactionRepository transactionRepository;
	private final SensitiveDataMasker dataMasker;

	public AccountTransactionServiceImpl(CustomerRepository customerRepository,
			BankAccountRepository bankAccountRepository, AccountTransactionRepository transactionRepository,
			SensitiveDataMasker dataMasker) {
		this.customerRepository = customerRepository;
		this.bankAccountRepository = bankAccountRepository;
		this.transactionRepository = transactionRepository;
		this.dataMasker = dataMasker;
	}

	@Override
	@Transactional(readOnly = true)
	public TransactionStatementView getStatement(Long userId, TransactionSearchCriteria criteria) {
		Search search = search(userId, criteria);
		Page<AccountTransaction> page = transactionRepository.search(userId, search.accountNumber(), search.direction(),
				search.fromDate(), search.toDate(), search.keyword(), PageRequest.of(search.page(), PAGE_SIZE));
		return new TransactionStatementView(search.accounts(), page.map(this::view));
	}

	@Override
	@Transactional(readOnly = true)
	public TransactionView getTransaction(Long userId, Long transactionId) {
		if (userId == null || transactionId == null) {
			throw new TransactionRecordNotFoundException();
		}
		return transactionRepository.findByIdAndAccountCustomerUserId(transactionId, userId)
				.map(this::view)
				.orElseThrow(TransactionRecordNotFoundException::new);
	}

	@Override
	@Transactional(readOnly = true)
	public List<TransactionView> getRecentTransactions(Long userId, int limit) {
		customer(userId);
		int safeLimit = Math.max(1, Math.min(limit, 10));
		return transactionRepository.findRecent(userId, PageRequest.of(0, safeLimit)).stream().map(this::view).toList();
	}

	@Override
	@Transactional(readOnly = true)
	public StatementExport exportCsv(Long userId, TransactionSearchCriteria criteria) {
		List<TransactionView> transactions = exportRows(userId, criteria);
		StringBuilder csv = new StringBuilder("Date,Reference,Account,Direction,Type,Counterparty,Counterparty Account,Description,Amount LKR,Balance After LKR\r\n");
		for (TransactionView transaction : transactions) {
			csv.append(csvCell(EXPORT_DATE_TIME.format(transaction.occurredAt()))).append(',')
					.append(csvCell(transaction.referenceNumber())).append(',')
					.append(csvCell(transaction.accountMasked())).append(',')
					.append(transaction.direction()).append(',')
					.append(transaction.transactionType()).append(',')
					.append(csvCell(transaction.counterpartyName())).append(',')
					.append(csvCell(transaction.counterpartyAccountMasked())).append(',')
					.append(csvCell(transaction.description())).append(',')
					.append(transaction.amount().toPlainString()).append(',')
					.append(decimal(transaction.balanceAfter())).append("\r\n");
		}
		byte[] content = ("\uFEFF" + csv).getBytes(StandardCharsets.UTF_8);
		return new StatementExport(content, "text/csv;charset=UTF-8", filename("csv"));
	}

	@Override
	@Transactional(readOnly = true)
	public StatementExport exportPdf(Long userId, TransactionSearchCriteria criteria) {
		List<TransactionView> transactions = exportRows(userId, criteria);
		List<String> lines = new ArrayList<>();
		lines.add("DIGIBANK ACCOUNT STATEMENT");
		lines.add("Generated: " + EXPORT_DATE_TIME.format(LocalDateTime.now()));
		lines.add("Transactions: " + transactions.size());
		lines.add("--------------------------------------------------------------------------------");
		for (TransactionView transaction : transactions) {
			String sign = transaction.direction().name().equals("DEBIT") ? "-" : "+";
			lines.add(EXPORT_DATE_TIME.format(transaction.occurredAt()) + " | " + transaction.referenceNumber()
					+ " | " + transaction.direction() + " " + sign + "LKR " + transaction.amount().toPlainString());
			lines.add("  " + transaction.counterpartyName() + " " + transaction.counterpartyAccountMasked()
					+ " | Account " + transaction.accountMasked() + " | Balance " + decimal(transaction.balanceAfter()));
		}
		return new StatementExport(SimplePdfDocument.create(lines), "application/pdf", filename("pdf"));
	}

	private List<TransactionView> exportRows(Long userId, TransactionSearchCriteria criteria) {
		Search search = search(userId, criteria);
		List<AccountTransaction> rows = transactionRepository.export(userId, search.accountNumber(), search.direction(),
				search.fromDate(), search.toDate(), search.keyword());
		return rows.stream().limit(MAX_EXPORT_ROWS).map(this::view).toList();
	}

	private Search search(Long userId, TransactionSearchCriteria criteria) {
		Customer customer = customer(userId);
		TransactionSearchCriteria safe = criteria == null ? new TransactionSearchCriteria() : criteria;
		List<BankAccount> ownedAccounts = bankAccountRepository.findByCustomerId(customer.getId());
		String accountNumber = trim(safe.getAccountNumber());
		if (accountNumber != null && ownedAccounts.stream()
				.noneMatch(account -> Objects.equals(account.getAccountNumber(), accountNumber))) {
			throw new StatementException("The selected account is not available for your profile.");
		}
		LocalDate from = safe.getFromDate();
		LocalDate to = safe.getToDate();
		if (from != null && to != null && from.isAfter(to)) {
			throw new StatementException("The statement start date cannot be after the end date.");
		}
		List<StatementAccountOption> accounts = ownedAccounts.stream()
				.sorted(Comparator.comparing(BankAccount::getAccountNumber))
				.map(account -> new StatementAccountOption(account.getAccountNumber(),
						dataMasker.maskAccountNumber(account.getAccountNumber()), account.getAccountType()))
				.toList();
		return new Search(accounts, accountNumber, safe.getDirection(), from == null ? null : from.atStartOfDay(),
				to == null ? null : to.plusDays(1).atStartOfDay(), trimToLength(safe.getKeyword(), 80),
				Math.max(safe.getPage(), 0));
	}

	private Customer customer(Long userId) {
		if (userId == null) {
			throw new CustomerProfileNotFoundException();
		}
		return customerRepository.findByUserId(userId).orElseThrow(CustomerProfileNotFoundException::new);
	}

	private TransactionView view(AccountTransaction transaction) {
		return new TransactionView(transaction.getId(),
				dataMasker.maskAccountNumber(transaction.getAccount().getAccountNumber()),
				transaction.getReferenceNumber(), transaction.getDirection(), transaction.getTransactionType(),
				transaction.getAmount(), transaction.getBalanceAfter(), transaction.getCounterpartyName(),
				transaction.getCounterpartyAccountMasked(), transaction.getDescription(), transaction.getOccurredAt());
	}

	private String filename(String extension) {
		return "digibank-statement-" + LocalDate.now() + "." + extension;
	}

	private String csvCell(String value) {
		String safe = value == null ? "" : value;
		if (!safe.isEmpty() && "=+-@".indexOf(safe.charAt(0)) >= 0) {
			safe = "'" + safe;
		}
		return '"' + safe.replace("\"", "\"\"") + '"';
	}

	private String decimal(BigDecimal value) {
		return value == null ? "-" : value.toPlainString();
	}

	private String trimToLength(String value, int length) {
		String trimmed = trim(value);
		return trimmed == null || trimmed.length() <= length ? trimmed : trimmed.substring(0, length);
	}

	private String trim(String value) {
		if (value == null) {
			return null;
		}
		String trimmed = value.trim();
		return trimmed.isEmpty() ? null : trimmed;
	}

	private record Search(List<StatementAccountOption> accounts, String accountNumber,
			com.digibank.enums.TransactionDirection direction, LocalDateTime fromDate, LocalDateTime toDate,
			String keyword, int page) {
	}
}
