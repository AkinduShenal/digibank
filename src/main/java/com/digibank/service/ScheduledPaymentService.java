package com.digibank.service;

import com.digibank.dto.schedule.*;
import java.util.List;

public interface ScheduledPaymentService {
	ScheduledPaymentView create(Long userId, String actorUsername, ScheduledPaymentRequest request);
	List<ScheduledPaymentView> list(Long userId);
	ScheduledPaymentView get(Long userId, String reference);
	ScheduledPaymentUpdateRequest getUpdateRequest(Long userId, String reference);
	void update(Long userId, String actorUsername, String reference, ScheduledPaymentUpdateRequest request);
	void cancel(Long userId, String actorUsername, String reference);
}
