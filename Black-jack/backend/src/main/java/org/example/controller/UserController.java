package org.example.controller;

import org.example.dto.UserProfileResponse;
import org.example.dto.WalletResponse;
import org.example.security.AuthSupport;
import org.example.service.WalletService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class UserController {

    private final AuthSupport authSupport;
    private final WalletService walletService;

    public UserController(AuthSupport authSupport, WalletService walletService) {
        this.authSupport = authSupport;
        this.walletService = walletService;
    }

    @GetMapping("/users/me")
    public UserProfileResponse me() {
        return walletService.getProfile(authSupport.currentUser());
    }

    @GetMapping("/wallet")
    public WalletResponse wallet() {
        return walletService.getWallet(authSupport.currentUser());
    }
}
