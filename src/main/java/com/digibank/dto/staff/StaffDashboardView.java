package com.digibank.dto.staff;

public record StaffDashboardView(
		long totalCustomers,
		long pendingCustomers,
		long activeCustomers,
		long totalAccounts,
		long pendingAccounts,
		long frozenAccounts,
		long pendingBeneficiaries) {
}
