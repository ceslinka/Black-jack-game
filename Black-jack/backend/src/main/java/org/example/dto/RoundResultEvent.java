package org.example.dto;

import java.util.List;
import java.util.UUID;

public record RoundResultEvent(String type, UUID tableId, UUID roundId, List<PlayerResult> results) {}
