package com.digibank.controller;

import com.digibank.dto.schedule.*;
import com.digibank.dto.transfer.*;
import com.digibank.dto.bill.*;
import com.digibank.entity.User;
import com.digibank.enums.*;
import com.digibank.exception.ScheduledPaymentException;
import com.digibank.security.*;
import com.digibank.service.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest({CustomerScheduledPaymentController.class, LegacyScheduleController.class,
        CustomerTransferController.class, CustomerBillPaymentController.class})
@Import(SecurityConfig.class)
class CustomerScheduledPaymentControllerTest {
    @Autowired MockMvc mvc;
    @MockitoBean ScheduledPaymentService schedules;
    @MockitoBean TransferService transfers;
    @MockitoBean BillPaymentService bills;
    @MockitoBean CustomAuthenticationSuccessHandler successHandler;
    CustomUserDetails customer;

    @BeforeEach void setup() {
        User user = new User("customer", "customer@example.com", "hash");
        user.setRole(Role.CUSTOMER);
        ReflectionTestUtils.setField(user, "id", 7L);
        customer = new CustomUserDetails(user, CustomerStatus.ACTIVE);
        when(transfers.getTransferForm(7L)).thenReturn(new TransferFormView(
                List.of(new TransferAccountOption("111122223333", "********3333", AccountType.SAVINGS, new BigDecimal("5000"))), List.of()));
        when(bills.getPaymentForm(7L)).thenReturn(new BillPaymentFormView(
                List.of(new BillAccountOption("111122223333", "********3333", "SAVINGS", new BigDecimal("5000"))), List.of(), List.of(), BillerCategory.values()));
    }

    ScheduledPaymentType type(String module) { return CustomerScheduledPaymentController.type(module); }
    String base(String module) { return "/customer/" + module + "/schedules"; }
    ScheduledPaymentView view(String module, ScheduleStatus status) {
        return new ScheduledPaymentView("SCHTEST", type(module), status, ScheduleRecurrence.ONCE,
                "********3333", "Recipient", new BigDecimal("100"), "Test", LocalDateTime.now().plusDays(2), null, 0, null, null, null);
    }
    ScheduledPaymentUpdateRequest updateRequest() {
        var r = new ScheduledPaymentUpdateRequest(); r.setAmount(new BigDecimal("100"));
        r.setRecurrence(ScheduleRecurrence.ONCE); r.setNextExecutionAt(LocalDateTime.now().plusDays(2).withSecond(0).withNano(0)); return r;
    }

    @ParameterizedTest @ValueSource(strings={"transfers", "bill-payments"})
    void separateListAndFormOnlyExposeTheirOwnModule(String module) throws Exception {
        when(schedules.list(7L, type(module))).thenReturn(List.of(view(module, ScheduleStatus.SCHEDULED)));
        mvc.perform(get(base(module)).with(user(customer))).andExpect(status().isOk())
                .andExpect(content().string(containsString(base(module)+"/SCHTEST/edit")))
                .andExpect(content().string(containsString(base(module)+"/SCHTEST/cancel")));
        verify(schedules).list(7L, type(module));
        mvc.perform(get(base(module)+"/new").with(user(customer))).andExpect(status().isOk())
                .andExpect(content().string(containsString(module.equals("transfers") ? "Recipient account number" : "Service provider")))
                .andExpect(content().string(not(containsString(module.equals("transfers") ? "Service provider" : "Recipient account number"))))
                .andExpect(content().string(not(containsString("name=\"paymentType\""))));
        if (module.equals("transfers")) verifyNoInteractions(bills); else verifyNoInteractions(transfers);
    }

    @ParameterizedTest @ValueSource(strings={"transfers", "bill-payments"})
    void createPinsTypeToRouteEvenWhenClientSubmitsAnotherType(String module) throws Exception {
        when(schedules.create(eq(7L), eq("customer"), any())).thenReturn(view(module, ScheduleStatus.SCHEDULED));
        mvc.perform(post(base(module)).with(user(customer)).with(csrf())
                .param("paymentType", module.equals("transfers") ? "BILL_PAYMENT" : "FUND_TRANSFER")
                .param("sourceAccountNumber","111122223333").param("amount","100")
                .param("recurrence","ONCE").param("nextExecutionAt","2099-01-10T10:00").param("transactionPin","2468"))
                .andExpect(redirectedUrl(base(module)+"/SCHTEST"));
        verify(schedules).create(eq(7L), eq("customer"), argThat(r -> r.getPaymentType()==type(module)));
    }

