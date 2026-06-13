package org.example;

import java.util.UUID;
import org.example.dto.CreateTableRequest;
import org.example.dto.TableJoinMessage;
import org.example.dto.TableResponse;
import org.example.entity.User;
import org.example.entity.UserRole;
import org.example.repository.UserRepository;
import org.example.service.TableService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

@Transactional
class DealerRequirementTest extends IntegrationTestBase {

    @Autowired
    private TableService tableService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private UUID tableId;

    @BeforeEach
    void setUp() {
        TableResponse created = tableService.createTable(new CreateTableRequest("M5 Table", 2, 10, 100));
        tableId = created.id();
    }

    @Test
    void singlePlayerStartsRoundWithoutHumanDealer() {
        User player = saveUser("solo@test.com", "solo", UserRole.player);
        var state = tableService.joinTable(player, new TableJoinMessage(tableId, 0));
        assertThat(state.occupiedPlayerSeats()).isEqualTo(1);
    }

    private User saveUser(String email, String username, UserRole role) {
        User user = new User();
        user.setEmail(email);
        user.setUsername(username);
        user.setPasswordHash(passwordEncoder.encode("Secret1!"));
        user.setRole(role);
        user.setBalance(1000);
        return userRepository.save(user);
    }
}
