package com.digibank.dto.card;

import com.digibank.enums.CardType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class CardRequest {
	@NotBlank(message = "Select an account for the card.")
	private String accountNumber;

	@NotNull(message = "Select a card type.")
	private CardType cardType;

	public String getAccountNumber() { return accountNumber; }
	public void setAccountNumber(String accountNumber) { this.accountNumber = accountNumber; }
	public CardType getCardType() { return cardType; }
	public void setCardType(CardType cardType) { this.cardType = cardType; }
}
