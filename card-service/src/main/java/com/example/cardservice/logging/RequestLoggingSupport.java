package com.example.cardservice.logging;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

public final class RequestLoggingSupport {
  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

  private RequestLoggingSupport() {}

  public static void logToStderr(String event, Map<String, Object> fields) {
    Map<String, Object> payload = new LinkedHashMap<>();
    payload.put("timestamp", Instant.now().toString());
    payload.put("event", event);
    payload.putAll(fields);
    try {
      System.err.println(OBJECT_MAPPER.writeValueAsString(payload));
    } catch (JsonProcessingException ex) {
      System.err.println("{\"event\":\"logging_error\",\"message\":\"" + ex.getMessage() + "\"}");
    }
  }
}
