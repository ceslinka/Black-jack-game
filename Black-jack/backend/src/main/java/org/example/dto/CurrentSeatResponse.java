package org.example.dto;

import java.util.UUID;

public record CurrentSeatResponse(UUID tableId, String tableName, int seatIndex) {}
