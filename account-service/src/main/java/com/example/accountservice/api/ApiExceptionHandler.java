package com.example.accountservice.api;

import com.example.accountservice.exception.AccountAlreadyExistsException;
import com.example.accountservice.exception.AccountNotFoundException;
import com.example.accountservice.exception.InsufficientFundsException;
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
    logRejectedRequest("account_exists", "ACCOUNT_EXISTS");
    return ResponseEntity.status(HttpStatus.CONFLICT)
        .body(new ApiError("ACCOUNT_EXISTS", ex.getMessage()));
  }

  @ExceptionHandler(AccountNotFoundException.class)
  public ResponseEntity<ApiError> handleNotFound(AccountNotFoundException ex) {
    logRejectedRequest("account_not_found", "ACCOUNT_NOT_FOUND");
    return ResponseEntity.status(HttpStatus.NOT_FOUND)
        .body(new ApiError("ACCOUNT_NOT_FOUND", ex.getMessage()));
  }

  @ExceptionHandler(InsufficientFundsException.class)
  public ResponseEntity<ApiError> handleInsufficient(InsufficientFundsException ex) {
    logRejectedRequest("insufficient_funds", "INSUFFICIENT_FUNDS");
    return ResponseEntity.status(HttpStatus.CONFLICT)
        .body(new ApiError("INSUFFICIENT_FUNDS", ex.getMessage()));
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
        .log("Account request rejected");
  }
}
