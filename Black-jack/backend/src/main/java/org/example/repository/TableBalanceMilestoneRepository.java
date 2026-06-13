package org.example.repository;

import java.util.Optional;
import java.util.UUID;
import org.example.entity.TableBalanceMilestone;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TableBalanceMilestoneRepository extends JpaRepository<TableBalanceMilestone, UUID> {

    Optional<TableBalanceMilestone> findByTableIdAndUserId(UUID tableId, UUID userId);
}
