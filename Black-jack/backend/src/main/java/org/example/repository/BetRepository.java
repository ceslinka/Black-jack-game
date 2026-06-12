package org.example.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.example.entity.Bet;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BetRepository extends JpaRepository<Bet, UUID> {

    Optional<Bet> findByRoundIdAndUserId(UUID roundId, UUID userId);

    List<Bet> findByRoundId(UUID roundId);

    long countByRoundId(UUID roundId);
}
