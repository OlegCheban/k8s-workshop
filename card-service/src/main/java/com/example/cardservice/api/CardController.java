package com.example.cardservice.api;

import com.example.cardservice.model.AmountRequest;
import com.example.cardservice.model.Card;
import com.example.cardservice.model.CreateCardRequest;
import com.example.cardservice.model.PaymentRequest;
import com.example.cardservice.service.CardService;
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
@RequestMapping("/api/cards")
public class CardController {
  private static final Logger logger = LoggerFactory.getLogger(CardController.class);

  private final CardService cardService;

  public CardController(CardService cardService) {
    this.cardService = cardService;
  }

  @PostMapping
  public ResponseEntity<Card> create(@RequestBody CreateCardRequest request) {
    Card card = cardService.createCard(request.cardId(), request.accountId());
    logger.atInfo()
        .addKeyValue("event", "card_created")
        .addKeyValue("outcome", "success")
        .log("Card created");
    return ResponseEntity.status(HttpStatus.CREATED).body(card);
  }

  @GetMapping("/{cardId}")
  public Card get(@PathVariable String cardId) {
    Card card = cardService.getCard(cardId);
    logger.atInfo()
        .addKeyValue("event", "card_retrieved")
        .addKeyValue("outcome", "success")
        .log("Card retrieved");
    return card;
  }

  @PostMapping("/{cardId}/add-money")
  public Card addMoney(@PathVariable String cardId, @RequestBody AmountRequest request) {
    Card card = cardService.addMoney(cardId, request.amount());
    logger.atInfo()
        .addKeyValue("event", "card_add_money")
        .addKeyValue("outcome", "success")
        .log("Money added to card");
    return card;
  }

  @PostMapping("/{cardId}/pay")
  public Card pay(@PathVariable String cardId, @RequestBody PaymentRequest request) {
    Card card = cardService.pay(cardId, request.amount(), request.merchant());
    logger.atInfo()
        .addKeyValue("event", "card_payment")
        .addKeyValue("outcome", "success")
        .log("Card payment completed");
    return card;
  }
}
