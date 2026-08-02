package com.digibank.dto.transaction;

import com.digibank.enums.TransactionDirection;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

public class TransactionSearchCriteria {

	private String accountNumber;
	private TransactionDirection direction;
	@DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
	private LocalDate fromDate;
	@DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
	private LocalDate toDate;
	private String keyword;
	private int page;

	public String getAccountNumber() { return accountNumber; }
	public void setAccountNumber(String accountNumber) { this.accountNumber = accountNumber; }
	public TransactionDirection getDirection() { return direction; }
	public void setDirection(TransactionDirection direction) { this.direction = direction; }
	public LocalDate getFromDate() { return fromDate; }
	public void setFromDate(LocalDate fromDate) { this.fromDate = fromDate; }
	public LocalDate getToDate() { return toDate; }
	public void setToDate(LocalDate toDate) { this.toDate = toDate; }
	public String getKeyword() { return keyword; }
	public void setKeyword(String keyword) { this.keyword = keyword; }
	public int getPage() { return page; }
	public void setPage(int page) { this.page = page; }
}
