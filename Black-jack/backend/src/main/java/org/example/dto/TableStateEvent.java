package org.example.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.example.entity.TableStatus;

public record TableStateEvent(
        String type,
        UUID tableId,
        TableStatus status,
        List<SeatState> seats,
        int occupiedPlayerSeats,
        int maxPlayers,
        int minBet,
        int maxBet,
        Instant intermissionEndsAt) {}
