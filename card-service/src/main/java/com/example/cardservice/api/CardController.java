package com.example.cardservice.api;

import com.example.cardservice.logging.RequestLoggingSupport;
import com.example.cardservice.model.AmountRequest;
import com.example.cardservice.model.Card;
import com.example.cardservice.model.CreateCardRequest;
import com.example.cardservice.model.PaymentRequest;
import com.example.cardservice.service.CardService;
import java.util.Map;
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
    logger.info("card_created cardId={} accountId={}", card.cardId(), card.accountId());
    RequestLoggingSupport.logToStderr(
        "card_created",
        Map.of("cardId", card.cardId(), "accountId", card.accountId()));
    return ResponseEntity.status(HttpStatus.CREATED).body(card);
  }

  @GetMapping("/{cardId}")
  public Card get(@PathVariable String cardId) {
    Card card = cardService.getCard(cardId);
    logger.info("card_retrieved cardId={} balance={}", card.cardId(), card.availableBalance());
    RequestLoggingSupport.logToStderr(
        "card_retrieved",
        Map.of("cardId", card.cardId(), "balance", card.availableBalance().toPlainString()));
    return card;
  }

  @PostMapping("/{cardId}/add-money")
  public Card addMoney(@PathVariable String cardId, @RequestBody AmountRequest request) {
    Card card = cardService.addMoney(cardId, request.amount());
    logger.info("card_add_money cardId={} amount={} balance={}",
        card.cardId(), request.amount(), card.availableBalance());
    RequestLoggingSupport.logToStderr(
        "card_add_money",
        Map.of(
            "cardId", card.cardId(),
            "amount", request.amount().toPlainString(),
            "balance", card.availableBalance().toPlainString()));
    return card;
  }

  @PostMapping("/{cardId}/pay")
  public Card pay(@PathVariable String cardId, @RequestBody PaymentRequest request) {
    Card card = cardService.pay(cardId, request.amount(), request.merchant());
    logger.info("card_pay cardId={} amount={} merchant={} balance={}",
        card.cardId(), request.amount(), request.merchant(), card.availableBalance());
    RequestLoggingSupport.logToStderr(
        "card_pay",
        Map.of(
            "cardId", card.cardId(),
            "amount", request.amount().toPlainString(),
            "merchant", request.merchant(),
            "balance", card.availableBalance().toPlainString()));
    return card;
  }
}
