package hu.mocsar.server.persistence;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoundRepository extends JpaRepository<RoundEntity, Long> {

    List<RoundEntity> findByGameIdOrderByNumber(Long gameId);
}
