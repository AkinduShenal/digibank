package com.digibank.config;

import com.digibank.security.CardDataProtector;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
@Profile("!test")
public class CardNumberSecurityMigration implements ApplicationRunner {
	private final JdbcTemplate jdbcTemplate;
	private final CardDataProtector protector;

	public CardNumberSecurityMigration(JdbcTemplate jdbcTemplate, CardDataProtector protector) {
		this.jdbcTemplate = jdbcTemplate;
		this.protector = protector;
	}

	@Override
	@Transactional
	public void run(ApplicationArguments args) {
		List<LegacyCard> cards = jdbcTemplate.query(
				"select id, card_number from payment_cards where card_number is not null and card_number not like 'v1:%'",
				(rs, row) -> new LegacyCard(rs.getLong("id"), rs.getString("card_number")));
		for (LegacyCard card : cards) {
			jdbcTemplate.update("update payment_cards set card_number=?, card_number_hash=?, card_last_four=? where id=?",
					protector.encrypt(card.number()), protector.hash(card.number()), protector.lastFour(card.number()), card.id());
		}
	}

	private record LegacyCard(Long id, String number) { }
}
