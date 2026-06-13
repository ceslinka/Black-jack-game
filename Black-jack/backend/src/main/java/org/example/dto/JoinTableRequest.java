package org.example.dto;

import jakarta.validation.constraints.Min;

public record JoinTableRequest(@Min(0) int seatIndex) {}
