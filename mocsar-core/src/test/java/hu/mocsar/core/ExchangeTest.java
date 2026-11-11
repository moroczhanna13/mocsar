package hu.mocsar.core;

import static hu.mocsar.core.Cards.c;
import static hu.mocsar.core.Cards.cards;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ExchangeTest {

    private static Map<String, Role> roles() {
        Map<String, Role> roles = new LinkedHashMap<>();
        roles.put("king", Role.KING);
        roles.put("queen", Role.QUEEN);
        roles.put("prince", Role.PRINCE);
        roles.put("citizen", Role.CITIZEN);
        roles.put("cleaner", Role.CLEANER);
        roles.put("maid", Role.MAID);
        roles.put("swamp", Role.SWAMP);
        return roles;
    }

    private static Map<String, List<Card>> hands() {
        Map<String, List<Card>> hands = new LinkedHashMap<>();
        hands.put("king", cards("3C-0", "4C-0", "5C-0", "6C-0"));
        hands.put("queen", cards("3D-0", "4D-0", "5D-0", "6D-0"));
        hands.put("prince", cards("3H-0", "4H-0", "5H-0", "6H-0"));
        hands.put("citizen", cards("3S-0", "4S-0", "5S-0", "6S-0"));
        hands.put("cleaner", cards("7C-0", "AC-0", "2C-0", "8C-0"));
        hands.put("maid", cards("AD-0", "2D-0", "KD-0", "9D-0"));
        hands.put("swamp", cards("2H-0", "2S-0", "AH-0", "KH-0"));
        return hands;
    }

    @Test
    void lowerRolesGiveTheirBestCardsAndTwoBeatsAce() {
        Exchange ex = new Exchange(roles(), hands());

        assertEquals(cards("3H-0", "4H-0", "5H-0", "6H-0", "2C-0"), ex.hand("prince"));
        assertEquals(cards("7C-0", "8C-0", "AC-0"), ex.hand("cleaner"));

        assertEquals(cards("3D-0", "4D-0", "5D-0", "6D-0", "AD-0", "2D-0"), ex.hand("queen"));
        assertEquals(cards("9D-0", "KD-0"), ex.hand("maid"));

        assertEquals(cards("3C-0", "4C-0", "5C-0", "6C-0", "AH-0", "2H-0", "2S-0"), ex.hand("king"));
        assertEquals(cards("KH-0"), ex.hand("swamp"));

        assertEquals(cards("3S-0", "4S-0", "5S-0", "6S-0"), ex.hand("citizen"));

        assertEquals(3, ex.transfers().size());
        assertEquals(Map.of("prince", 1, "queen", 2, "king", 3), ex.pendingGiveBack());
        assertFalse(ex.isComplete());
    }

    @Test
    void upperRolesGiveBackAnyCardsOfSameCount() {
        Exchange ex = new Exchange(roles(), hands());

        assertEquals("Pontosan 3 lapot kell visszaadni", ex.validateGiveBack("king", cards("3C-0")));
        assertEquals("Neked nem kell lapot visszaadnod", ex.validateGiveBack("citizen", cards("3S-0")));
        assertEquals("Nincs nálad ez a lap", ex.validateGiveBack("prince", cards("3C-0")));
        assertThrows(IllegalMoveException.class, () -> ex.giveBack("king", cards("3C-0")));

        ex.giveBack("king", cards("3C-0", "4C-0", "2S-0"));
        assertEquals(cards("5C-0", "6C-0", "AH-0", "2H-0"), ex.hand("king"));
        assertEquals(cards("3C-0", "4C-0", "KH-0", "2S-0"), ex.hand("swamp"));
        assertEquals(2, ex.pendingGiveBack().size());

        ex.giveBack("queen", cards("3D-0", "4D-0"));
        assertEquals(cards("3D-0", "4D-0", "9D-0", "KD-0"), ex.hand("maid"));

        ex.giveBack("prince", cards("3H-0"));
        assertEquals(cards("3H-0", "7C-0", "8C-0", "AC-0"), ex.hand("cleaner"));

        assertTrue(ex.isComplete());
        assertEquals(6, ex.transfers().size());
        assertThrows(IllegalMoveException.class, () -> ex.giveBack("prince", cards("4H-0")));

        int total = ex.hands().values().stream().mapToInt(List::size).sum();
        assertEquals(28, total);
        for (List<Card> hand : ex.hands().values()) {
            assertEquals(4, hand.size(), "a csere után mindenkinek ugyanannyi lapja van");
        }
    }

    @Test
    void bestCardsPrefersTwoThenAce() {
        List<Card> best = Exchange.bestCards(cards("3C-0", "AC-0", "2C-0", "KC-0", "2D-0"), 3);
        assertTrue(best.contains(c("2C-0")));
        assertTrue(best.contains(c("2D-0")));
        assertTrue(best.contains(c("AC-0")));
    }
}
