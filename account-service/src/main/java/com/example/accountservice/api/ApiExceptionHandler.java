package com.example.accountservice.api;

import com.example.accountservice.exception.AccountAlreadyExistsException;
import com.example.accountservice.exception.AccountNotFoundException;
import com.example.accountservice.exception.InsufficientFundsException;
import com.example.accountservice.logging.RequestLoggingSupport;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ApiExceptionHandler {
  private static final Logger logger = LoggerFactory.getLogger(ApiExceptionHandler.class);

  @ExceptionHandler(AccountAlreadyExistsException.class)
  public ResponseEntity<ApiError> handleAccountExists(AccountAlreadyExistsException ex) {
    logger.warn("account_exists error={}", ex.getMessage());
    RequestLoggingSupport.logToStderr("account_exists", Map.of("message", ex.getMessage()));
    return ResponseEntity.status(HttpStatus.CONFLICT)
        .body(new ApiError("ACCOUNT_EXISTS", ex.getMessage()));
  }

  @ExceptionHandler(AccountNotFoundException.class)
  public ResponseEntity<ApiError> handleNotFound(AccountNotFoundException ex) {
    logger.warn("account_not_found error={}", ex.getMessage());
    RequestLoggingSupport.logToStderr("account_not_found", Map.of("message", ex.getMessage()));
    return ResponseEntity.status(HttpStatus.NOT_FOUND)
        .body(new ApiError("ACCOUNT_NOT_FOUND", ex.getMessage()));
  }

  @ExceptionHandler(InsufficientFundsException.class)
  public ResponseEntity<ApiError> handleInsufficient(InsufficientFundsException ex) {
    logger.warn("insufficient_funds error={}", ex.getMessage());
    RequestLoggingSupport.logToStderr("insufficient_funds", Map.of("message", ex.getMessage()));
    return ResponseEntity.status(HttpStatus.CONFLICT)
        .body(new ApiError("INSUFFICIENT_FUNDS", ex.getMessage()));
  }

  @ExceptionHandler(IllegalArgumentException.class)
  public ResponseEntity<ApiError> handleBadRequest(IllegalArgumentException ex) {
    logger.warn("bad_request error={}", ex.getMessage());
    RequestLoggingSupport.logToStderr("bad_request", Map.of("message", ex.getMessage()));
    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
        .body(new ApiError("BAD_REQUEST", ex.getMessage()));
  }
}
