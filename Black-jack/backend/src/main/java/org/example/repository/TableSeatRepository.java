package org.example.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.example.entity.TableSeat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TableSeatRepository extends JpaRepository<TableSeat, UUID> {

    List<TableSeat> findByGameTableIdOrderBySeatIndex(UUID tableId);

    Optional<TableSeat> findByGameTableIdAndSeatIndex(UUID tableId, int seatIndex);

    Optional<TableSeat> findByGameTableIdAndUserId(UUID tableId, UUID userId);

    @Query("SELECT COUNT(s) FROM TableSeat s WHERE s.gameTable.id = :tableId AND s.user IS NOT NULL AND s.dealer = false")
    long countOccupiedPlayerSeats(@Param("tableId") UUID tableId);

    @Query("SELECT COUNT(s) > 0 FROM TableSeat s WHERE s.gameTable.id = :tableId AND s.dealer = true AND s.user IS NOT NULL")
    boolean existsOccupiedDealerSeat(@Param("tableId") UUID tableId);

    boolean existsByUserId(UUID userId);
}
