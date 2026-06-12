package org.example.dto;

import java.util.List;
import java.util.UUID;
import org.example.entity.TableStatus;

public record TableStateEvent(
        String type,
        UUID tableId,
        TableStatus status,
        List<SeatState> seats,
        boolean dealerPresent,
        int occupiedPlayerSeats,
        int maxPlayers) {}
