package com.digibank.security;

import com.digibank.exception.CardException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CardDataProtectorTest {
	private final CardDataProtector protector=new CardDataProtector("test-card-security-key-with-32-characters");
	@Test void encryptsWithRandomIvAndDecrypts(){
		String pan="4532015112830366";String first=protector.encrypt(pan);String second=protector.encrypt(pan);
		assertTrue(protector.isEncrypted(first));assertNotEquals(first,second);assertEquals(pan,protector.decrypt(first));
		assertEquals(protector.hash(pan),protector.hash(pan));assertEquals("0366",protector.lastFour(pan));
	}
	@Test void rejectsTamperedCiphertext(){String encrypted=protector.encrypt("4532015112830366");String tampered=encrypted.substring(0,encrypted.length()-2)+"AA";assertThrows(CardException.class,()->protector.decrypt(tampered));}
}
