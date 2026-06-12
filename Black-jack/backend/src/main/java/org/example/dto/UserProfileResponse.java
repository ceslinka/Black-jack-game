package org.example.dto;

import java.util.UUID;
import org.example.entity.UserRole;

public record UserProfileResponse(
        UUID id,
        String username,
        String email,
        UserRole role,
        int balance) {}
