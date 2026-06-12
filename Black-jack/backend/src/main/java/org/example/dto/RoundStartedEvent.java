package org.example.dto;

import java.util.UUID;

public record RoundStartedEvent(String type, UUID tableId, UUID roundId) {}
