package com.digibank.service;

import com.digibank.dto.bill.BillPaymentDetailsView;
import com.digibank.dto.bill.BillPaymentFormView;
import com.digibank.dto.bill.BillPaymentListView;
import com.digibank.dto.bill.BillPaymentRequest;
import com.digibank.dto.bill.SavedBillerRequest;
import com.digibank.dto.bill.SavedBillerView;
import java.util.List;

public interface BillPaymentService {
	BillPaymentFormView getPaymentForm(Long userId);
	BillPaymentDetailsView pay(Long userId, String actorUsername, BillPaymentRequest request);
	List<BillPaymentListView> getCustomerHistory(Long userId);
	BillPaymentDetailsView getCustomerPayment(Long userId, String referenceNumber);
	List<SavedBillerView> getSavedBillers(Long userId);
	SavedBillerView saveBiller(Long userId, String actorUsername, SavedBillerRequest request);
	void deleteBiller(Long userId, String actorUsername, Long savedBillerId);
	List<BillPaymentListView> getStaffPayments();
	BillPaymentDetailsView getStaffPayment(String referenceNumber);
}
