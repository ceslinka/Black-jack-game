package org.example.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.example.entity.GameHistoryEntry;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GameHistoryEntryRepository extends JpaRepository<GameHistoryEntry, UUID> {

    List<GameHistoryEntry> findByUserIdOrderBySettledAtDesc(UUID userId);
}
