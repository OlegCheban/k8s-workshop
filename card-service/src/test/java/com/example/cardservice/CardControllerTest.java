package com.example.cardservice;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.cardservice.model.AccountResponse;
import com.example.cardservice.service.AccountClient;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ExtendWith(OutputCaptureExtension.class)
class CardControllerTest {
  @Autowired
  private MockMvc mockMvc;

  @MockBean
  private AccountClient accountClient;

  @Test
  void createAndFetchCard(CapturedOutput output) throws Exception {
    mockMvc.perform(post("/api/cards")
            .header("X-Request-Id", "card-test-request")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"cardId\":\"C-123\",\"accountId\":\"A-123\"}"))
        .andExpect(status().isCreated())
        .andExpect(header().string("X-Request-Id", "card-test-request"))
        .andExpect(jsonPath("$.cardId").value("C-123"));

    mockMvc.perform(get("/api/cards/C-123"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.availableBalance").value(0));

    assertThat(output.getOut())
        .contains("\"traceId\":\"")
        .contains("\"spanId\":\"")
        .contains("\"requestId\":\"card-test-request\"")
        .contains("\"event\":\"card_created\"")
        .doesNotContain("\"accountId\"")
        .doesNotContain("\"cardId\"")
        .doesNotContain("\"availableBalance\"");
  }

  @Test
  void addMoneyCallsAccountService() throws Exception {
    when(accountClient.deposit(eq("A-123"), any(BigDecimal.class)))
        .thenReturn(new AccountResponse("A-123", new BigDecimal("100.00")));

    mockMvc.perform(post("/api/cards")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"cardId\":\"C-999\",\"accountId\":\"A-123\"}"))
        .andExpect(status().isCreated());

    mockMvc.perform(post("/api/cards/C-999/add-money")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"amount\":100}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.availableBalance").value(100.00));
  }
}
