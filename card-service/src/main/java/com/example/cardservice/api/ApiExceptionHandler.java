package com.example.cardservice.api;

import com.example.cardservice.exception.CardAlreadyExistsException;
import com.example.cardservice.exception.CardNotFoundException;
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
    logRejectedRequest("card_exists", "CARD_EXISTS");
    return ResponseEntity.status(HttpStatus.CONFLICT)
        .body(new ApiError("CARD_EXISTS", ex.getMessage()));
  }

  @ExceptionHandler(CardNotFoundException.class)
  public ResponseEntity<ApiError> handleNotFound(CardNotFoundException ex) {
    logRejectedRequest("card_not_found", "CARD_NOT_FOUND");
    return ResponseEntity.status(HttpStatus.NOT_FOUND)
        .body(new ApiError("CARD_NOT_FOUND", ex.getMessage()));
  }

  @ExceptionHandler(HttpClientErrorException.class)
  public ResponseEntity<ApiError> handleClientError(HttpClientErrorException ex) {
    logger.atWarn()
        .addKeyValue("event", "account_service_error")
        .addKeyValue("outcome", "failure")
        .addKeyValue("downstreamService", "account-service")
        .addKeyValue("httpStatus", ex.getStatusCode().value())
        .log("Account service rejected the request");
    return ResponseEntity.status(ex.getStatusCode())
        .body(new ApiError("ACCOUNT_SERVICE_ERROR", ex.getResponseBodyAsString()));
  }

  @ExceptionHandler(IllegalArgumentException.class)
  public ResponseEntity<ApiError> handleBadRequest(IllegalArgumentException ex) {
    logRejectedRequest("bad_request", "BAD_REQUEST");
    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
        .body(new ApiError("BAD_REQUEST", ex.getMessage()));
  }

  private void logRejectedRequest(String event, String errorCode) {
    logger.atWarn()
        .addKeyValue("event", event)
        .addKeyValue("outcome", "rejected")
        .addKeyValue("errorCode", errorCode)
        .log("Card request rejected");
  }
}
