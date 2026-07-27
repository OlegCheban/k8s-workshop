package com.example.cardservice.config;

import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class AccountClientConfig {
  @Bean
  public RestClient accountServiceRestClient(
      RestClient.Builder builder,
      @Value("${account-service.url}") String baseUrl) {
    return builder
        .baseUrl(baseUrl)
        .requestInterceptor((request, body, execution) -> {
          String requestId = MDC.get("requestId");
          if (requestId != null) {
            request.getHeaders().set("X-Request-Id", requestId);
          }
          return execution.execute(request, body);
        })
        .build();
  }
}
