package org.example.security;

import org.example.entity.User;
import org.example.entity.UserRole;
import org.example.repository.UserRepository;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class AuthSupport {

    private final UserRepository userRepository;

    public AuthSupport(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public User currentUser() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (!(principal instanceof AuthenticatedUser authenticatedUser)) {
            throw new org.example.exception.ApiException("UNAUTHORIZED", "Authentication required");
        }
        return userRepository
                .findById(authenticatedUser.getId())
                .orElseThrow(() -> new org.example.exception.ApiException("UNAUTHORIZED", "User not found"));
    }

    public static boolean isDealerRole(UserRole role) {
        return role == UserRole.dealer;
    }
}
