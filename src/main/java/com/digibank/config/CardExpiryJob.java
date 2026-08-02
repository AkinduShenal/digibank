package com.digibank.config;

import com.digibank.repository.PaymentCardRepository;
import com.digibank.service.CardManagementService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnBean(PaymentCardRepository.class)
public class CardExpiryJob {
	private final CardManagementService cards;
	public CardExpiryJob(CardManagementService cards){this.cards=cards;}
	@Scheduled(cron="${digibank.card-expiry.cron:0 15 0 * * *}")
	public void expireCards(){cards.expireDueCards();}
}
