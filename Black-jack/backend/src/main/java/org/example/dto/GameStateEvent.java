package org.example.dto;

import java.util.List;
import java.util.UUID;

public record GameStateEvent(
        String type,
        UUID tableId,
        UUID roundId,
        String phase,
        Integer currentSeat,
        List<SeatHandView> seats,
        List<String> dealerCards,
        boolean dealerHidden) {}
