package com.example.accountservice.api;

import com.example.accountservice.model.Account;
import com.example.accountservice.model.AmountRequest;
import com.example.accountservice.model.CreateAccountRequest;
import com.example.accountservice.service.AccountService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/accounts")
public class AccountController {
  private static final Logger logger = LoggerFactory.getLogger(AccountController.class);

  private final AccountService accountService;

  public AccountController(AccountService accountService) {
    this.accountService = accountService;
  }

  @PostMapping
  public ResponseEntity<Account> create(@RequestBody CreateAccountRequest request) {
    Account account = accountService.createAccount(request.accountId());
    logger.atInfo()
        .addKeyValue("event", "account_created")
        .addKeyValue("outcome", "success")
        .log("Account created");
    return ResponseEntity.status(HttpStatus.CREATED).body(account);
  }

  @GetMapping("/{accountId}")
  public Account get(@PathVariable String accountId) {
    Account account = accountService.getAccount(accountId);
    logger.atInfo()
        .addKeyValue("event", "account_retrieved")
        .addKeyValue("outcome", "success")
        .log("Account retrieved");
    return account;
  }

  @PostMapping("/{accountId}/deposit")
  public Account deposit(@PathVariable String accountId, @RequestBody AmountRequest request) {
    Account account = accountService.deposit(accountId, request.amount());
    logger.atInfo()
        .addKeyValue("event", "account_deposit")
        .addKeyValue("outcome", "success")
        .log("Account deposit completed");
    return account;
  }

  @PostMapping("/{accountId}/withdraw")
  public Account withdraw(@PathVariable String accountId, @RequestBody AmountRequest request) {
    Account account = accountService.withdraw(accountId, request.amount());
    logger.atInfo()
        .addKeyValue("event", "account_withdraw")
        .addKeyValue("outcome", "success")
        .log("Account withdrawal completed");
    return account;
  }
}
