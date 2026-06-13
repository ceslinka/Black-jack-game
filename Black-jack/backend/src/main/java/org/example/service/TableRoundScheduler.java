package org.example.service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import org.example.entity.GameTable;
import org.example.entity.TableStatus;
import org.example.repository.GameTableRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Component;

@Component
public class TableRoundScheduler {

    private static final Logger log = LoggerFactory.getLogger(TableRoundScheduler.class);

    private final TaskScheduler taskScheduler;
    private final TableService tableService;
    private final GameTableRepository gameTableRepository;
    private final ConcurrentHashMap<UUID, ScheduledFuture<?>> scheduled = new ConcurrentHashMap<>();

    public TableRoundScheduler(
            TaskScheduler taskScheduler,
            @Lazy TableService tableService,
            GameTableRepository gameTableRepository) {
        this.taskScheduler = taskScheduler;
        this.tableService = tableService;
        this.gameTableRepository = gameTableRepository;
    }

    @EventListener(ApplicationReadyEvent.class)
    @Order(200)
    void recoverPendingIntermissions() {
        List<GameTable> tables = gameTableRepository.findByStatus(TableStatus.settlement);
        for (GameTable table : tables) {
            Instant endsAt = table.getIntermissionEndsAt();
            if (endsAt == null) {
                continue;
            }
            log.info("Resuming intermission schedule for table {}", table.getId());
            scheduleIntermissionEnd(table.getId(), endsAt);
        }
    }

    public void scheduleIntermissionEnd(UUID tableId, Instant endsAt) {
        cancel(tableId);
        if (!endsAt.isAfter(Instant.now())) {
            tableService.finishIntermission(tableId);
            return;
        }
        ScheduledFuture<?> future = taskScheduler.schedule(
                () -> {
                    try {
                        tableService.finishIntermission(tableId);
                    } finally {
                        scheduled.remove(tableId);
                    }
                },
                endsAt);
        scheduled.put(tableId, future);
    }

    public void cancel(UUID tableId) {
        ScheduledFuture<?> future = scheduled.remove(tableId);
        if (future != null) {
            future.cancel(false);
        }
    }
}
