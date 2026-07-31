package com.digibank.service;

import com.digibank.dto.auth.CustomerRegistrationRequest;
import com.digibank.dto.auth.RegistrationResult;

public interface CustomerRegistrationService {

	RegistrationResult register(CustomerRegistrationRequest request);
}
