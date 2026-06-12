package org.example;

import java.util.UUID;
import org.example.dto.CreateTableRequest;
import org.example.dto.TableJoinMessage;
import org.example.dto.TableStateEvent;
import org.example.entity.User;
import org.example.entity.UserRole;
import org.example.repository.UserRepository;
import org.example.service.TableService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

@Transactional
@Transactional
class TableJoinServiceTest extends IntegrationTestBase {

    @Autowired
    private TableService tableService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void joinTableReturnsTableStateEvent() {
        UUID tableId = tableService
                .createTable(new CreateTableRequest("Join Test", 4, 10, 200))
                .id();
        User player = saveUser("join@test.com", "joiner", UserRole.player);

        TableStateEvent state = tableService.joinTable(player, new TableJoinMessage(tableId, 0, false));

        assertThat(state.type()).isEqualTo("table.state");
        assertThat(state.tableId()).isEqualTo(tableId);
        assertThat(state.seats()).hasSize(1);
        assertThat(state.seats().get(0).username()).isEqualTo("joiner");
        assertThat(state.seats().get(0).dealer()).isFalse();
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
