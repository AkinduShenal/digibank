package com.digibank.entity;

import com.digibank.enums.BillerProvider;
import com.digibank.enums.SavedBillerStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "saved_billers")
public class SavedBiller extends BaseEntity {

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "customer_id", nullable = false)
	private Customer customer;

	@Enumerated(EnumType.STRING)
	@Column(name = "provider", nullable = false, length = 40)
	private BillerProvider provider;

	@Column(name = "nickname", nullable = false, length = 80)
	private String nickname;

	@Column(name = "consumer_reference", nullable = false, length = 50)
	private String consumerReference;

	@Enumerated(EnumType.STRING)
	@Column(name = "status", nullable = false, length = 20)
	private SavedBillerStatus status;

	protected SavedBiller() {
	}

	public SavedBiller(Customer customer, BillerProvider provider, String nickname, String consumerReference) {
		this.customer = customer;
		this.provider = provider;
		this.nickname = nickname;
		this.consumerReference = consumerReference;
		this.status = SavedBillerStatus.ACTIVE;
	}

	public void delete() { this.status = SavedBillerStatus.DELETED; }
	public void update(BillerProvider provider, String nickname, String consumerReference) {
		this.provider = provider;
		this.nickname = nickname;
		this.consumerReference = consumerReference;
	}

	public Customer getCustomer() { return customer; }
	public BillerProvider getProvider() { return provider; }
	public String getNickname() { return nickname; }
	public String getConsumerReference() { return consumerReference; }
	public SavedBillerStatus getStatus() { return status; }
}
