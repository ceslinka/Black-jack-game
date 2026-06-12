package org.example.service;

import org.example.dto.UserProfileResponse;
import org.example.dto.WalletResponse;
import org.example.entity.User;
import org.example.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class WalletService {

    private final UserRepository userRepository;

    public WalletService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public WalletResponse getWallet(User user) {
        User fresh = userRepository.findById(user.getId()).orElseThrow();
        return new WalletResponse(fresh.getBalance(), "chips");
    }

    @Transactional(readOnly = true)
    public UserProfileResponse getProfile(User user) {
        User fresh = userRepository.findById(user.getId()).orElseThrow();
        return new UserProfileResponse(
                fresh.getId(), fresh.getUsername(), fresh.getEmail(), fresh.getRole(), fresh.getBalance());
    }
}
