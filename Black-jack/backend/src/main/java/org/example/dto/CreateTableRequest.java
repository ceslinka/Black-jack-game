package org.example.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateTableRequest(
        @NotBlank @Size(max = 100) String name,
        @NotNull @Min(2) @Max(6) Integer maxPlayers,
        @NotNull @Min(1) Integer minBet,
        @NotNull @Min(1) Integer maxBet) {}
