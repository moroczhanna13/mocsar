package hu.mocsar.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class GameTest {

    private static Game gameWith(int players, long seed) {
        Game game = new Game(new Random(seed));
        for (int i = 0; i < players; i++) {
            game.addPlayer("p" + i);
        }
        return game;
    }

    /** Véletlen, de szabályos lépés: a kezéből bármely azonos értékű, megfelelő méretű csoport, vagy passz. */
    static void randomLegalMove(Game game, Random rnd) {
        Round round = game.round();
        String player = round.currentPlayer();
        List<Card> hand = round.hand(player);
        List<List<Card>> options = new ArrayList<>();
        for (Rank rank : Rank.values()) {
            List<Card> ofRank = hand.stream().filter(c -> c.rank() == rank).toList();
            if (ofRank.isEmpty()) {
                continue;
            }
            if (round.pile().isEmpty()) {
                for (int k = 1; k <= ofRank.size(); k++) {
                    options.add(ofRank.subList(0, k));
                }
            } else if (ofRank.size() >= round.pile().setSize()
                    && rank.canFollow(round.pile().topRank().orElseThrow())) {
                options.add(ofRank.subList(0, round.pile().setSize()));
            }
        }
        boolean mayPass = round.canPass(player);
        if (options.isEmpty() || (mayPass && rnd.nextInt(3) == 0)) {
            game.pass(player);
        } else {
            List<Card> move = options.get(rnd.nextInt(options.size()));
            assertTrue(round.isValidPlay(player, move));
            game.play(player, move);
        }
    }

    static void randomGiveBacks(Game game, Random rnd) {
        while (game.phase() == Game.Phase.EXCHANGE) {
            Exchange ex = game.exchange();
            String who = ex.pendingGiveBack().keySet().iterator().next();
            List<Card> hand = new ArrayList<>(ex.hand(who));
            java.util.Collections.shuffle(hand, rnd);
            game.giveBack(who, hand.subList(0, ex.pendingGiveBack(who)));
        }
    }

    static void assertCardsConserved(Game game) {
        Round round = game.round();
        List<Card> all = round.allCards();
        assertEquals(Deck.SIZE - game.removedCards(), all.size(), "lap sem keletkezik, sem eltűnik");
        assertEquals(all.size(), new HashSet<>(all).size(), "nincs duplikált lap");
    }

    static void playRound(Game game, Random rnd) {
        game.startRound();
        randomGiveBacks(game, rnd);
        assertEquals(Game.Phase.PLAYING, game.phase());
        int moves = 0;
        while (game.phase() == Game.Phase.PLAYING) {
            randomLegalMove(game, rnd);
            if (game.phase() == Game.Phase.PLAYING) {
                assertCardsConserved(game);
            }
            if (++moves > 5000) {
                throw new AssertionError("A játék elakadt");
            }
        }
        assertEquals(Game.Phase.BETWEEN_ROUNDS, game.phase());
    }

    @Test
    void firstRoundHasNoRolesAndRandomStarter() {
        Game game = gameWith(7, 1);
        assertEquals(Game.Phase.WAITING, game.phase());
        assertTrue(game.roles().isEmpty());
        game.startRound();
        assertEquals(Game.Phase.PLAYING, game.phase());
        assertNotNull(game.round().currentPlayer());
        assertEquals(14, game.hand("p0").size());
        assertEquals(6, game.removedCards());
    }

    @Test
    void cannotStartWithTooFewPlayers() {
        Game game = gameWith(6, 1);
        assertFalse(game.canStart());
        assertThrows(IllegalStateException.class, game::startRound);
    }

    @Test
    void cannotJoinOrLeaveDuringRound() {
        Game game = gameWith(7, 1);
        game.startRound();
        assertThrows(IllegalStateException.class, () -> game.addPlayer("x"));
        assertThrows(IllegalStateException.class, () -> game.removePlayer("p0"));
    }

    @Test
    void secondRoundSeatsByRolesExchangesAndKingStarts() {
        Random rnd = new Random(7);
        Game game = gameWith(9, 7);
        playRound(game, rnd);

        RoundRecord record = game.lastRound();
        assertEquals(1, record.number());
        assertEquals(9, record.playerCount());
        assertEquals(5, record.removedCards());
        assertEquals(record.finishOrder(), game.seating());
        assertEquals(Role.KING, game.roles().get(game.seating().get(0)));
        assertEquals(Role.SWAMP, game.roles().get(game.seating().get(8)));

        game.startRound();
        assertEquals(Game.Phase.EXCHANGE, game.phase());
        String king = game.seating().get(0);
        assertEquals(3, game.exchange().pendingGiveBack(king));
        assertEquals(14, game.hand(king).size());
        randomGiveBacks(game, rnd);
        assertEquals(Game.Phase.PLAYING, game.phase());
        assertEquals(king, game.round().currentPlayer(), "a király kezd");
        assertEquals(11, game.hand(king).size());
    }

    @Test
    void newPlayerBetweenRoundsBecomesCitizen() {
        Random rnd = new Random(3);
        Game game = gameWith(7, 3);
        playRound(game, rnd);
        game.addPlayer("new");
        assertEquals(8, game.playerCount());
        assertEquals(Role.CITIZEN, game.roles().get("new"));
        assertEquals(4, game.seating().indexOf("new"), "a polgárok végére ül");
        assertEquals(Role.CLEANER, game.roles().get(game.seating().get(5)));
    }

    @Test
    void leavingPlayerShiftsRoles() {
        Random rnd = new Random(5);
        Game game = gameWith(8, 5);
        playRound(game, rnd);
        String king = game.seating().get(0);
        String queen = game.seating().get(1);
        game.removePlayer(king);
        assertEquals(Role.KING, game.roles().get(queen));
        game.removePlayer(queen);
        assertFalse(game.canStart(), "6 játékossal nem indulhat kör");
        assertTrue(game.roles().isEmpty());
    }

    @ParameterizedTest(name = "{0} játékos, sok véletlen játék")
    @ValueSource(ints = {7, 8, 9, 10, 11, 12, 13, 14, 15})
    void thousandsOfRandomGamesNeverStallAndConserveCards(int players) {
        for (int seed = 0; seed < 300; seed++) {
            Random rnd = new Random(seed * 31L + players);
            Game game = gameWith(players, seed);
            for (int roundNo = 0; roundNo < 3; roundNo++) {
                playRound(game, rnd);
                RoundRecord record = game.lastRound();
                assertEquals(players, record.finishOrder().size());
                assertEquals(players, new HashSet<>(record.finishOrder()).size());
                Map<String, Role> roles = record.roles();
                assertEquals(players - 6, roles.values().stream().filter(r -> r == Role.CITIZEN).count());
            }
            assertEquals(3, game.history().size());
        }
    }
}
