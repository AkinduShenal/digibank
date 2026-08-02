package com.digibank.service;

import com.digibank.dto.transfer.InternalAccountLookupView;
import com.digibank.dto.transfer.TransferDetailsView;
import com.digibank.dto.transfer.TransferFormView;
import com.digibank.dto.transfer.TransferListView;
import com.digibank.dto.transfer.TransferRequest;

import java.util.List;

public interface TransferService {

	TransferFormView getTransferForm(Long authenticatedUserId);

	InternalAccountLookupView lookupInternalAccount(Long authenticatedUserId, String accountNumber);

	TransferDetailsView transfer(Long authenticatedUserId, String actorUsername, TransferRequest request);

	List<TransferListView> getTransferHistory(Long authenticatedUserId);

	TransferDetailsView getTransfer(Long authenticatedUserId, String referenceNumber);
}
