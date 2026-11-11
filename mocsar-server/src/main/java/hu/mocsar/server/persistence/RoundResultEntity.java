package hu.mocsar.server.persistence;

import hu.mocsar.core.Role;
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

/** Egy játékos helyezése és szerepe egy körben. Ebből jön a ranglista. */
@Entity
@Table(name = "round_result")
public class RoundResultEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "round_id")
    private RoundEntity round;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "player_id")
    private PlayerEntity player;

    /** Helyezés 1-től: 1 a király, az utolsó a Mocsár. */
    @Column(nullable = false)
    private int place;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private Role role;

    protected RoundResultEntity() {
    }

    public RoundResultEntity(RoundEntity round, PlayerEntity player, int place, Role role) {
        this.round = round;
        this.player = player;
        this.place = place;
        this.role = role;
    }

    public Long getId() {
        return id;
    }

    public RoundEntity getRound() {
        return round;
    }

    public PlayerEntity getPlayer() {
        return player;
    }

    public int getPlace() {
        return place;
    }

    public Role getRole() {
        return role;
    }
}
