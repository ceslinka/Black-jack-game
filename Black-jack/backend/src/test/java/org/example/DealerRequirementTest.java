package org.example;

import java.util.UUID;
import org.example.dto.CreateTableRequest;
import org.example.dto.RoundCannotStartEvent;
import org.example.dto.TableJoinMessage;
import org.example.dto.TableResponse;
import org.example.entity.User;
import org.example.entity.UserRole;
import org.example.repository.UserRepository;
import org.example.service.TableEventPublisher;
import org.example.service.TableService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@Transactional
class DealerRequirementTest extends IntegrationTestBase {

    @Autowired
    private TableService tableService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @MockBean
    private TableEventPublisher eventPublisher;

    private UUID tableId;

    @BeforeEach
    void setUp() {
        TableResponse created = tableService.createTable(new CreateTableRequest("M5 Table", 2, 10, 100));
        tableId = created.id();
    }

    @Test
    void fullTableWithoutDealerEmitsRoundCannotStart() {
        User p1 = saveUser("p1@test.com", "player1", UserRole.player);
        User p2 = saveUser("p2@test.com", "player2", UserRole.player);

        tableService.joinTable(p1, new TableJoinMessage(tableId, 0, false));
        tableService.joinTable(p2, new TableJoinMessage(tableId, 1, false));

        ArgumentCaptor<RoundCannotStartEvent> captor = ArgumentCaptor.forClass(RoundCannotStartEvent.class);
        verify(eventPublisher).publishRoundCannotStart(captor.capture());

        RoundCannotStartEvent event = captor.getValue();
        assertThat(event.type()).isEqualTo("round.cannot_start");
        assertThat(event.tableId()).isEqualTo(tableId);
        assertThat(event.reason()).isEqualTo("no_dealer");
    }

    @Test
    void fullTableWithDealerDoesNotEmitRoundCannotStart() {
        User p1 = saveUser("p3@test.com", "player3", UserRole.player);
        User p2 = saveUser("p4@test.com", "player4", UserRole.player);
        User dealer = saveUser("dealer@test.com", "dealer1", UserRole.dealer);

        tableService.joinTable(p1, new TableJoinMessage(tableId, 0, false));
        tableService.joinTable(p2, new TableJoinMessage(tableId, 1, false));
        tableService.joinTable(dealer, new TableJoinMessage(tableId, TableService.DEALER_SEAT_INDEX, true));

        verify(eventPublisher, never()).publishRoundCannotStart(any());
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
