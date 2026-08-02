package com.digibank.service;

public interface PasswordResetService {
	String requestToken(String usernameOrEmail);
	boolean isTokenValid(String rawToken);
	void reset(String rawToken,String password,String confirmation);
}
