package com.digibank.repository;

import com.digibank.entity.SavedBiller;
import com.digibank.enums.BillerProvider;
import com.digibank.enums.SavedBillerStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SavedBillerRepository extends JpaRepository<SavedBiller, Long> {
	List<SavedBiller> findByCustomerIdAndStatusOrderByNicknameAsc(Long customerId, SavedBillerStatus status);
	Optional<SavedBiller> findByIdAndCustomerIdAndStatus(Long id, Long customerId, SavedBillerStatus status);
	boolean existsByCustomerIdAndProviderAndConsumerReferenceIgnoreCaseAndStatus(Long customerId,
			BillerProvider provider, String consumerReference, SavedBillerStatus status);
	boolean existsByCustomerIdAndProviderAndConsumerReferenceIgnoreCaseAndStatusAndIdNot(Long customerId,
			BillerProvider provider, String consumerReference, SavedBillerStatus status, Long id);
}
