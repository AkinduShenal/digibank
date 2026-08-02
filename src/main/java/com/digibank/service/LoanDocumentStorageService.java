package com.digibank.service;

import com.digibank.dto.loan.LoanDocumentDownload;
import com.digibank.dto.loan.StoredLoanDocument;
import com.digibank.exception.LoanException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;

@Service
public class LoanDocumentStorageService {
	private static final long MAX_SIZE = 5L * 1024L * 1024L;
	private static final Map<String,String> TYPES = Map.of(
			"application/pdf", ".pdf", "image/jpeg", ".jpg", "image/png", ".png");
	private final Path root;

	public LoanDocumentStorageService(@Value("${digibank.loan-document-storage:uploads/loan-documents}") String root) {
		this.root = Paths.get(root).toAbsolutePath().normalize();
	}

	public StoredLoanDocument store(MultipartFile file) {
		if (file == null || file.isEmpty()) return null;
		if (file.getSize() <= 0 || file.getSize() > MAX_SIZE) throw new LoanException("Supporting document must be 5 MB or smaller.");
		String type = TYPES.containsKey(file.getContentType()) ? file.getContentType() : null;
		if (type == null) throw new LoanException("Only PDF, JPEG and PNG supporting documents are accepted.");
		String original = safeOriginalName(file.getOriginalFilename(), TYPES.get(type));
		String stored = UUID.randomUUID().toString().replace("-", "") + TYPES.get(type);
		Path target = resolve(stored);
		try {
			Files.createDirectories(root);
			byte[] bytes = file.getBytes();
			if (!matchesSignature(type, bytes)) throw new LoanException("The supporting document content does not match its file type.");
			Files.write(target, bytes, StandardOpenOption.CREATE_NEW);
			deleteOnRollback(target);
			return new StoredLoanDocument(original, stored, type, bytes.length);
		} catch (LoanException ex) { throw ex; }
		catch (IOException ex) { throw new LoanException("Supporting document could not be stored securely."); }
	}

	public LoanDocumentDownload load(String storedName, String originalName, String contentType) {
		if (storedName == null || !TYPES.containsKey(contentType)) throw new LoanException("No uploaded supporting document is available.");
		try {
			byte[] content = Files.readAllBytes(resolve(storedName));
			if (!matchesSignature(contentType, content)) throw new LoanException("The stored supporting document failed validation.");
			return new LoanDocumentDownload(safeOriginalName(originalName, TYPES.get(contentType)), contentType, content);
		} catch (NoSuchFileException ex) { throw new LoanException("The uploaded supporting document was not found."); }
		catch (IOException ex) { throw new LoanException("The supporting document could not be read."); }
	}

	public void deleteAfterCommit(String storedName) {
		if (storedName == null) return;
		Runnable delete = () -> { try { Files.deleteIfExists(resolve(storedName)); } catch (IOException ignored) { } };
		if (TransactionSynchronizationManager.isSynchronizationActive()) {
			TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
				@Override public void afterCommit() { delete.run(); }
			});
		} else delete.run();
	}

	private Path resolve(String storedName) {
		if (storedName == null || !storedName.matches("[a-f0-9]{32}\\.(pdf|jpg|png)")) throw new LoanException("Invalid supporting document path.");
		Path path = root.resolve(storedName).normalize();
		if (!path.startsWith(root)) throw new LoanException("Invalid supporting document path.");
		return path;
	}
	private void deleteOnRollback(Path path) {
		if (!TransactionSynchronizationManager.isSynchronizationActive()) return;
		TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
			@Override public void afterCompletion(int status) {
				if (status != STATUS_COMMITTED) try { Files.deleteIfExists(path); } catch (IOException ignored) { }
			}
		});
	}
	private String safeOriginalName(String value, String extension) {
		String name = value == null ? "supporting-document" + extension : value.replace('\\', '/');
		if (name.contains("/")) name = name.substring(name.lastIndexOf('/') + 1);
		name = name.replaceAll("[^A-Za-z0-9._ -]", "_").trim();
		if (name.isEmpty()) name = "supporting-document" + extension;
		return name.length() <= 255 ? name : name.substring(name.length() - 255);
	}
	private boolean matchesSignature(String type, byte[] bytes) {
		if (bytes == null) return false;
		return switch (type) {
			case "application/pdf" -> bytes.length >= 5 && bytes[0]=='%' && bytes[1]=='P' && bytes[2]=='D' && bytes[3]=='F' && bytes[4]=='-';
			case "image/jpeg" -> bytes.length >= 3 && (bytes[0]&255)==0xff && (bytes[1]&255)==0xd8 && (bytes[2]&255)==0xff;
			case "image/png" -> bytes.length >= 8 && (bytes[0]&255)==0x89 && bytes[1]=='P' && bytes[2]=='N' && bytes[3]=='G' && bytes[4]==13 && bytes[5]==10 && bytes[6]==26 && bytes[7]==10;
			default -> false;
		};
	}
}
