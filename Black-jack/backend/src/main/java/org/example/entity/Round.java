package org.example.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "rounds")
public class Round {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "table_id", nullable = false)
    private GameTable gameTable;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RoundStatus status = RoundStatus.betting;

    @Column(name = "current_seat_index")
    private Integer currentSeatIndex;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "dealer_cards", columnDefinition = "jsonb")
    private String dealerCardsJson = "[]";

    @Column(name = "dealer_hidden", nullable = false)
    private boolean dealerHidden = true;

    @Column(name = "started_at", nullable = false)
    private Instant startedAt = Instant.now();

    @Column(name = "ended_at")
    private Instant endedAt;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public GameTable getGameTable() {
        return gameTable;
    }

    public void setGameTable(GameTable gameTable) {
        this.gameTable = gameTable;
    }

    public RoundStatus getStatus() {
        return status;
    }

    public void setStatus(RoundStatus status) {
        this.status = status;
    }

    public Integer getCurrentSeatIndex() {
        return currentSeatIndex;
    }

    public void setCurrentSeatIndex(Integer currentSeatIndex) {
        this.currentSeatIndex = currentSeatIndex;
    }

    public String getDealerCardsJson() {
        return dealerCardsJson;
    }

    public void setDealerCardsJson(String dealerCardsJson) {
        this.dealerCardsJson = dealerCardsJson;
    }

    public boolean isDealerHidden() {
        return dealerHidden;
    }

    public void setDealerHidden(boolean dealerHidden) {
        this.dealerHidden = dealerHidden;
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(Instant startedAt) {
        this.startedAt = startedAt;
    }

    public Instant getEndedAt() {
        return endedAt;
    }

    public void setEndedAt(Instant endedAt) {
        this.endedAt = endedAt;
    }
}
