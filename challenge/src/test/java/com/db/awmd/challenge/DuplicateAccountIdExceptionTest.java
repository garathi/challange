package com.db.awmd.challenge;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.context.WebApplicationContext;

import com.db.awmd.challenge.service.AccountsService;

@RunWith(SpringRunner.class)
@SpringBootTest
@WebAppConfiguration
public class DuplicateAccountIdExceptionTest {

	  private MockMvc mockMvc;

	  @Autowired
	  private AccountsService accountsService;

	  @Autowired
	  private WebApplicationContext webApplicationContext;

	  @Before
	  public void prepareMockMvc() {
		  this.mockMvc = webAppContextSetup(this.webApplicationContext).build();

	    // Reset the existing accounts before each test.
	    accountsService.getAccountsRepository().clearAccounts();
	  }
	  
	  @Test
	  public void createAccount() throws Exception {
		    this.mockMvc.perform(post("/v1/accounts").contentType(MediaType.APPLICATION_JSON)
		    	      .content("{\"accountId\":\"Id-123\",\"balance\":1000}")).andExpect(status().isCreated());

		    	    this.mockMvc.perform(post("/v1/accounts").contentType(MediaType.APPLICATION_JSON)
		    	      .content("{\"accountId\":\"Id-123\",\"balance\":1000}")).andExpect(status().isBadRequest());
	    
	  }
}
