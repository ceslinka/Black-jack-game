package org.example.dto;

import java.util.UUID;

public record AuthResponse(UUID userId, String token, long expiresIn) {}
