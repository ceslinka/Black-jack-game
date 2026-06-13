package org.example.dto;

import java.util.UUID;

public record SeatState(int index, UUID userId, String username) {}
