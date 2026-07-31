package com.digibank.service;

import com.digibank.dto.customer.AccountSummaryView;
import com.digibank.dto.customer.CustomerDashboardView;
import com.digibank.dto.customer.CustomerProfileUpdateRequest;
import com.digibank.dto.customer.CustomerProfileView;
import com.digibank.security.CustomUserDetails;

public interface CustomerProfileService {

	CustomerDashboardView getDashboard(CustomUserDetails userDetails);

	CustomerProfileView getProfile(CustomUserDetails userDetails);

	CustomerProfileUpdateRequest getProfileUpdateRequest(CustomUserDetails userDetails);

	CustomerProfileView updateProfile(CustomUserDetails userDetails, CustomerProfileUpdateRequest request);

	AccountSummaryView getAccountDetails(CustomUserDetails userDetails, String accountNumber);
}
