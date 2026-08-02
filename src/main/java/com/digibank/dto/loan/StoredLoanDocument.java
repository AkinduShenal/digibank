package com.digibank.dto.loan;

public record StoredLoanDocument(String originalFilename, String storedFilename, String contentType, long size) { }
