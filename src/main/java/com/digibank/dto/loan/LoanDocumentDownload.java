package com.digibank.dto.loan;

public record LoanDocumentDownload(String filename, String contentType, byte[] content) { }
