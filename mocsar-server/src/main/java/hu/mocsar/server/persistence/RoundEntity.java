package hu.mocsar.server.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;

/** Egy befejezett kör. A zajló kör állapota csak a memóriában él. */
@Entity
@Table(name = "round")
public class RoundEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "game_id")
    private GameEntity game;

    @Column(name = "number", nullable = false)
    private int number;

    @Column(name = "player_count", nullable = false)
    private int playerCount;

    @Column(name = "removed_cards", nullable = false)
    private int removedCards;

    @Column(name = "finished_at", nullable = false)
    private Instant finishedAt = Instant.now();

    protected RoundEntity() {
    }

    public RoundEntity(GameEntity game, int number, int playerCount, int removedCards) {
        this.game = game;
        this.number = number;
        this.playerCount = playerCount;
        this.removedCards = removedCards;
    }

    public Long getId() {
        return id;
    }

    public GameEntity getGame() {
        return game;
    }

    public int getNumber() {
        return number;
    }

    public int getPlayerCount() {
        return playerCount;
    }

    public int getRemovedCards() {
        return removedCards;
    }

    public Instant getFinishedAt() {
        return finishedAt;
    }
}
