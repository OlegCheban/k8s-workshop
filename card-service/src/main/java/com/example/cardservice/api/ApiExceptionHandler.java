package com.example.cardservice.api;

import com.example.cardservice.exception.CardAlreadyExistsException;
import com.example.cardservice.exception.CardNotFoundException;
import com.example.cardservice.logging.RequestLoggingSupport;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.client.HttpClientErrorException;

@RestControllerAdvice
public class ApiExceptionHandler {
  private static final Logger logger = LoggerFactory.getLogger(ApiExceptionHandler.class);

  @ExceptionHandler(CardAlreadyExistsException.class)
  public ResponseEntity<ApiError> handleExists(CardAlreadyExistsException ex) {
    logger.warn("card_exists error={}", ex.getMessage());
    RequestLoggingSupport.logToStderr("card_exists", Map.of("message", ex.getMessage()));
    return ResponseEntity.status(HttpStatus.CONFLICT)
        .body(new ApiError("CARD_EXISTS", ex.getMessage()));
  }

  @ExceptionHandler(CardNotFoundException.class)
  public ResponseEntity<ApiError> handleNotFound(CardNotFoundException ex) {
    logger.warn("card_not_found error={}", ex.getMessage());
    RequestLoggingSupport.logToStderr("card_not_found", Map.of("message", ex.getMessage()));
    return ResponseEntity.status(HttpStatus.NOT_FOUND)
        .body(new ApiError("CARD_NOT_FOUND", ex.getMessage()));
  }

  @ExceptionHandler(HttpClientErrorException.class)
  public ResponseEntity<ApiError> handleClientError(HttpClientErrorException ex) {
    logger.warn("account_service_error status={} body={}", ex.getStatusCode(), ex.getResponseBodyAsString());
    RequestLoggingSupport.logToStderr(
        "account_service_error",
        Map.of("status", ex.getStatusCode().value(), "body", ex.getResponseBodyAsString()));
    return ResponseEntity.status(ex.getStatusCode())
        .body(new ApiError("ACCOUNT_SERVICE_ERROR", ex.getResponseBodyAsString()));
  }

  @ExceptionHandler(IllegalArgumentException.class)
  public ResponseEntity<ApiError> handleBadRequest(IllegalArgumentException ex) {
    logger.warn("bad_request error={}", ex.getMessage());
    RequestLoggingSupport.logToStderr("bad_request", Map.of("message", ex.getMessage()));
    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
        .body(new ApiError("BAD_REQUEST", ex.getMessage()));
  }
}
