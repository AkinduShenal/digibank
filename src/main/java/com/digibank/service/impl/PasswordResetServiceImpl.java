package com.digibank.service.impl;

import com.digibank.entity.*;
import com.digibank.repository.*;
import com.digibank.service.PasswordResetService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.time.LocalDateTime;
import java.util.HexFormat;

@Service
public class PasswordResetServiceImpl implements PasswordResetService {
	private final UserRepository users; private final PasswordResetTokenRepository tokens; private final PasswordEncoder passwords; private final AuditLogRepository audits;
	private final SecureRandom random=new SecureRandom();
	public PasswordResetServiceImpl(UserRepository users,PasswordResetTokenRepository tokens,PasswordEncoder passwords,AuditLogRepository audits){this.users=users;this.tokens=tokens;this.passwords=passwords;this.audits=audits;}
	@Override @Transactional public String requestToken(String identifier){
		if(identifier==null||identifier.isBlank())return null;
		var user=users.findByUsernameIgnoreCase(identifier.trim()).or(()->users.findByEmailIgnoreCase(identifier.trim())).orElse(null);
		if(user==null)return null;
		LocalDateTime now=LocalDateTime.now();tokens.findByUserIdAndUsedAtIsNull(user.getId()).forEach(t->t.markUsed(now));
		byte[] bytes=new byte[32];random.nextBytes(bytes);String raw=HexFormat.of().formatHex(bytes);
		tokens.save(new PasswordResetToken(user,hash(raw),now.plusMinutes(30)));return raw;
	}
	@Override @Transactional(readOnly=true) public boolean isTokenValid(String raw){return raw!=null&&tokens.findByTokenHash(hash(raw)).map(t->t.usableAt(LocalDateTime.now())).orElse(false);}
	@Override @Transactional public void reset(String raw,String password,String confirmation){
		if(password==null||!password.equals(confirmation))throw new IllegalArgumentException("Passwords do not match.");
		if(!password.matches("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^A-Za-z0-9]).{8,72}$"))throw new IllegalArgumentException("Use 8-72 characters with uppercase, lowercase, a number and a special character.");
		PasswordResetToken token=tokens.findByTokenHash(hash(raw)).filter(t->t.usableAt(LocalDateTime.now())).orElseThrow(()->new IllegalArgumentException("This reset link is invalid or expired."));
		token.getUser().setPasswordHash(passwords.encode(password));token.getUser().setFailedLoginAttempts(0);token.getUser().setAccountNonLocked(true);token.markUsed(LocalDateTime.now());
		users.save(token.getUser());audits.save(new AuditLog(token.getUser().getUsername(),"PASSWORD_RESET","USER",token.getUser().getUsername(),null,null,"Password reset with single-use token.",LocalDateTime.now()));
	}
	private String hash(String value){try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));}catch(NoSuchAlgorithmException ex){throw new IllegalStateException(ex);}}
}
