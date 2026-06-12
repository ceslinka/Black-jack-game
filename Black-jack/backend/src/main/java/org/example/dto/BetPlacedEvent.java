package org.example.dto;

import java.util.UUID;

public record BetPlacedEvent(String type, UUID tableId, UUID userId, int amount, int seatIndex) {}
