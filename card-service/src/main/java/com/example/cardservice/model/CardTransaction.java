package com.example.cardservice.model;

import java.math.BigDecimal;
import java.time.Instant;

public record CardTransaction(String cardId, BigDecimal amount, String merchant, Instant timestamp) {}
