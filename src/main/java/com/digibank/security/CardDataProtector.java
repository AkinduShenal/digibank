package com.digibank.security;

import com.digibank.exception.CardException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;

@Component
public class CardDataProtector {
	private static final String PREFIX = "v1:";
	private static final int IV_LENGTH = 12;
	private final SecretKeySpec encryptionKey;
	private final SecretKeySpec hashKey;
	private final SecureRandom random = new SecureRandom();

	public CardDataProtector(@Value("${digibank.card-security.key}") String secret) {
		try {
			if (secret == null || secret.length() < 24) {
				throw new IllegalArgumentException("DIGIBANK_CARD_SECURITY_KEY must contain at least 24 characters.");
			}
			byte[] digest = MessageDigest.getInstance("SHA-256").digest(secret.getBytes(StandardCharsets.UTF_8));
			this.encryptionKey = new SecretKeySpec(digest, "AES");
			this.hashKey = new SecretKeySpec(digest, "HmacSHA256");
		} catch (IllegalArgumentException ex) {
			throw ex;
		} catch (Exception ex) {
			throw new IllegalStateException("Card security could not be initialized.", ex);
		}
	}

	public String encrypt(String cardNumber) {
		try {
			byte[] iv = new byte[IV_LENGTH]; random.nextBytes(iv);
			Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
			cipher.init(Cipher.ENCRYPT_MODE, encryptionKey, new GCMParameterSpec(128, iv));
			byte[] encrypted = cipher.doFinal(cardNumber.getBytes(StandardCharsets.UTF_8));
			byte[] combined = new byte[iv.length + encrypted.length];
			System.arraycopy(iv, 0, combined, 0, iv.length);
			System.arraycopy(encrypted, 0, combined, iv.length, encrypted.length);
			return PREFIX + Base64.getEncoder().encodeToString(combined);
		} catch (Exception ex) {
			throw new CardException("Card number encryption failed.");
		}
	}

	public String decrypt(String ciphertext) {
		if (ciphertext == null) return null;
		if (!ciphertext.startsWith(PREFIX)) return ciphertext;
		try {
			byte[] combined = Base64.getDecoder().decode(ciphertext.substring(PREFIX.length()));
			byte[] iv = java.util.Arrays.copyOfRange(combined, 0, IV_LENGTH);
			byte[] encrypted = java.util.Arrays.copyOfRange(combined, IV_LENGTH, combined.length);
			Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
			cipher.init(Cipher.DECRYPT_MODE, encryptionKey, new GCMParameterSpec(128, iv));
			return new String(cipher.doFinal(encrypted), StandardCharsets.UTF_8);
		} catch (Exception ex) {
			throw new CardException("Card number could not be decrypted securely.");
		}
	}

	public String hash(String cardNumber) {
		try {
			Mac mac = Mac.getInstance("HmacSHA256"); mac.init(hashKey);
			return HexFormat.of().formatHex(mac.doFinal(cardNumber.getBytes(StandardCharsets.UTF_8)));
		} catch (Exception ex) {
			throw new CardException("Card number fingerprinting failed.");
		}
	}

	public String lastFour(String cardNumber) {
		return cardNumber.substring(cardNumber.length() - 4);
	}

	public boolean isEncrypted(String value) { return value != null && value.startsWith(PREFIX); }
}
