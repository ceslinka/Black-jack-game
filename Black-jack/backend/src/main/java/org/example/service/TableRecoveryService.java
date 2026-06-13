package org.example.service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.example.entity.Bet;
import org.example.entity.GameTable;
import org.example.entity.Round;
import org.example.entity.RoundStatus;
import org.example.entity.TableSeat;
import org.example.entity.TableStatus;
import org.example.entity.User;
import org.example.repository.BetRepository;
import org.example.repository.GameTableRepository;
import org.example.repository.HandRepository;
import org.example.repository.RoundRepository;
import org.example.repository.TableBalanceMilestoneRepository;
import org.example.repository.TableSeatRepository;
import org.example.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TableRecoveryService {

    private static final Logger log = LoggerFactory.getLogger(TableRecoveryService.class);

    private final GameTableRepository gameTableRepository;
    private final RoundRepository roundRepository;
    private final HandRepository handRepository;
    private final TableSeatRepository tableSeatRepository;
    private final TableBalanceMilestoneRepository milestoneRepository;
    private final BetRepository betRepository;
    private final UserRepository userRepository;
    private final TableRoundScheduler roundScheduler;

    public TableRecoveryService(
            GameTableRepository gameTableRepository,
            RoundRepository roundRepository,
            HandRepository handRepository,
            TableSeatRepository tableSeatRepository,
            TableBalanceMilestoneRepository milestoneRepository,
            BetRepository betRepository,
            UserRepository userRepository,
            TableRoundScheduler roundScheduler) {
        this.gameTableRepository = gameTableRepository;
        this.roundRepository = roundRepository;
        this.handRepository = handRepository;
        this.tableSeatRepository = tableSeatRepository;
        this.milestoneRepository = milestoneRepository;
        this.betRepository = betRepository;
        this.userRepository = userRepository;
        this.roundScheduler = roundScheduler;
    }

    @EventListener(ApplicationReadyEvent.class)
    @Order(100)
    @Transactional
    public void recoverOnStartup() {
        List<RoundStatus> activeStatuses = List.of(RoundStatus.betting, RoundStatus.playing);
        Set<UUID> tableIds = new HashSet<>();

        for (Round round : roundRepository.findByStatusIn(activeStatuses)) {
            tableIds.add(round.getGameTable().getId());
        }
        for (GameTable table : gameTableRepository.findByStatus(TableStatus.in_game)) {
            tableIds.add(table.getId());
        }
        for (GameTable table : gameTableRepository.findByStatus(TableStatus.settlement)) {
            tableIds.add(table.getId());
        }

        if (tableIds.isEmpty()) {
            return;
        }

        log.info("Recovering {} table session(s) after interrupted state", tableIds.size());
        for (UUID tableId : tableIds) {
            recoverTable(tableId);
        }
    }

    @Transactional
    public void recoverTable(UUID tableId) {
        roundScheduler.cancel(tableId);

        GameTable table = gameTableRepository.findById(tableId).orElse(null);
        if (table == null) {
            return;
        }

        List<Round> corruptedRounds = roundRepository.findByGameTableIdAndStatusIn(
                tableId, List.of(RoundStatus.betting, RoundStatus.playing));

        Set<UUID> userIds = new HashSet<>();
        for (TableSeat seat : tableSeatRepository.findByGameTableIdOrderBySeatIndex(tableId)) {
            if (seat.getUser() != null) {
                userIds.add(seat.getUser().getId());
            }
        }
        for (Round round : corruptedRounds) {
            for (Bet bet : betRepository.findByRoundId(round.getId())) {
                userIds.add(bet.getUser().getId());
            }
        }

        for (UUID userId : userIds) {
            restoreUserBalance(tableId, userId, corruptedRounds);
        }

        for (Round round : corruptedRounds) {
            handRepository.deleteByRoundId(round.getId());
            betRepository.deleteByRoundId(round.getId());
            roundRepository.delete(round);
        }

        for (TableSeat seat : tableSeatRepository.findByGameTableIdOrderBySeatIndex(tableId)) {
            if (seat.getUser() != null) {
                seat.setUser(null);
                seat.setJoinedAt(null);
                seat.setDealer(false);
                tableSeatRepository.save(seat);
            }
        }

        table.setStatus(TableStatus.waiting);
        table.setIntermissionEndsAt(null);
        gameTableRepository.save(table);

        log.info("Table {} reset to waiting; {} user(s) restored", tableId, userIds.size());
    }

    private void restoreUserBalance(UUID tableId, UUID userId, List<Round> corruptedRounds) {
        User user = userRepository.findByIdForUpdate(userId).orElse(null);
        if (user == null) {
            return;
        }

        var milestone = milestoneRepository.findByTableIdAndUserId(tableId, userId);
        if (milestone.isPresent()) {
            user.setBalance(milestone.get().getBalanceAfter());
            userRepository.save(user);
            return;
        }

        int refund = 0;
        for (Round round : corruptedRounds) {
            var bet = betRepository.findByRoundIdAndUserId(round.getId(), userId);
            if (bet.isPresent()) {
                refund += bet.get().getAmount();
            }
        }
        if (refund > 0) {
            user.setBalance(user.getBalance() + refund);
            userRepository.save(user);
        }
    }
}
