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

@Entity
@Table(name = "player")
public class PlayerEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 32)
    private String nickname;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "game_id")
    private GameEntity game;

    @Column(name = "join_order", nullable = false)
    private int joinOrder;

    @Column(nullable = false)
    private boolean bot;

    protected PlayerEntity() {
    }

    public PlayerEntity(String nickname, GameEntity game, int joinOrder, boolean bot) {
        this.nickname = nickname;
        this.game = game;
        this.joinOrder = joinOrder;
        this.bot = bot;
    }

    public Long getId() {
        return id;
    }

    public String getNickname() {
        return nickname;
    }

    public GameEntity getGame() {
        return game;
    }

    public int getJoinOrder() {
        return joinOrder;
    }

    public boolean isBot() {
        return bot;
    }
}
