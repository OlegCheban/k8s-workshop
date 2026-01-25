package com.example.accountservice.service;

import com.example.accountservice.exception.AccountAlreadyExistsException;
import com.example.accountservice.exception.AccountNotFoundException;
import com.example.accountservice.exception.InsufficientFundsException;
import com.example.accountservice.model.Account;
import java.math.BigDecimal;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Service;

@Service
public class AccountService {
  private final Map<String, BigDecimal> balances = new ConcurrentHashMap<>();

  public Account createAccount(String accountId) {
    if (balances.containsKey(accountId)) {
      throw new AccountAlreadyExistsException(accountId);
    }
    balances.put(accountId, BigDecimal.ZERO);
    return new Account(accountId, BigDecimal.ZERO);
  }

  public Account getAccount(String accountId) {
    BigDecimal balance = balances.get(accountId);
    if (balance == null) {
      throw new AccountNotFoundException(accountId);
    }
    return new Account(accountId, balance);
  }

  public Account deposit(String accountId, BigDecimal amount) {
    if (amount == null || amount.signum() <= 0) {
      throw new IllegalArgumentException("Amount must be positive");
    }
    BigDecimal updated = balances.compute(accountId, (key, balance) -> {
      if (balance == null) {
        throw new AccountNotFoundException(accountId);
      }
      return balance.add(amount);
    });
    return new Account(accountId, updated);
  }

  public Account withdraw(String accountId, BigDecimal amount) {
    if (amount == null || amount.signum() <= 0) {
      throw new IllegalArgumentException("Amount must be positive");
    }
    BigDecimal updated = balances.compute(accountId, (key, balance) -> {
      if (balance == null) {
        throw new AccountNotFoundException(accountId);
      }
      if (balance.compareTo(amount) < 0) {
        throw new InsufficientFundsException(accountId);
      }
      return balance.subtract(amount);
    });
    return new Account(accountId, updated);
  }
}
