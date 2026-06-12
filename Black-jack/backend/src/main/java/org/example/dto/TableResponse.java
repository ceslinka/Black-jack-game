package org.example.dto;

import java.util.UUID;
import org.example.entity.TableStatus;

public record TableResponse(
        UUID id,
        String name,
        int maxPlayers,
        int currentPlayers,
        TableStatus status,
        int minBet,
        int maxBet) {}
