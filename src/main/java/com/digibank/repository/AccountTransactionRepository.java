package com.digibank.repository;

import com.digibank.entity.AccountTransaction;
import com.digibank.enums.TransactionDirection;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface AccountTransactionRepository extends JpaRepository<AccountTransaction, Long> {

	@Query("""
			select entry from AccountTransaction entry
			where entry.account.customer.user.id = :userId
			and (:accountNumber is null or entry.account.accountNumber = :accountNumber)
			and (:direction is null or entry.direction = :direction)
			and (:fromDate is null or entry.occurredAt >= :fromDate)
			and (:toDate is null or entry.occurredAt < :toDate)
			and (:keyword is null or lower(entry.referenceNumber) like lower(concat('%', :keyword, '%'))
				or lower(entry.counterpartyName) like lower(concat('%', :keyword, '%'))
				or lower(coalesce(entry.description, '')) like lower(concat('%', :keyword, '%')))
			order by entry.occurredAt desc, entry.id desc
			""")
	Page<AccountTransaction> search(@Param("userId") Long userId, @Param("accountNumber") String accountNumber,
			@Param("direction") TransactionDirection direction, @Param("fromDate") LocalDateTime fromDate,
			@Param("toDate") LocalDateTime toDate, @Param("keyword") String keyword, Pageable pageable);

	@Query("""
			select entry from AccountTransaction entry
			where entry.account.customer.user.id = :userId
			and (:accountNumber is null or entry.account.accountNumber = :accountNumber)
			and (:direction is null or entry.direction = :direction)
			and (:fromDate is null or entry.occurredAt >= :fromDate)
			and (:toDate is null or entry.occurredAt < :toDate)
			and (:keyword is null or lower(entry.referenceNumber) like lower(concat('%', :keyword, '%'))
				or lower(entry.counterpartyName) like lower(concat('%', :keyword, '%'))
				or lower(coalesce(entry.description, '')) like lower(concat('%', :keyword, '%')))
			order by entry.occurredAt desc, entry.id desc
			""")
	List<AccountTransaction> export(@Param("userId") Long userId, @Param("accountNumber") String accountNumber,
			@Param("direction") TransactionDirection direction, @Param("fromDate") LocalDateTime fromDate,
			@Param("toDate") LocalDateTime toDate, @Param("keyword") String keyword);

	Optional<AccountTransaction> findByIdAndAccountCustomerUserId(Long id, Long userId);

	@Query("select entry from AccountTransaction entry "
			+ "where entry.account.customer.user.id = :userId "
			+ "order by entry.occurredAt desc, entry.id desc")
	List<AccountTransaction> findRecent(@Param("userId") Long userId, Pageable pageable);
}
