package com.digibank.repository;

import com.digibank.entity.Customer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CustomerRepository extends JpaRepository<Customer, Long> {

	Optional<Customer> findByUserId(Long userId);

	Optional<Customer> findByCustomerNumber(String customerNumber);

	Optional<Customer> findByIdentityNumberIgnoreCase(String identityNumber);

	boolean existsByIdentityNumberIgnoreCase(String identityNumber);
}
