package com.digibank.repository;

import com.digibank.entity.PasswordResetToken;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;

public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken,Long>{
	Optional<PasswordResetToken> findByTokenHash(String tokenHash);
	List<PasswordResetToken> findByUserIdAndUsedAtIsNull(Long userId);
}
