package com.digibank.dto.transaction;

public record StatementExport(byte[] content, String contentType, String filename) {
}
