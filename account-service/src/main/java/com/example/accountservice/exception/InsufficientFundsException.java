package com.example.accountservice.exception;

public class InsufficientFundsException extends RuntimeException {
  public InsufficientFundsException(String accountId) {
    super("Insufficient funds for account: " + accountId);
  }
}
