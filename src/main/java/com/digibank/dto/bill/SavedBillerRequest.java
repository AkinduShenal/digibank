package com.digibank.dto.bill;

import com.digibank.enums.BillerProvider;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class SavedBillerRequest {
	@NotNull(message = "Select a service provider.")
	private BillerProvider provider;
	@NotBlank(message = "Enter a nickname.")
	@Size(min = 2, max = 80, message = "Nickname must contain 2 to 80 characters.")
	private String nickname;
	@NotBlank(message = "Enter the consumer or service reference.")
	@Size(min = 5, max = 50, message = "Reference must contain 5 to 50 characters.")
	private String consumerReference;

	public BillerProvider getProvider() { return provider; }
	public void setProvider(BillerProvider provider) { this.provider = provider; }
	public String getNickname() { return nickname; }
	public void setNickname(String nickname) { this.nickname = nickname; }
	public String getConsumerReference() { return consumerReference; }
	public void setConsumerReference(String consumerReference) { this.consumerReference = consumerReference; }
}
