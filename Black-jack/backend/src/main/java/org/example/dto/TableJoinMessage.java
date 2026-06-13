package org.example.dto;

import java.util.UUID;

public record TableJoinMessage(UUID tableId, int seatIndex) {}
