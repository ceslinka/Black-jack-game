package org.example.service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.example.dto.CreateTableRequest;
import org.example.dto.RoundCannotStartEvent;
import org.example.dto.SeatState;
import org.example.dto.TableJoinMessage;
import org.example.dto.TableLeaveMessage;
import org.example.dto.TableResponse;
import org.example.dto.TableStateEvent;
import org.example.entity.GameTable;
import org.example.entity.TableSeat;
import org.example.entity.TableStatus;
import org.example.entity.User;
import org.example.entity.UserRole;
import org.example.exception.ApiException;
import org.example.repository.GameTableRepository;
import org.example.repository.TableSeatRepository;
import org.example.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TableService {

    public static final int DEALER_SEAT_INDEX = 6;

    private final GameTableRepository gameTableRepository;
    private final TableSeatRepository tableSeatRepository;
    private final UserRepository userRepository;
    private final TableEventPublisher eventPublisher;
    private final GameRoundService gameRoundService;

    public TableService(
            GameTableRepository gameTableRepository,
            TableSeatRepository tableSeatRepository,
            UserRepository userRepository,
            TableEventPublisher eventPublisher,
            GameRoundService gameRoundService) {
        this.gameTableRepository = gameTableRepository;
        this.tableSeatRepository = tableSeatRepository;
        this.userRepository = userRepository;
        this.eventPublisher = eventPublisher;
        this.gameRoundService = gameRoundService;
    }

    @Transactional(readOnly = true)
    public List<TableResponse> listTables(TableStatus statusFilter) {
        List<GameTable> tables =
                statusFilter == null ? gameTableRepository.findAll() : gameTableRepository.findByStatus(statusFilter);
        return tables.stream().map(this::toTableResponse).toList();
    }

    @Transactional(readOnly = true)
    public TableResponse getTable(UUID tableId) {
        GameTable table = findTable(tableId);
        return toTableResponse(table);
    }

    @Transactional
    public TableResponse createTable(CreateTableRequest request) {
        if (request.maxBet() < request.minBet()) {
            throw new ApiException("VALIDATION_ERROR", "maxBet must be >= minBet");
        }

        GameTable table = new GameTable();
        table.setName(request.name());
        table.setMaxPlayers(request.maxPlayers());
        table.setMinBet(request.minBet());
        table.setMaxBet(request.maxBet());
        table.setStatus(TableStatus.waiting);
        gameTableRepository.save(table);

        initializeSeats(table);
        return toTableResponse(table);
    }

    @Transactional
    public TableStateEvent joinTable(User user, TableJoinMessage message) {
        GameTable table = findTable(message.tableId());
        if (table.getStatus() != TableStatus.waiting) {
            throw new ApiException("FORBIDDEN", "Table is not accepting players");
        }
        if (tableSeatRepository.existsByUserId(user.getId())) {
            throw new ApiException("CONFLICT", "User already seated at a table");
        }

        boolean asDealer = message.asDealer();
        if (asDealer && user.getRole() != UserRole.dealer) {
            throw new ApiException("FORBIDDEN", "Only dealer accounts can join as dealer");
        }
        if (!asDealer && message.seatIndex() >= table.getMaxPlayers()) {
            throw new ApiException("VALIDATION_ERROR", "Invalid player seat index");
        }
        if (asDealer && message.seatIndex() != DEALER_SEAT_INDEX) {
            throw new ApiException("VALIDATION_ERROR", "Dealer must use seat index " + DEALER_SEAT_INDEX);
        }

        TableSeat seat = tableSeatRepository
                .findByGameTableIdAndSeatIndex(table.getId(), message.seatIndex())
                .orElseThrow(() -> new ApiException("NOT_FOUND", "Seat not found"));

        if (seat.getUser() != null) {
            throw new ApiException("CONFLICT", "Seat already occupied");
        }
        if (asDealer && tableSeatRepository.existsOccupiedDealerSeat(table.getId())) {
            throw new ApiException("CONFLICT", "Dealer seat already occupied");
        }

        seat.setUser(user);
        seat.setDealer(asDealer);
        seat.setJoinedAt(Instant.now());
        tableSeatRepository.save(seat);

        TableStateEvent state = buildTableState(table);
        eventPublisher.publishTableState(state);
        evaluateStartConditions(table);
        return state;
    }

    @Transactional
    public TableStateEvent leaveTable(User user, TableLeaveMessage message) {
        GameTable table = findTable(message.tableId());
        if (table.getStatus() != TableStatus.waiting) {
            throw new ApiException("FORBIDDEN", "Cannot leave during active game");
        }
        TableSeat seat = tableSeatRepository
                .findByGameTableIdAndUserId(table.getId(), user.getId())
                .orElseThrow(() -> new ApiException("FORBIDDEN", "User not at this table"));

        seat.setUser(null);
        seat.setDealer(false);
        seat.setJoinedAt(null);
        tableSeatRepository.save(seat);

        TableStateEvent state = buildTableState(table);
        eventPublisher.publishTableState(state);
        return state;
    }

    @Transactional(readOnly = true)
    public TableStateEvent buildTableState(GameTable table) {
        List<TableSeat> seats = tableSeatRepository.findByGameTableIdOrderBySeatIndex(table.getId());
        List<SeatState> seatStates = new ArrayList<>();
        for (TableSeat seat : seats) {
            if (seat.getUser() != null) {
                seatStates.add(new SeatState(
                        seat.getSeatIndex(),
                        seat.getUser().getId(),
                        seat.getUser().getUsername(),
                        seat.isDealer()));
            }
        }

        long occupiedPlayers = tableSeatRepository.countOccupiedPlayerSeats(table.getId());
        boolean dealerPresent = tableSeatRepository.existsOccupiedDealerSeat(table.getId());

        return new TableStateEvent(
                "table.state",
                table.getId(),
                table.getStatus(),
                seatStates,
                dealerPresent,
                (int) occupiedPlayers,
                table.getMaxPlayers());
    }

    private void evaluateStartConditions(GameTable table) {
        long occupiedPlayers = tableSeatRepository.countOccupiedPlayerSeats(table.getId());
        boolean dealerPresent = tableSeatRepository.existsOccupiedDealerSeat(table.getId());

        if (occupiedPlayers < table.getMaxPlayers()) {
            return;
        }

        if (!dealerPresent) {
            eventPublisher.publishRoundCannotStart(
                    new RoundCannotStartEvent("round.cannot_start", table.getId(), "no_dealer"));
            return;
        }
        gameRoundService.startRound(table.getId());
    }

    private void initializeSeats(GameTable table) {
        for (int i = 0; i < table.getMaxPlayers(); i++) {
            createEmptySeat(table, i, false);
        }
        createEmptySeat(table, DEALER_SEAT_INDEX, true);
    }

    private void createEmptySeat(GameTable table, int index, boolean dealer) {
        TableSeat seat = new TableSeat();
        seat.setGameTable(table);
        seat.setSeatIndex(index);
        seat.setDealer(dealer);
        tableSeatRepository.save(seat);
    }

    private TableResponse toTableResponse(GameTable table) {
        long currentPlayers = tableSeatRepository.countOccupiedPlayerSeats(table.getId());
        return new TableResponse(
                table.getId(),
                table.getName(),
                table.getMaxPlayers(),
                (int) currentPlayers,
                table.getStatus(),
                table.getMinBet(),
                table.getMaxBet());
    }

    private GameTable findTable(UUID tableId) {
        return gameTableRepository
                .findById(tableId)
                .orElseThrow(() -> new ApiException("NOT_FOUND", "Table not found"));
    }
}
