package com.digibank.repository;

import com.digibank.entity.BankAccount;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BankAccountRepository extends JpaRepository<BankAccount, Long> {

	Optional<BankAccount> findByAccountNumber(String accountNumber);

	List<BankAccount> findByCustomerId(Long customerId);

	boolean existsByAccountNumber(String accountNumber);
}
