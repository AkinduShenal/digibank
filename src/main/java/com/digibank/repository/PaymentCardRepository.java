package com.digibank.repository;

import com.digibank.entity.PaymentCard;
import com.digibank.enums.CardStatus;
import com.digibank.enums.CardType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface PaymentCardRepository extends JpaRepository<PaymentCard, Long> {
	List<PaymentCard> findByCustomerUserIdOrderByRequestedAtDesc(Long userId);
	Optional<PaymentCard> findByRequestNumberAndCustomerUserId(String requestNumber, Long userId);
	Optional<PaymentCard> findByRequestNumber(String requestNumber);
	List<PaymentCard> findByStatusOrderByRequestedAtAsc(CardStatus status);
	boolean existsByBankAccountIdAndCardTypeAndStatusIn(Long accountId, CardType type, Collection<CardStatus> statuses);
	boolean existsByCardNumber(String cardNumber);
	boolean existsByRequestNumber(String requestNumber);

	@Query(value = "select * from payment_cards where request_number = :requestNumber for update", nativeQuery = true)
	Optional<PaymentCard> findByRequestNumberForUpdate(@Param("requestNumber") String requestNumber);
}
