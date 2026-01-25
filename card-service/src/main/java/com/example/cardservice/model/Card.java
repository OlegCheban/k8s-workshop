package com.example.cardservice.model;

import java.math.BigDecimal;
import java.util.List;

public record Card(
    String cardId,
    String accountId,
    BigDecimal availableBalance,
    List<CardTransaction> transactions) {}
