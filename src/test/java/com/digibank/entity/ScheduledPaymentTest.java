package com.digibank.entity;

import com.digibank.enums.*;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.time.*;
import static org.junit.jupiter.api.Assertions.*;

class ScheduledPaymentTest {
	@Test void oneTimeScheduleCompletesAfterSuccess(){LocalDateTime due=LocalDateTime.of(2026,8,3,10,0);ScheduledPayment s=new ScheduledPayment(null,null,"SCH1",ScheduledPaymentType.FUND_TRANSFER,ScheduleRecurrence.ONCE,due,null,new BigDecimal("100"),null);s.processing();s.succeeded("TRF1",due);assertEquals(ScheduleStatus.COMPLETED,s.getStatus());assertEquals(1,s.getExecutionCount());assertEquals("TRF1",s.getLastExecutionReference());}
	@Test void monthlyScheduleAdvancesUntilEndDate(){LocalDateTime due=LocalDateTime.of(2026,8,3,10,0);ScheduledPayment s=new ScheduledPayment(null,null,"SCH2",ScheduledPaymentType.BILL_PAYMENT,ScheduleRecurrence.MONTHLY,due,LocalDate.of(2026,10,3),new BigDecimal("100"),null);s.processing();s.succeeded("BIL1",due);assertEquals(ScheduleStatus.SCHEDULED,s.getStatus());assertEquals(due.plusMonths(1),s.getNextExecutionAt());}
}
