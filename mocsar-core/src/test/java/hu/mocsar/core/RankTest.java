package hu.mocsar.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class RankTest {

    @Test
    void threeIsWeakestTwoIsStrongest() {
        assertEquals(Rank.THREE, Rank.values()[0]);
        assertEquals(Rank.TWO, Rank.values()[Rank.values().length - 1]);
        assertTrue(Rank.TWO.strength() > Rank.ACE.strength());
        assertTrue(Rank.ACE.strength() > Rank.KING.strength());
        assertTrue(Rank.TEN.strength() > Rank.NINE.strength());
        assertTrue(Rank.FOUR.strength() > Rank.THREE.strength());
    }

    @Test
    void fullOrder() {
        List<String> labels = List.of("3", "4", "5", "6", "7", "8", "9", "10", "J", "Q", "K", "A", "2");
        for (int i = 0; i < labels.size(); i++) {
            assertEquals(labels.get(i), Rank.values()[i].label());
            assertEquals(Rank.values()[i], Rank.fromLabel(labels.get(i)));
        }
    }

    @Test
    void canFollowSameOrHigher() {
        assertTrue(Rank.SEVEN.canFollow(Rank.SEVEN));
        assertTrue(Rank.EIGHT.canFollow(Rank.SEVEN));
        assertTrue(Rank.TWO.canFollow(Rank.ACE));
        assertFalse(Rank.SIX.canFollow(Rank.SEVEN));
        assertFalse(Rank.ACE.canFollow(Rank.TWO));
    }

    @Test
    void suitDoesNotMatterForStrength() {
        Card heart = new Card(Rank.JACK, Suit.HEARTS, 0);
        Card club = new Card(Rank.JACK, Suit.CLUBS, 1);
        assertEquals(heart.rank().strength(), club.rank().strength());
        assertTrue(heart.rank().canFollow(club.rank()));
        assertTrue(club.rank().canFollow(heart.rank()));
    }

    @Test
    void cardIdRoundTrip() {
        for (Card card : Deck.fullDoubleDeck()) {
            assertEquals(card, Card.parse(card.id()));
        }
        assertEquals("10H-1", new Card(Rank.TEN, Suit.HEARTS, 1).id());
    }
}
