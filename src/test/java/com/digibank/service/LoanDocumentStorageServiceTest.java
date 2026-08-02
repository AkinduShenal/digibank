package com.digibank.service;

import com.digibank.exception.LoanException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;
import java.nio.file.Path;
import static org.junit.jupiter.api.Assertions.*;

class LoanDocumentStorageServiceTest {
	@TempDir Path directory;
	@Test void validPdfIsStoredWithGeneratedNameAndCanBeLoaded() {
		LoanDocumentStorageService service=new LoanDocumentStorageService(directory.toString());
		var stored=service.store(new MockMultipartFile("supportingDocument","payslip.pdf","application/pdf","%PDF-1.7 test".getBytes()));
		assertNotEquals("payslip.pdf",stored.storedFilename());assertTrue(stored.storedFilename().matches("[a-f0-9]{32}\\.pdf"));
		var loaded=service.load(stored.storedFilename(),stored.originalFilename(),stored.contentType());
		assertEquals("payslip.pdf",loaded.filename());assertArrayEquals("%PDF-1.7 test".getBytes(),loaded.content());
	}
	@Test void spoofedMimeTypeIsRejected() {
		LoanDocumentStorageService service=new LoanDocumentStorageService(directory.toString());
		assertThrows(LoanException.class,()->service.store(new MockMultipartFile("file","fake.pdf","application/pdf","not a pdf".getBytes())));
	}
	@Test void unsupportedFileTypeIsRejected() {
		LoanDocumentStorageService service=new LoanDocumentStorageService(directory.toString());
		assertThrows(LoanException.class,()->service.store(new MockMultipartFile("file","script.html","text/html","<html>".getBytes())));
	}
}
