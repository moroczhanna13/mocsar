package hu.mocsar.server;

import static org.assertj.core.api.Assertions.assertThat;

import hu.mocsar.server.api.dto.JoinResponse;
import hu.mocsar.server.persistence.PlayerStats;
import hu.mocsar.server.persistence.StatsService;
import hu.mocsar.server.room.RoomService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Ugyanaz, mint a H2-s teszt, de igazi PostgreSQL-lel (Testcontainers).
 * Docker nélkül a teszt kimarad.
 */
@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest(properties = "mocsar.bot-delay-ms=0")
class PostgresLeaderboardTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    RoomService rooms;

    @Autowired
    StatsService stats;

    @Test
    void roundsArePersistedInPostgres() {
        JoinResponse host = rooms.createRoom("Anna");
        for (int i = 0; i < 8; i++) {
            rooms.addBot(host.code(), host.token());
        }
        RoomServiceTest.playRoundAsHost(rooms, host.code(), host.token());
        RoomServiceTest.playRoundAsHost(rooms, host.code(), host.token());
        RoomServiceTest.playRoundAsHost(rooms, host.code(), host.token());

        List<PlayerStats> board = stats.leaderboard(rooms.gameId(host.code()));
        assertThat(board).hasSize(9);
        assertThat(board.stream().mapToLong(PlayerStats::rounds).sum()).isEqualTo(27);
        assertThat(board.stream().mapToLong(PlayerStats::kings).sum()).isEqualTo(3);
        assertThat(board.stream().mapToLong(PlayerStats::swamps).sum()).isEqualTo(3);
        assertThat(stats.leaderboard()).isNotEmpty();
    }
}
