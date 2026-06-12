package org.example.dto;

import java.util.List;
import java.util.UUID;

public record PlayerTurnEvent(
        String type, UUID tableId, int seatIndex, UUID handId, UUID userId, List<String> allowedActions) {}
