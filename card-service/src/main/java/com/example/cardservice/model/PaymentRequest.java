package com.example.cardservice.model;

import java.math.BigDecimal;

public record PaymentRequest(BigDecimal amount, String merchant) {}
