package com.digibank.service;

import com.digibank.dto.customer.AccountSummaryView;
import com.digibank.dto.staff.StaffCustomerView;
import com.digibank.enums.AccountType;

import java.util.List;

public interface AccountManagementService {

	AccountSummaryView activateAccount(String actorUsername, String accountNumber);

	AccountSummaryView freezeAccount(String actorUsername, String accountNumber, String reason);

	AccountSummaryView unfreezeAccount(String actorUsername, String accountNumber, String reason);

	AccountSummaryView deactivateAccount(String actorUsername, String accountNumber, String reason);

	AccountSummaryView closeAccount(String actorUsername, String customerNumber, String accountNumber, String reason);

	AccountSummaryView reactivateEligibleAccount(String actorUsername, String accountNumber, String reason);

	AccountSummaryView changeAccountType(String actorUsername, String accountNumber, AccountType accountType,
			String reason);

	AccountSummaryView requestAccountClosure(String actorUsername, String accountNumber, String reason);

	List<StaffCustomerView> getCustomerRecords();

	StaffCustomerView getCustomerDetails(String customerNumber);
}
