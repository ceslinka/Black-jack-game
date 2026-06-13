package org.example.dto;

import java.time.Instant;
import java.util.UUID;

public record RoundIntermissionEvent(
        String type,
        UUID tableId,
        Instant endsAt,
        int secondsRemaining,
        int minBet,
        int maxBet) {}
