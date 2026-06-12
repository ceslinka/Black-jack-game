package org.example.dto;

import java.util.List;
import java.util.UUID;

public record DealerTurnEvent(String type, UUID tableId, List<String> cards) {}
