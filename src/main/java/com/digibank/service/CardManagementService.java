package com.digibank.service;

import com.digibank.dto.card.CardRequest;
import com.digibank.dto.card.CardRequestFormView;
import com.digibank.dto.card.CardView;
import com.digibank.enums.CardStatus;

import java.util.List;

public interface CardManagementService {
	CardRequestFormView getRequestForm(Long userId);
	CardView requestCard(Long userId, String actorUsername, CardRequest request);
	List<CardView> getCustomerCards(Long userId);
	CardView getCustomerCard(Long userId, String requestNumber);
	String revealCardNumber(Long userId, String actorUsername, String requestNumber);
	void activate(Long userId, String actorUsername, String requestNumber, String transactionPin);
	void blockByCustomer(Long userId, String actorUsername, String requestNumber, String transactionPin);
	void reactivateByCustomer(Long userId, String actorUsername, String requestNumber, String transactionPin);
	List<CardView> getCardsForReview(CardStatus status);
	CardView getCardForStaff(String requestNumber);
	void approve(String actorUsername, String requestNumber, String note);
	void reject(String actorUsername, String requestNumber, String reason);
	void blockByStaff(String actorUsername, String requestNumber, String reason);
	void reactivateByStaff(String actorUsername, String requestNumber, String note);
}
