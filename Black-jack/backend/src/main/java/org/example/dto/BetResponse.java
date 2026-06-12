package org.example.dto;

import java.util.UUID;

public record BetResponse(UUID betId, int newBalance) {}
