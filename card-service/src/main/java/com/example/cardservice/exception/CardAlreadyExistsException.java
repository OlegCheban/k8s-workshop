package com.example.cardservice.exception;

public class CardAlreadyExistsException extends RuntimeException {
  public CardAlreadyExistsException(String cardId) {
    super("Card already exists: " + cardId);
  }
}
