package org.example.repository;

import java.util.List;
import java.util.UUID;
import org.example.entity.GameTable;
import org.example.entity.TableStatus;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GameTableRepository extends JpaRepository<GameTable, UUID> {

    List<GameTable> findByStatus(TableStatus status);
}
