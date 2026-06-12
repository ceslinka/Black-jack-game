package org.example.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.example.entity.Round;
import org.example.entity.RoundStatus;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoundRepository extends JpaRepository<Round, UUID> {

    Optional<Round> findFirstByGameTableIdAndStatusNotOrderByStartedAtDesc(UUID tableId, RoundStatus status);

    Optional<Round> findFirstByGameTableIdAndStatusOrderByStartedAtDesc(UUID tableId, RoundStatus status);
}
