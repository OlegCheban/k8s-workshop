package com.example.cardservice.service;

import com.example.cardservice.model.AccountResponse;
import com.example.cardservice.model.AmountRequest;
import java.math.BigDecimal;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class AccountClient {
  private final RestClient restClient;

  public AccountClient(RestClient restClient) {
    this.restClient = restClient;
  }

  public AccountResponse getAccount(String accountId) {
    return restClient.get()
        .uri("/api/accounts/{accountId}", accountId)
        .retrieve()
        .body(AccountResponse.class);
  }

  public AccountResponse deposit(String accountId, BigDecimal amount) {
    return restClient.post()
        .uri("/api/accounts/{accountId}/deposit", accountId)
        .body(new AmountRequest(amount))
        .retrieve()
        .body(AccountResponse.class);
  }

  public AccountResponse withdraw(String accountId, BigDecimal amount) {
    return restClient.post()
        .uri("/api/accounts/{accountId}/withdraw", accountId)
        .body(new AmountRequest(amount))
        .retrieve()
        .body(AccountResponse.class);
  }
}
