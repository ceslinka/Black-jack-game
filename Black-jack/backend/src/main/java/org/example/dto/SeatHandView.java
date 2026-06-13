package org.example.dto;

import java.util.List;
import java.util.UUID;

public record SeatHandView(
        int seatIndex,
        UUID userId,
        String username,
        UUID handId,
        List<String> cards,
        Integer handValue,
        String handStatus,
        Integer bet) {}
