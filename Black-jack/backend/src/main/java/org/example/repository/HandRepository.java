package org.example.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.example.entity.Hand;
import org.example.entity.HandStatus;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HandRepository extends JpaRepository<Hand, UUID> {

    List<Hand> findByRoundIdOrderBySeatIndex(UUID roundId);

    Optional<Hand> findByRoundIdAndUserId(UUID roundId, UUID userId);

    Optional<Hand> findFirstByRoundIdAndStatusOrderBySeatIndexAsc(UUID roundId, HandStatus status);
}
