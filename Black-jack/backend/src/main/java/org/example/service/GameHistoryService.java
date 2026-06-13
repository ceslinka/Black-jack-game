package org.example.service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.example.dto.GameHistoryItemResponse;
import org.example.dto.GameHistoryListResponse;
import org.example.entity.GameHistoryEntry;
import org.example.entity.GameTable;
import org.example.entity.TableBalanceMilestone;
import org.example.entity.User;
import org.example.repository.GameHistoryEntryRepository;
import org.example.repository.TableBalanceMilestoneRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GameHistoryService {

    private final GameHistoryEntryRepository gameHistoryEntryRepository;
    private final TableBalanceMilestoneRepository milestoneRepository;

    public GameHistoryService(
            GameHistoryEntryRepository gameHistoryEntryRepository,
            TableBalanceMilestoneRepository milestoneRepository) {
        this.gameHistoryEntryRepository = gameHistoryEntryRepository;
        this.milestoneRepository = milestoneRepository;
    }

    @Transactional
    public void recordPlayerRound(
            User user,
            GameTable table,
            UUID roundId,
            String outcome,
            int betAmount,
            int payout) {
        int balanceAfter = user.getBalance();
        int balanceBefore = balanceAfter - payout + betAmount;

        GameHistoryEntry entry = new GameHistoryEntry();
        entry.setUserId(user.getId());
        entry.setTableId(table.getId());
        entry.setTableName(table.getName());
        entry.setRoundId(roundId);
        entry.setOutcome(outcome);
        entry.setBetAmount(betAmount);
        entry.setPayout(payout);
        entry.setBalanceBefore(balanceBefore);
        entry.setBalanceAfter(balanceAfter);
        gameHistoryEntryRepository.save(entry);

        TableBalanceMilestone milestone = milestoneRepository
                .findByTableIdAndUserId(table.getId(), user.getId())
                .orElseGet(TableBalanceMilestone::new);
        milestone.setTableId(table.getId());
        milestone.setUserId(user.getId());
        milestone.setBalanceAfter(balanceAfter);
        milestone.setLastRoundId(roundId);
        milestone.setUpdatedAt(Instant.now());
        milestoneRepository.save(milestone);
    }

    @Transactional(readOnly = true)
    public GameHistoryListResponse getPlayerHistory(User user) {
        List<GameHistoryItemResponse> items = gameHistoryEntryRepository.findByUserIdOrderBySettledAtDesc(user.getId()).stream()
                .map(entry -> new GameHistoryItemResponse(
                        entry.getRoundId(),
                        entry.getTableId(),
                        entry.getTableName(),
                        entry.getOutcome(),
                        entry.getBetAmount(),
                        entry.getPayout(),
                        entry.getBalanceBefore(),
                        entry.getBalanceAfter(),
                        entry.getBalanceAfter() - entry.getBalanceBefore(),
                        entry.getSettledAt()))
                .toList();
        return new GameHistoryListResponse(items);
    }
}
