package com.digibank.config;

import com.digibank.entity.ScheduledPayment;
import com.digibank.enums.ScheduleStatus;
import com.digibank.repository.ScheduledPaymentRepository;
import com.digibank.service.impl.ScheduledPaymentProcessor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@ConditionalOnBean(ScheduledPaymentRepository.class)
public class ScheduledPaymentJob {
	private final ScheduledPaymentRepository schedules;
	private final ScheduledPaymentProcessor processor;
	public ScheduledPaymentJob(ScheduledPaymentRepository schedules,ScheduledPaymentProcessor processor){this.schedules=schedules;this.processor=processor;}
	@Scheduled(fixedDelayString="${digibank.schedules.poll-delay-ms:60000}")
	public void processDuePayments(){
		schedules.findTop100ByStatusAndNextExecutionAtLessThanEqualOrderByNextExecutionAt(ScheduleStatus.SCHEDULED,LocalDateTime.now())
				.stream().map(ScheduledPayment::getId).forEach(processor::execute);
	}
}
