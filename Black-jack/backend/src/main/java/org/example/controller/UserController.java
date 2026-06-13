package org.example.controller;

import org.example.dto.CurrentSeatResponse;
import org.example.dto.GameHistoryListResponse;
import org.example.dto.UserProfileResponse;
import org.example.dto.WalletResponse;
import org.example.security.AuthSupport;
import org.example.service.GameHistoryService;
import org.example.service.TableService;
import org.example.service.WalletService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class UserController {

    private final AuthSupport authSupport;
    private final WalletService walletService;
    private final TableService tableService;
    private final GameHistoryService gameHistoryService;

    public UserController(
            AuthSupport authSupport,
            WalletService walletService,
            TableService tableService,
            GameHistoryService gameHistoryService) {
        this.authSupport = authSupport;
        this.walletService = walletService;
        this.tableService = tableService;
        this.gameHistoryService = gameHistoryService;
    }

    @GetMapping("/users/me")
    public UserProfileResponse me() {
        return walletService.getProfile(authSupport.currentUser());
    }

    @GetMapping("/wallet")
    public WalletResponse wallet() {
        return walletService.getWallet(authSupport.currentUser());
    }

    @GetMapping("/users/me/seat")
    public ResponseEntity<CurrentSeatResponse> mySeat() {
        CurrentSeatResponse seat = tableService.getCurrentSeat(authSupport.currentUser());
        if (seat == null) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(seat);
    }

    @GetMapping("/users/me/game-history")
    public GameHistoryListResponse gameHistory() {
        return gameHistoryService.getPlayerHistory(authSupport.currentUser());
    }
}
