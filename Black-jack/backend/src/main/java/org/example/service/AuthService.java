package org.example.service;

import org.example.config.WalletProperties;
import org.example.dto.AuthResponse;
import org.example.dto.LoginRequest;
import org.example.dto.RegisterRequest;
import org.example.entity.User;
import org.example.entity.UserRole;
import org.example.exception.ApiException;
import org.example.repository.UserRepository;
import org.example.security.JwtService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final WalletProperties walletProperties;

    public AuthService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            WalletProperties walletProperties) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.walletProperties = walletProperties;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request, UserRole role) {
        if (userRepository.existsByEmail(request.email())) {
            throw new ApiException("CONFLICT", "Email already exists");
        }
        if (userRepository.existsByUsername(request.username())) {
            throw new ApiException("CONFLICT", "Username already exists");
        }

        User user = new User();
        user.setEmail(request.email().toLowerCase());
        user.setUsername(request.username());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setRole(role);
        user.setBalance(walletProperties.initialBalance());
        userRepository.save(user);

        String token = jwtService.generateToken(user.getId(), user.getEmail());
        return new AuthResponse(user.getId(), token, jwtService.getExpirationSeconds());
    }

    public AuthResponse login(LoginRequest request) {
        User user = userRepository
                .findByEmail(request.email().toLowerCase())
                .orElseThrow(() -> new ApiException("UNAUTHORIZED", "Invalid credentials"));

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new ApiException("UNAUTHORIZED", "Invalid credentials");
        }

        String token = jwtService.generateToken(user.getId(), user.getEmail());
        return new AuthResponse(user.getId(), token, jwtService.getExpirationSeconds());
    }
}
