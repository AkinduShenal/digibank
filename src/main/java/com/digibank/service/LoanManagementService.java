package com.digibank.service;

import com.digibank.dto.loan.LoanApplicationFormView;
import com.digibank.dto.loan.LoanApplicationRequest;
import com.digibank.dto.loan.LoanApplicationView;
import com.digibank.dto.loan.LoanAccountOption;
import com.digibank.dto.loan.LoanRepaymentRequest;
import com.digibank.dto.loan.LoanDocumentDownload;
import com.digibank.enums.LoanStatus;

import java.math.BigDecimal;
import java.util.List;

public interface LoanManagementService {
	LoanApplicationFormView getApplicationForm(Long userId);
	LoanApplicationView apply(Long userId, String actorUsername, LoanApplicationRequest request);
	LoanApplicationRequest getPendingApplicationForEdit(Long userId, String applicationNumber);
	void updatePendingApplication(Long userId, String actorUsername, String applicationNumber, LoanApplicationRequest request);
	void withdrawPendingApplication(Long userId, String actorUsername, String applicationNumber);
	List<LoanApplicationView> getCustomerLoans(Long userId);
	LoanApplicationView getCustomerLoan(Long userId, String applicationNumber);
	List<LoanApplicationView> getLoansForReview(LoanStatus status);
	LoanApplicationView getLoanForStaff(String applicationNumber);
	LoanDocumentDownload getCustomerDocument(Long userId, String applicationNumber);
	LoanDocumentDownload getStaffDocument(String applicationNumber);
	void approveAndDisburse(String actorUsername, String applicationNumber, BigDecimal approvedAmount, String note);
	void reject(String actorUsername, String applicationNumber, String reason);
	List<LoanAccountOption> getRepaymentAccounts(Long userId);
	String payInstallment(Long userId, String actorUsername, String applicationNumber, int installmentNumber,
			LoanRepaymentRequest request);
}
