package hu.mocsar.server;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.awaitility.Awaitility.await;

import hu.mocsar.core.Game;
import hu.mocsar.core.IllegalMoveException;
import hu.mocsar.core.Role;
import hu.mocsar.server.api.dto.JoinResponse;
import hu.mocsar.server.api.dto.RoomView;
import hu.mocsar.server.persistence.PlayerStats;
import hu.mocsar.server.persistence.StatsService;
import hu.mocsar.server.room.RoomException;
import hu.mocsar.server.room.RoomService;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/** A szobakezelés H2-vel: szabálytalan lépés elutasítása, botok, körök mentése, ranglista. */
@SpringBootTest(properties = "mocsar.bot-delay-ms=0")
class RoomServiceTest {

    @Autowired
    RoomService rooms;

    @Autowired
    StatsService stats;

    /** A gazda emberi játékos; a tesztből lépünk helyette, amíg a kör véget nem ér. */
    static void playRoundAsHost(RoomService rooms, String code, String token) {
        rooms.start(code, token);
        await().atMost(Duration.ofSeconds(30)).pollInterval(Duration.ofMillis(5)).until(() -> {
            RoomView view = rooms.view(code, token);
            if (view.phase() == Game.Phase.BETWEEN_ROUNDS) {
                return true;
            }
            if (view.phase() == Game.Phase.EXCHANGE && view.myGiveBack() > 0) {
                rooms.giveBack(code, token, TestMoves.weakest(view, view.myGiveBack()));
            } else if (view.phase() == Game.Phase.PLAYING && view.me().equals(view.currentPlayer())) {
                Optional<List<String>> play = TestMoves.lowestPlay(view);
                if (play.isPresent()) {
                    rooms.play(code, token, play.get());
                } else {
                    rooms.pass(code, token);
                }
            }
            return false;
        });
    }

    @Test
    void needsSevenPlayersAndOnlyHostStarts() {
        JoinResponse host = rooms.createRoom("Anna");
        JoinResponse guest = rooms.join(host.code(), "Bence");
        assertThatThrownBy(() -> rooms.start(host.code(), host.token()))
                .isInstanceOf(RoomException.class).hasMessageContaining("Legalább 7");
        assertThatThrownBy(() -> rooms.start(host.code(), guest.token()))
                .isInstanceOf(RoomException.class).hasMessageContaining("gazdája");
        assertThatThrownBy(() -> rooms.join(host.code(), "anna"))
                .isInstanceOf(RoomException.class).hasMessageContaining("foglalt");
        assertThatThrownBy(() -> rooms.join("NOPE", "x")).isInstanceOf(RoomException.class);
    }

    @Test
    void rejectsIllegalMovesAndWrongTokens() {
        JoinResponse host = rooms.createRoom("Anna");
        for (int i = 0; i < 6; i++) {
            rooms.addBot(host.code(), host.token());
        }
        assertThatThrownBy(() -> rooms.play(host.code(), host.token(), List.of("3C-0")))
                .isInstanceOf(IllegalMoveException.class);
        rooms.start(host.code(), host.token());
        assertThatThrownBy(() -> rooms.play(host.code(), "bad-token", List.of("3C-0")))
                .isInstanceOf(RoomException.class);
        assertThatThrownBy(() -> rooms.play(host.code(), host.token(), List.of("nonsense")))
                .isInstanceOf(IllegalMoveException.class).hasMessageContaining("Hibás lapazonosító");
        assertThatThrownBy(() -> rooms.join(host.code(), "Késő"))
                .isInstanceOf(RoomException.class).hasMessageContaining("Kör közben");
    }

    @Test
    void botsPlayFullRoundsAndResultsArePersisted() {
        JoinResponse host = rooms.createRoom("Anna");
        for (int i = 0; i < 7; i++) {
            rooms.addBot(host.code(), host.token());
        }
        RoomView before = rooms.view(host.code(), host.token());
        assertThat(before.players()).hasSize(8);
        assertThat(before.canStart()).isTrue();

        playRoundAsHost(rooms, host.code(), host.token());
        RoomView after = rooms.view(host.code(), host.token());
        assertThat(after.lastRound().number()).isEqualTo(1);
        assertThat(after.lastRound().finishOrder()).hasSize(8);
        assertThat(after.players().get(0).role()).isEqualTo(Role.KING);
        assertThat(after.players().get(7).role()).isEqualTo(Role.SWAMP);

        playRoundAsHost(rooms, host.code(), host.token());
        assertThat(rooms.view(host.code(), host.token()).lastRound().number()).isEqualTo(2);

        List<PlayerStats> board = stats.leaderboard(rooms.gameId(host.code()));
        assertThat(board).hasSize(8);
        assertThat(board.stream().mapToLong(PlayerStats::rounds).sum()).isEqualTo(16);
        assertThat(board.stream().mapToLong(PlayerStats::kings).sum()).isEqualTo(2);
        assertThat(board.stream().mapToLong(PlayerStats::swamps).sum()).isEqualTo(2);
        assertThat(board.get(0).kings()).isGreaterThanOrEqualTo(1);
    }

    @Test
    void viewNeverContainsOtherPlayersCards() {
        JoinResponse host = rooms.createRoom("Anna");
        JoinResponse guest = rooms.join(host.code(), "Bence");
        for (int i = 0; i < 5; i++) {
            rooms.addBot(host.code(), host.token());
        }
        rooms.start(host.code(), host.token());
        RoomView hostView = rooms.view(host.code(), host.token());
        RoomView guestView = rooms.view(host.code(), guest.token());
        assertThat(hostView.hand()).hasSize(14);
        assertThat(guestView.hand()).hasSize(14);
        assertThat(hostView.hand()).doesNotContainAnyElementsOf(guestView.hand());
        assertThat(hostView.players()).allSatisfy(p -> assertThat(p.cardCount()).isEqualTo(14));
    }
}
