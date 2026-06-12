package org.example.controller;

import jakarta.validation.Valid;
import java.util.UUID;
import org.example.dto.BetRequest;
import org.example.dto.BetResponse;
import org.example.security.AuthSupport;
import org.example.service.GameRoundService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/tables/{tableId}/bets")
public class BetController {

    private final AuthSupport authSupport;
    private final GameRoundService gameRoundService;

    public BetController(AuthSupport authSupport, GameRoundService gameRoundService) {
        this.authSupport = authSupport;
        this.gameRoundService = gameRoundService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public BetResponse placeBet(@PathVariable UUID tableId, @Valid @RequestBody BetRequest request) {
        return gameRoundService.placeBet(authSupport.currentUser(), tableId, request.amount());
    }
}
