package com.example.cardservice.model;

import java.math.BigDecimal;

public record AccountResponse(String accountId, BigDecimal balance) {}
