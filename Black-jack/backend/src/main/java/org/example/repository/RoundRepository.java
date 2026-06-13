package org.example.repository;

import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.example.entity.Round;
import org.example.entity.RoundStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RoundRepository extends JpaRepository<Round, UUID> {

    Optional<Round> findFirstByGameTableIdAndStatusNotOrderByStartedAtDesc(UUID tableId, RoundStatus status);

    Optional<Round> findFirstByGameTableIdAndStatusOrderByStartedAtDesc(UUID tableId, RoundStatus status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT r FROM Round r WHERE r.id = :id")
    Optional<Round> findByIdForUpdate(@Param("id") UUID id);

    @Query("SELECT r FROM Round r WHERE r.gameTable.id = :tableId AND r.status IN :statuses")
    List<Round> findByGameTableIdAndStatusIn(@Param("tableId") UUID tableId, @Param("statuses") List<RoundStatus> statuses);

    List<Round> findByStatusIn(List<RoundStatus> statuses);
}
