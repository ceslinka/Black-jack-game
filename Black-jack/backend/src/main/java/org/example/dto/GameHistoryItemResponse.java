package org.example.dto;

import java.time.Instant;
import java.util.UUID;

public record GameHistoryItemResponse(
        UUID roundId,
        UUID tableId,
        String tableName,
        String outcome,
        int betAmount,
        int payout,
        int balanceBefore,
        int balanceAfter,
        int netChange,
        Instant settledAt) {}
