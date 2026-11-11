package hu.mocsar.core;

import static hu.mocsar.core.Cards.cards;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class RoundTest {

    /** Három játékos kis kezekkel: a szabályok tesztjéhez elég, a Round nem követeli meg a 7 főt. */
    private static Round threePlayers() {
        Map<String, List<Card>> hands = new LinkedHashMap<>();
        hands.put("a", cards("3C-0", "7H-0", "7D-0", "KS-0"));
        hands.put("b", cards("5C-0", "9H-0", "AS-0", "2C-0"));
        hands.put("c", cards("4D-0", "7S-0", "10C-0", "2H-0"));
        return new Round(List.of("a", "b", "c"), hands, "a");
    }

    @Test
    void sameOrHigherIsValidLowerIsNot() {
        Round r = threePlayers();
        r.play("a", cards("7H-0"));
        assertEquals(Rank.SEVEN, r.pile().topRank().orElseThrow());
        assertEquals("b", r.currentPlayer());

        assertFalse(r.isValidPlay("b", cards("5C-0")), "kisebb lap nem tehető");
        assertEquals("Kisebb lapot nem lehet tenni, mint 7", r.validatePlay("b", cards("5C-0")));
        assertThrows(IllegalMoveException.class, () -> r.play("b", cards("5C-0")));

        assertTrue(r.isValidPlay("b", cards("9H-0")));
        r.play("b", cards("9H-0"));
        assertTrue(r.isValidPlay("c", cards("2H-0")));
        assertTrue(r.isValidPlay("c", cards("10C-0")));
        assertFalse(r.isValidPlay("c", cards("7S-0")));
    }

    @Test
    void sameRankIsValid() {
        Round r = threePlayers();
        r.play("a", cards("7H-0"));
        r.play("b", cards("9H-0"));
        r.play("c", cards("10C-0"));
        r.pass("a");
        r.play("b", cards("AS-0"));
        assertTrue(r.isValidPlay("c", cards("2H-0")));
    }

    @Test
    void onlyCurrentPlayerMayMove() {
        Round r = threePlayers();
        assertEquals("Nem te következel", r.validatePlay("b", cards("5C-0")));
        assertThrows(IllegalMoveException.class, () -> r.play("b", cards("5C-0")));
        assertThrows(IllegalMoveException.class, () -> r.pass("b"));
    }

    @Test
    void cannotPlayCardsNotInHand() {
        Round r = threePlayers();
        assertEquals("Nincs nálad ez a lap", r.validatePlay("a", cards("5C-0")));
        assertEquals("Ugyanaz a lap kétszer szerepel", r.validatePlay("a", cards("7H-0", "7H-0")));
    }

    @Test
    void setSizeFollowsTheOpener() {
        Round r = threePlayers();
        r.play("a", cards("7H-0", "7D-0"));
        assertEquals(2, r.pile().setSize());
        assertEquals("Pontosan 2 lapot kell letenni", r.validatePlay("b", cards("AS-0")));
        assertEquals("Csak azonos értékű lapokat lehet együtt letenni", r.validatePlay("b", cards("AS-0", "2C-0")));
        assertTrue(r.canPass("b"));
    }

    @Test
    void passIsAlwaysAllowedOnNonEmptyPileEvenWithValidMove() {
        Round r = threePlayers();
        r.play("a", cards("3C-0"));
        assertTrue(r.isValidPlay("b", cards("5C-0")));
        assertTrue(r.canPass("b"));
        r.pass("b");
        assertEquals("c", r.currentPlayer());
    }

    @Test
    void cannotPassOnEmptyPile() {
        Round r = threePlayers();
        assertFalse(r.canPass("a"));
        assertThrows(IllegalMoveException.class, () -> r.pass("a"));
    }

    @Test
    void pileClosesWhenEveryoneElsePassedAndLastPlacerLeads() {
        Round r = threePlayers();
        r.play("a", cards("3C-0"));
        r.play("b", cards("9H-0"));
        r.pass("c");
        assertEquals(java.util.Set.of("c"), r.passedPlayers());
        assertFalse(r.pile().isEmpty());
        r.pass("a");
        assertTrue(r.pile().isEmpty(), "a kupac lezárult");
        assertEquals("b", r.currentPlayer(), "a legutóbbi lerakó kezd");
        assertEquals(2, r.discarded().size());
        assertNull(r.lastPlacer());
        assertTrue(r.passedPlayers().isEmpty(), "új kupacnál mindenki tehet megint");
        assertFalse(r.canPass("b"));
    }

    @Test
    void whoPassedIsSkippedUntilThePileCloses() {
        Round r = threePlayers();
        r.play("a", cards("3C-0"));
        r.pass("b");
        r.play("c", cards("4D-0"));
        assertEquals("a", r.currentPlayer(), "b passzolt, kimarad");
        r.play("a", cards("7H-0"));
        assertEquals("c", r.currentPlayer(), "b-t megint átugorjuk, pedig lenne nagyobb lapja");
        assertTrue(r.hasPassed("b"));
        assertThrows(IllegalMoveException.class, () -> r.play("b", cards("9H-0")));
        r.pass("c");
        assertTrue(r.pile().isEmpty(), "a-n kívül senki sem tehet, a kupac lezárul");
        assertEquals("a", r.currentPlayer());
        assertFalse(r.hasPassed("b"));
    }

    @Test
    void pileClosesRightAfterPlayWhenEveryoneElseAlreadyPassed() {
        Round r = threePlayers();
        r.play("a", cards("3C-0"));
        r.pass("b");
        r.pass("c");
        assertTrue(r.pile().isEmpty());
        assertEquals("a", r.currentPlayer());
        r.play("a", cards("7H-0"));
        r.play("b", cards("9H-0"));
        r.pass("c");
        r.pass("a");
        assertTrue(r.pile().isEmpty());
        assertEquals("b", r.currentPlayer());
    }

    @Test
    void whenLastPlacerRanOutTheNextPlayerLeads() {
        Map<String, List<Card>> hands = new LinkedHashMap<>();
        hands.put("a", cards("3C-0", "4C-0"));
        hands.put("b", cards("KS-0"));
        hands.put("c", cards("5D-0", "6D-0"));
        hands.put("d", cards("5H-0", "6H-0"));
        Round r = new Round(List.of("a", "b", "c", "d"), hands, "a");

        r.play("a", cards("3C-0"));
        r.play("b", cards("KS-0"));
        assertEquals(List.of("b"), r.finishOrder());
        assertEquals("c", r.currentPlayer());
        r.pass("c");
        r.pass("d");
        assertFalse(r.pile().isEmpty(), "a még nem passzolt");
        r.pass("a");
        assertTrue(r.pile().isEmpty());
        assertEquals("c", r.currentPlayer(), "b kifogyott, az utána ülő c kezd");
    }

    @Test
    void finishedPlayersAreSkippedInTurnOrder() {
        Map<String, List<Card>> hands = new LinkedHashMap<>();
        hands.put("a", cards("3C-0", "4C-0"));
        hands.put("b", cards("KS-0"));
        hands.put("c", cards("5D-0", "6D-0"));
        Round r = new Round(List.of("a", "b", "c"), hands, "a");
        r.play("a", cards("3C-0"));
        r.play("b", cards("KS-0"));
        r.pass("c");
        r.pass("a");
        assertEquals("c", r.currentPlayer());
        r.play("c", cards("5D-0"));
        assertEquals("a", r.currentPlayer(), "b-t átugorjuk");
        r.pass("a");
        assertEquals("c", r.currentPlayer());
    }

    @Test
    void roundEndsWhenOnePlayerRemainsAndFinishOrderGivesRoles() {
        Map<String, List<Card>> hands = new LinkedHashMap<>();
        hands.put("a", cards("3C-0"));
        hands.put("b", cards("KS-0"));
        hands.put("c", cards("5D-0", "6D-0"));
        Round r = new Round(List.of("a", "b", "c"), hands, "a");
        r.play("a", cards("3C-0"));
        assertFalse(r.isFinished());
        r.play("b", cards("KS-0"));
        assertTrue(r.isFinished());
        assertEquals(List.of("a", "b", "c"), r.finishOrder());
        assertNull(r.currentPlayer());
        assertThrows(IllegalMoveException.class, () -> r.play("c", cards("5D-0")));
        assertEquals(2, r.hand("c").size(), "a Mocsár kezében maradnak a lapok");
        assertEquals(4, r.allCards().size());
        assertThrows(IllegalArgumentException.class, r::roles, "3 játékosra nincs szereposztás");
    }

    @Test
    void cardsAreNeverCreatedOrLost() {
        Round r = threePlayers();
        int total = r.allCards().size();
        r.play("a", cards("7H-0", "7D-0"));
        r.pass("b");
        r.pass("c");
        r.play("a", cards("3C-0"));
        r.play("b", cards("5C-0"));
        r.play("c", cards("10C-0"));
        assertEquals(total, r.allCards().size());
        assertEquals(total, new java.util.HashSet<>(r.allCards()).size());
    }
}
