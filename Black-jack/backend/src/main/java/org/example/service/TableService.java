package org.example.service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.example.dto.CreateTableRequest;
import org.example.dto.CurrentSeatResponse;
import org.example.dto.RoundIntermissionEvent;
import org.example.dto.SeatState;
import org.example.dto.TableJoinMessage;
import org.example.dto.TableLeaveMessage;
import org.example.dto.TableResponse;
import org.example.dto.TableStateEvent;
import org.example.entity.GameTable;
import org.example.entity.TableSeat;
import org.example.entity.TableStatus;
import org.example.entity.User;
import org.example.exception.ApiException;
import org.example.repository.GameTableRepository;
import org.example.repository.TableSeatRepository;
import org.example.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TableService {

    public static final int INTERMISSION_SECONDS = 10;

    private final GameTableRepository gameTableRepository;
    private final TableSeatRepository tableSeatRepository;
    private final UserRepository userRepository;
    private final TableEventPublisher eventPublisher;
    private final GameRoundService gameRoundService;
    private final TableRoundScheduler roundScheduler;

    public TableService(
            GameTableRepository gameTableRepository,
            TableSeatRepository tableSeatRepository,
            UserRepository userRepository,
            TableEventPublisher eventPublisher,
            GameRoundService gameRoundService,
            TableRoundScheduler roundScheduler) {
        this.gameTableRepository = gameTableRepository;
        this.tableSeatRepository = tableSeatRepository;
        this.userRepository = userRepository;
        this.eventPublisher = eventPublisher;
        this.gameRoundService = gameRoundService;
        this.roundScheduler = roundScheduler;
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
        if (table.getStatus() != TableStatus.waiting && table.getStatus() != TableStatus.settlement) {
            throw new ApiException("FORBIDDEN", "Table is not accepting players");
        }
        if (tableSeatRepository.existsByUserId(user.getId())) {
            throw new ApiException("CONFLICT", "User already seated at a table");
        }
        if (message.seatIndex() < 0 || message.seatIndex() >= table.getMaxPlayers()) {
            throw new ApiException("VALIDATION_ERROR", "Invalid seat index");
        }

        TableSeat seat = tableSeatRepository
                .findByGameTableIdAndSeatIndex(table.getId(), message.seatIndex())
                .orElseThrow(() -> new ApiException("NOT_FOUND", "Seat not found"));

        if (seat.getUser() != null) {
            throw new ApiException("CONFLICT", "Seat already occupied");
        }

        seat.setUser(user);
        seat.setDealer(false);
        seat.setJoinedAt(Instant.now());
        tableSeatRepository.save(seat);

        TableStateEvent state = buildTableState(table);
        eventPublisher.publishTableState(state);
        if (table.getStatus() == TableStatus.waiting) {
            evaluateStartConditions(table);
        }
        return state;
    }

    @Transactional
    public void beginIntermission(UUID tableId) {
        GameTable table = findTable(tableId);
        Instant endsAt = Instant.now().plusSeconds(INTERMISSION_SECONDS);
        table.setStatus(TableStatus.settlement);
        table.setIntermissionEndsAt(endsAt);
        gameTableRepository.save(table);

        TableStateEvent state = buildTableState(table);
        eventPublisher.publishTableState(state);
        eventPublisher.publishToTable(
                tableId,
                new RoundIntermissionEvent(
                        "round.intermission",
                        tableId,
                        endsAt,
                        INTERMISSION_SECONDS,
                        table.getMinBet(),
                        table.getMaxBet()));

        roundScheduler.scheduleIntermissionEnd(tableId, endsAt);
    }

    @Transactional
    public void finishIntermission(UUID tableId) {
        GameTable table = findTable(tableId);
        if (table.getStatus() != TableStatus.settlement) {
            return;
        }

        roundScheduler.cancel(tableId);
        table.setIntermissionEndsAt(null);
        table.setStatus(TableStatus.waiting);
        gameTableRepository.save(table);

        long occupiedPlayers = tableSeatRepository.countOccupiedPlayerSeats(tableId);
        if (occupiedPlayers >= 1) {
            gameRoundService.startRound(tableId);
            table = findTable(tableId);
        }

        eventPublisher.publishTableState(buildTableState(table));
    }

    @Transactional(readOnly = true)
    public TableStateEvent getTableState(UUID tableId) {
        GameTable table = findTable(tableId);
        return buildTableState(table);
    }

    @Transactional(readOnly = true)
    public CurrentSeatResponse getCurrentSeat(User user) {
        return tableSeatRepository
                .findByUserId(user.getId())
                .map(seat -> new CurrentSeatResponse(
                        seat.getGameTable().getId(),
                        seat.getGameTable().getName(),
                        seat.getSeatIndex()))
                .orElse(null);
    }

    @Transactional
    public TableStateEvent leaveTable(User user, TableLeaveMessage message) {
        GameTable table = findTable(message.tableId());
        if (table.getStatus() == TableStatus.in_game) {
            gameRoundService.abortRoundIfBetting(table.getId());
            table = findTable(message.tableId());
        }
        TableSeat seat = tableSeatRepository
                .findByGameTableIdAndUserId(table.getId(), user.getId())
                .orElseThrow(() -> new ApiException("FORBIDDEN", "User not at this table"));

        seat.setUser(null);
        seat.setJoinedAt(null);
        seat.setDealer(false);
        tableSeatRepository.save(seat);

        table = findTable(message.tableId());
        if (table.getStatus() == TableStatus.settlement) {
            long remainingPlayers = tableSeatRepository.countOccupiedPlayerSeats(table.getId());
            if (remainingPlayers == 0) {
                roundScheduler.cancel(table.getId());
                table.setIntermissionEndsAt(null);
                table.setStatus(TableStatus.waiting);
                gameTableRepository.save(table);
            }
        }

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
                        seat.getUser().getUsername()));
            }
        }

        long occupiedPlayers = tableSeatRepository.countOccupiedPlayerSeats(table.getId());

        return new TableStateEvent(
                "table.state",
                table.getId(),
                table.getStatus(),
                seatStates,
                (int) occupiedPlayers,
                table.getMaxPlayers(),
                table.getMinBet(),
                table.getMaxBet(),
                table.getIntermissionEndsAt());
    }

    private void evaluateStartConditions(GameTable table) {
        long occupiedPlayers = tableSeatRepository.countOccupiedPlayerSeats(table.getId());
        if (occupiedPlayers >= 1) {
            gameRoundService.startRound(table.getId());
            table = findTable(table.getId());
            eventPublisher.publishTableState(buildTableState(table));
        }
    }

    private void initializeSeats(GameTable table) {
        for (int i = 0; i < table.getMaxPlayers(); i++) {
            TableSeat seat = new TableSeat();
            seat.setGameTable(table);
            seat.setSeatIndex(i);
            seat.setDealer(false);
            tableSeatRepository.save(seat);
        }
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
                table.getMaxBet(),
                table.getIntermissionEndsAt());
    }

    private GameTable findTable(UUID tableId) {
        return gameTableRepository
                .findById(tableId)
                .orElseThrow(() -> new ApiException("NOT_FOUND", "Table not found"));
    }
}