    @ParameterizedTest @ValueSource(strings={"transfers", "bill-payments"})
    void createErrorsRetainFieldsAndDoNotEchoPin(String module) throws Exception {
        mvc.perform(post(base(module)).with(user(customer)).with(csrf())
                .param("amount","-2").param("nextExecutionAt","2099-01-10T10:00").param("transactionPin","2468"))
                .andExpect(status().isOk()).andExpect(model().attributeHasFieldErrors("scheduleRequest","amount","sourceAccountNumber"))
                .andExpect(content().string(not(containsString("value=\"2468\""))))
                .andExpect(content().string(containsString("2099-01-10T10:00")));
        verify(schedules, never()).create(any(),any(),any());
    }

    @ParameterizedTest @ValueSource(strings={"transfers", "bill-payments"})
    void editAndCancelStayInModuleAndShowValidationErrors(String module) throws Exception {
        when(schedules.get(7L,"SCHTEST")).thenReturn(view(module, ScheduleStatus.SCHEDULED));
        when(schedules.getUpdateRequest(7L,"SCHTEST")).thenReturn(updateRequest());
        mvc.perform(get(base(module)+"/SCHTEST/edit").with(user(customer))).andExpect(status().isOk())
                .andExpect(content().string(containsString("Save changes")));
        mvc.perform(post(base(module)+"/SCHTEST/edit").with(user(customer)).with(csrf())
                .param("amount","0").param("recurrence","ONCE").param("nextExecutionAt","2099-01-10T10:00"))
                .andExpect(status().isOk()).andExpect(model().attributeHasFieldErrors("updateRequest","amount"));
        verify(schedules, never()).update(any(),any(),any(),any());
        mvc.perform(post(base(module)+"/SCHTEST/edit").with(user(customer)).with(csrf())
                .param("amount","200").param("recurrence","ONCE").param("nextExecutionAt","2099-01-10T10:00"))
                .andExpect(redirectedUrl(base(module)+"/SCHTEST"));
        verify(schedules).update(eq(7L),eq("customer"),eq("SCHTEST"),any());
		mvc.perform(post(base(module)+"/SCHTEST/cancel").with(user(customer)).with(csrf()))
				.andExpect(redirectedUrl(base(module)));
        verify(schedules).cancel(7L,"customer","SCHTEST");
    }

    @Test void wrongModuleAndOtherCustomerCannotReadOrMutateSchedule() throws Exception {
        when(schedules.get(7L,"SCHTEST")).thenReturn(view("bill-payments", ScheduleStatus.SCHEDULED));
        when(schedules.get(7L,"OTHER")).thenThrow(new ScheduledPaymentException("Scheduled payment not found."));
        for (String ref : List.of("SCHTEST","OTHER")) {
            mvc.perform(get(base("transfers")+"/"+ref).with(user(customer))).andExpect(status().isNotFound());
            mvc.perform(get(base("transfers")+"/"+ref+"/edit").with(user(customer))).andExpect(status().isNotFound());
            mvc.perform(post(base("transfers")+"/"+ref+"/edit").with(user(customer)).with(csrf()))
                    .andExpect(status().isNotFound());
            mvc.perform(post(base("transfers")+"/"+ref+"/cancel").with(user(customer)).with(csrf()))
                    .andExpect(status().isNotFound());
        }
        verify(schedules,never()).update(any(),any(),any(),any());
        verify(schedules,never()).cancel(any(),any(),any());
    }

    @Test void terminalScheduleHasNoEditOrCancelControls() throws Exception {
        when(schedules.get(7L,"SCHTEST")).thenReturn(view("transfers", ScheduleStatus.COMPLETED));
        mvc.perform(get(base("transfers")+"/SCHTEST").with(user(customer))).andExpect(status().isOk())
                .andExpect(content().string(not(containsString(">Edit schedule</a>"))))
                .andExpect(content().string(not(containsString(">Cancel schedule</button>"))));
    }

    @Test void oldLinksRedirectToCorrectModule() throws Exception {
        when(schedules.get(7L,"SCHTEST")).thenReturn(view("bill-payments", ScheduleStatus.SCHEDULED));
        mvc.perform(get("/customer/schedules").with(user(customer))).andExpect(status().isOk());
        mvc.perform(get("/customer/schedules/SCHTEST/edit").with(user(customer)))
                .andExpect(redirectedUrl(base("bill-payments")+"/SCHTEST/edit"));
    }

    @Test void csrfAndCustomerAuthenticationAreRequired() throws Exception {
        mvc.perform(get(base("transfers"))).andExpect(status().is3xxRedirection());
        mvc.perform(get(base("transfers")).with(user("staff").roles("BANK_STAFF"))).andExpect(status().isForbidden());
        mvc.perform(post(base("transfers")+"/SCHTEST/cancel").with(user(customer))).andExpect(status().isForbidden());
        verifyNoInteractions(schedules);
    }
}
