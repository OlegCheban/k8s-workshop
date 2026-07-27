package com.example.accountservice;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ExtendWith(OutputCaptureExtension.class)
class AccountControllerTest {
  @Autowired
  private MockMvc mockMvc;

  @Test
  void createAndFetchAccount(CapturedOutput output) throws Exception {
    mockMvc.perform(post("/api/accounts")
            .header("X-Request-Id", "account-test-request")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"accountId\":\"A-123\"}"))
        .andExpect(status().isCreated())
        .andExpect(header().string("X-Request-Id", "account-test-request"))
        .andExpect(jsonPath("$.accountId").value("A-123"));

    mockMvc.perform(get("/api/accounts/A-123"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.balance").value(0));

    assertThat(output.getOut())
        .contains("\"traceId\":\"")
        .contains("\"spanId\":\"")
        .contains("\"requestId\":\"account-test-request\"")
        .contains("\"event\":\"account_created\"")
        .doesNotContain("\"accountId\"")
        .doesNotContain("\"balance\"");
  }
}
