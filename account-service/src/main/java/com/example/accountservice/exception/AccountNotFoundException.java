package com.example.accountservice.exception;

public class AccountNotFoundException extends RuntimeException {
  public AccountNotFoundException(String accountId) {
    super("Account not found: " + accountId);
  }
}
