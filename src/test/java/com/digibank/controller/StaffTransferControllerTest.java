package com.digibank.controller;

import com.digibank.dto.transfer.StaffTransferView;
import com.digibank.entity.User;
import com.digibank.enums.*;
import com.digibank.repository.UserRepository;
import com.digibank.security.*;
import com.digibank.service.TransferReversalService;
import org.junit.jupiter.api.*;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.*;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.beans.factory.annotation.Autowired;
import java.lang.reflect.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(StaffTransferController.class)
@Import({SecurityConfig.class,StaffTransferControllerTest.Config.class})
class StaffTransferControllerTest {
	@Autowired MockMvc mvc; @Autowired FakeService service; CustomUserDetails staff; CustomUserDetails customer;
	@BeforeEach void setup(){service.reversed=null;staff=userDetails("staff",Role.BANK_STAFF,1L);customer=userDetails("customer",Role.CUSTOMER,2L);}
	@Test void staffCanViewTransferListAndDetails() throws Exception {
		mvc.perform(get("/staff/transfers").with(user(staff))).andExpect(status().isOk()).andExpect(view().name("staff/transfers/list")).andExpect(content().string(org.hamcrest.Matchers.containsString("Transfer operations")));
		mvc.perform(get("/staff/transfers/TRFTEST").with(user(staff))).andExpect(status().isOk()).andExpect(view().name("staff/transfers/details")).andExpect(content().string(org.hamcrest.Matchers.containsString("Reverse transfer")));
		mvc.perform(get("/staff/transfers").with(user(customer))).andExpect(status().isForbidden());
	}
	@Test void reversalRequiresCsrfAndUsesAuthenticatedStaff() throws Exception {
		mvc.perform(post("/staff/transfers/TRFTEST/reverse").with(user(staff)).param("reason","Confirmed duplicate transfer")).andExpect(status().isForbidden());
		mvc.perform(post("/staff/transfers/TRFTEST/reverse").with(user(staff)).with(csrf()).param("reason","Confirmed duplicate transfer")).andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/staff/transfers/TRFTEST"));
		assertEquals("staff:TRFTEST",service.reversed);
	}
	private CustomUserDetails userDetails(String name,Role role,Long id){User u=new User(name,name+"@test","hash");u.setRole(role);u.setEnabled(true);try{Method m=User.class.getSuperclass().getDeclaredMethod("setId",Long.class);m.setAccessible(true);m.invoke(u,id);}catch(Exception e){throw new IllegalStateException(e);}return new CustomUserDetails(u,CustomerStatus.ACTIVE);}
	@TestConfiguration static class Config {
		@Bean FakeService transferReversalService(){return new FakeService();}
		@Bean CustomAuthenticationSuccessHandler customAuthenticationSuccessHandler(){InvocationHandler h=(p,m,a)->m.getReturnType()==int.class?0:null;UserRepository r=(UserRepository)Proxy.newProxyInstance(UserRepository.class.getClassLoader(),new Class[]{UserRepository.class},h);return new CustomAuthenticationSuccessHandler(r);}
	}
	static class FakeService implements TransferReversalService {
		String reversed; public Page<StaffTransferView> search(String q,TransferStatus s,int p,int z){return new PageImpl<>(List.of(view()));}
		public StaffTransferView get(String r){return view();} public String reverse(String actor,String ref,String reason){reversed=actor+":"+ref;return "REVTEST";}
		private StaffTransferView view(){return new StaffTransferView("TRFTEST","CUS1","Sender Customer","********3333","Receiver Customer","DigiBank","********7777",new BigDecimal("100.00"),TransferType.INTERNAL,TransferStatus.COMPLETED,"Test",LocalDateTime.now(),null,null,null,null);}
	}
}
