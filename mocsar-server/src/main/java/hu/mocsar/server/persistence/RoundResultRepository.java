package hu.mocsar.server.persistence;

import hu.mocsar.core.Role;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RoundResultRepository extends JpaRepository<RoundResultEntity, Long> {

    /** Becenév, szerep, darab: minden játékra. */
    record RoleCount(String nickname, Role role, long count) {
    }

    @Query("""
            select new hu.mocsar.server.persistence.RoundResultRepository$RoleCount(p.nickname, r.role, count(r))
            from RoundResultEntity r join r.player p
            group by p.nickname, r.role
            """)
    List<RoleCount> countRolesByNickname();

    @Query("""
            select new hu.mocsar.server.persistence.RoundResultRepository$RoleCount(p.nickname, r.role, count(r))
            from RoundResultEntity r join r.player p
            where p.game.id = :gameId
            group by p.nickname, r.role
            """)
    List<RoleCount> countRolesByNicknameForGame(@Param("gameId") Long gameId);

    List<RoundResultEntity> findByRoundIdOrderByPlace(Long roundId);
}
