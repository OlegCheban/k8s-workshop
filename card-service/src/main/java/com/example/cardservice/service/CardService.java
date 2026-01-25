package com.example.cardservice.service;

import com.example.cardservice.exception.CardAlreadyExistsException;
import com.example.cardservice.exception.CardNotFoundException;
import com.example.cardservice.model.AccountResponse;
import com.example.cardservice.model.Card;
import com.example.cardservice.model.CardTransaction;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Service;

@Service
public class CardService {
  private final AccountClient accountClient;
  private final Map<String, CardState> cards = new ConcurrentHashMap<>();

  public CardService(AccountClient accountClient) {
    this.accountClient = accountClient;
  }

  public Card createCard(String cardId, String accountId) {
    if (cards.containsKey(cardId)) {
      throw new CardAlreadyExistsException(cardId);
    }
    CardState state = new CardState(accountId, BigDecimal.ZERO, new ArrayList<>());
    cards.put(cardId, state);
    return toCard(cardId, state);
  }

  public Card getCard(String cardId) {
    CardState state = cards.get(cardId);
    if (state == null) {
      throw new CardNotFoundException(cardId);
    }
    return toCard(cardId, state);
  }

  public Card addMoney(String cardId, BigDecimal amount) {
    if (amount == null || amount.signum() <= 0) {
      throw new IllegalArgumentException("Amount must be positive");
    }
    CardState state = getState(cardId);
    AccountResponse response = accountClient.deposit(state.accountId(), amount);
    state.balance = response.balance();
    state.transactions.add(new CardTransaction(cardId, amount, "ADD_MONEY", Instant.now()));
    return toCard(cardId, state);
  }

  public Card pay(String cardId, BigDecimal amount, String merchant) {
    if (amount == null || amount.signum() <= 0) {
      throw new IllegalArgumentException("Amount must be positive");
    }
    CardState state = getState(cardId);
    AccountResponse response = accountClient.withdraw(state.accountId(), amount);
    state.balance = response.balance();
    state.transactions.add(new CardTransaction(cardId, amount, merchant, Instant.now()));
    return toCard(cardId, state);
  }

  private CardState getState(String cardId) {
    CardState state = cards.get(cardId);
    if (state == null) {
      throw new CardNotFoundException(cardId);
    }
    return state;
  }

  private Card toCard(String cardId, CardState state) {
    return new Card(cardId, state.accountId(), state.balance, List.copyOf(state.transactions));
  }

  private static class CardState {
    private final String accountId;
    private BigDecimal balance;
    private final List<CardTransaction> transactions;

    private CardState(String accountId, BigDecimal balance, List<CardTransaction> transactions) {
      this.accountId = accountId;
      this.balance = balance;
      this.transactions = transactions;
    }

    private String accountId() {
      return accountId;
    }
  }
}
