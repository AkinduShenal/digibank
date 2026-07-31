package com.digibank.security;

public interface TransactionPinEncoder {

	String encode(String rawTransactionPin);
}
