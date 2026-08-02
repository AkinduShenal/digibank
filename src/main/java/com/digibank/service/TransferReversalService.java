package com.digibank.service;

import com.digibank.dto.transfer.StaffTransferView;
import com.digibank.enums.TransferStatus;
import org.springframework.data.domain.Page;

public interface TransferReversalService {
	Page<StaffTransferView> search(String query, TransferStatus status, int page, int size);
	StaffTransferView get(String referenceNumber);
	String reverse(String actorUsername,String referenceNumber,String reason);
}
