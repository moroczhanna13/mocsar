package hu.mocsar.bot;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import hu.mocsar.core.Card;
import hu.mocsar.core.Deck;
import hu.mocsar.core.Game;
import hu.mocsar.core.Role;
import hu.mocsar.core.RoundRecord;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Teljes játékok botokkal, sok ezerszer: a játék sosem akad el, lap sosem keletkezik vagy tűnik el.
 */
class BotSimulationTest {

    private static final int MAX_STEPS_PER_ROUND = 10_000;

    static Map<String, Bot> bots(int players, Random rnd, boolean mixed) {
        Map<String, Bot> bots = new HashMap<>();
        for (int i = 0; i < players; i++) {
            Bot bot = (mixed && i % 2 == 0) ? new LowestCardBot() : new RandomBot(rnd);
            bots.put("p" + i, bot);
        }
        return bots;
    }

    static Game game(Map<String, Bot> bots, long seed) {
        Game game = new Game(new Random(seed));
        bots.keySet().stream().sorted().forEach(game::addPlayer);
        return game;
    }

    static void assertConserved(Game game) {
        List<Card> all = game.round().allCards();
        assertEquals(Deck.SIZE - game.removedCards(), all.size());
        assertEquals(all.size(), new HashSet<>(all).size());
    }

    static RoundRecord playRound(Game game, Map<String, Bot> bots) {
        game.startRound();
        int steps = 0;
        while (game.phase() == Game.Phase.EXCHANGE) {
            for (String id : game.exchange().pendingGiveBack().keySet().stream().toList()) {
                assertTrue(BotDriver.act(game, id, bots.get(id)));
            }
            if (++steps > 10) {
                fail("A lapcsere nem ért véget");
            }
        }
        steps = 0;
        while (game.phase() == Game.Phase.PLAYING) {
            String current = game.round().currentPlayer();
            assertTrue(BotDriver.act(game, current, bots.get(current)), "a botnak lépnie kell, ha ő jön");
            if (game.phase() == Game.Phase.PLAYING) {
                assertConserved(game);
            }
            if (++steps > MAX_STEPS_PER_ROUND) {
                fail("A játék elakadt");
            }
        }
        assertEquals(Game.Phase.BETWEEN_ROUNDS, game.phase());
        return game.lastRound();
    }

    @ParameterizedTest(name = "{0} játékos, vegyes botok")
    @ValueSource(ints = {7, 8, 9, 10, 11, 12, 13, 14, 15})
    void thousandsOfGamesWithBots(int players) {
        int rounds = 0;
        for (int seed = 0; seed < 400; seed++) {
            Random rnd = new Random(seed);
            Map<String, Bot> bots = bots(players, rnd, seed % 2 == 0);
            Game game = game(bots, seed);
            for (int r = 0; r < 3; r++) {
                RoundRecord record = playRound(game, bots);
                assertEquals(players, record.finishOrder().size());
                assertEquals(Role.KING, record.roles().get(record.finishOrder().get(0)));
                assertEquals(Role.SWAMP, record.roles().get(record.finishOrder().get(players - 1)));
                rounds++;
            }
        }
        assertEquals(1200, rounds);
    }

    @Test
    void lowestCardBotsOnlyNeverStall() {
        Map<String, Bot> bots = new HashMap<>();
        for (int i = 0; i < 10; i++) {
            bots.put("p" + i, new LowestCardBot());
        }
        for (int seed = 0; seed < 200; seed++) {
            Game game = game(bots, seed);
            for (int r = 0; r < 5; r++) {
                playRound(game, bots);
            }
            assertEquals(5, game.history().size());
        }
    }

    @Test
    void playersJoiningAndLeavingBetweenRounds() {
        Random rnd = new Random(42);
        Map<String, Bot> bots = bots(7, rnd, true);
        Game game = game(bots, 42);
        playRound(game, bots);
        for (int i = 7; i < 15; i++) {
            bots.put("p" + i, new RandomBot(rnd));
            game.addPlayer("p" + i);
            playRound(game, bots);
            assertEquals(i + 1, game.lastRound().playerCount());
        }
        for (int i = 14; i >= 7; i--) {
            game.removePlayer("p" + i);
            playRound(game, bots);
            assertEquals(i, game.lastRound().playerCount());
        }
    }
}
