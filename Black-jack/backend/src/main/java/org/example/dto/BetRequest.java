package org.example.dto;

import jakarta.validation.constraints.Min;

public record BetRequest(@Min(1) int amount) {}
