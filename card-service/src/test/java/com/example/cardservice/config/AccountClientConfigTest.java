package com.example.cardservice.config;

import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class AccountClientConfigTest {
  private static final String REQUEST_ID = "client-test-request";

  @AfterEach
  void clearMdc() {
    MDC.clear();
  }

  @Test
  void propagatesRequestIdToAccountService() {
    RestClient.Builder builder = RestClient.builder();
    MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
    RestClient restClient =
        new AccountClientConfig().accountServiceRestClient(builder, "http://account-service");

    server.expect(once(), requestTo("http://account-service/probe"))
        .andExpect(header("X-Request-Id", REQUEST_ID))
        .andRespond(withSuccess());

    MDC.put("requestId", REQUEST_ID);
    restClient.get().uri("/probe").retrieve().toBodilessEntity();

    server.verify();
  }
}
