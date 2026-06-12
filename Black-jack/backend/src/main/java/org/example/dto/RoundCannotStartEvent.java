package org.example.dto;

import java.util.UUID;

public record RoundCannotStartEvent(String type, UUID tableId, String reason) {}
