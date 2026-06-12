package org.example.dto;

import java.util.UUID;

public record PlayerResult(UUID userId, int handValue, int dealerValue, String outcome, int payout) {}
