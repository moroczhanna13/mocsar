package hu.mocsar.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class DealerTest {

    static List<String> players(int n) {
        List<String> ids = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            ids.add("p" + i);
        }
        return ids;
    }

    @Test
    void deckHas104CardsEachAtMostTwice() {
        List<Card> deck = Deck.fullDoubleDeck();
        assertEquals(104, deck.size());
        assertEquals(104, new HashSet<>(deck).size());
        Map<String, Integer> byFace = new HashMap<>();
        for (Card c : deck) {
            byFace.merge(c.rank() + "" + c.suit(), 1, Integer::sum);
        }
        assertEquals(52, byFace.size());
        assertTrue(byFace.values().stream().allMatch(count -> count == 2));
    }

    @ParameterizedTest(name = "{0} játékos: {1} lap főnként, {2} kivéve")
    @CsvSource({
            "7, 14, 6",
            "8, 13, 0",
            "9, 11, 5",
            "10, 10, 4",
            "11, 9, 5",
            "12, 8, 8",
            "13, 8, 0",
            "14, 7, 6",
            "15, 6, 14"
    })
    void dealMatchesTable(int players, int perPlayer, int removed) {
        assertEquals(perPlayer, Dealer.cardsPerPlayer(players));
        assertEquals(removed, Dealer.removedCount(players));

        Deal deal = Dealer.deal(players(players), new Random(players));
        assertEquals(players, deal.hands().size());
        assertEquals(removed, deal.removed().size());
        for (List<Card> hand : deal.hands().values()) {
            assertEquals(perPlayer, hand.size());
        }

        Set<Card> all = new HashSet<>(deal.removed());
        int total = deal.removed().size();
        for (List<Card> hand : deal.hands().values()) {
            all.addAll(hand);
            total += hand.size();
        }
        assertEquals(104, total);
        assertEquals(104, all.size(), "minden lap pontosan egyszer szerepel");
    }

    @Test
    void handsAreSorted() {
        Deal deal = Dealer.deal(players(9), new Random(1));
        for (List<Card> hand : deal.hands().values()) {
            for (int i = 1; i < hand.size(); i++) {
                assertTrue(hand.get(i - 1).compareTo(hand.get(i)) <= 0);
            }
        }
    }

    @Test
    void rejectsWrongPlayerCount() {
        assertThrows(IllegalArgumentException.class, () -> Dealer.deal(players(6), new Random()));
        assertThrows(IllegalArgumentException.class, () -> Dealer.deal(players(16), new Random()));
    }
}
