package com.digibank.repository;

import com.digibank.entity.BankAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface BankAccountRepository extends JpaRepository<BankAccount, Long> {

	Optional<BankAccount> findByAccountNumber(String accountNumber);

	List<BankAccount> findByCustomerId(Long customerId);

	boolean existsByAccountNumber(String accountNumber);

	@Query(value = "select * from bank_accounts where account_number in (:accountNumbers) "
			+ "order by account_number for update", nativeQuery = true)
	List<BankAccount> findAllByAccountNumberInForUpdate(@Param("accountNumbers") List<String> accountNumbers);
}
