package org.example.dto;

import java.util.List;
import java.util.UUID;

public record RoundSettledEvent(String type, UUID tableId, UUID roundId, List<PlayerResult> payouts) {}
