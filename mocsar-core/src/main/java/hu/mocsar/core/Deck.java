package hu.mocsar.core;

import java.util.ArrayList;
import java.util.List;

/** Két francia pakli joker nélkül: 104 lap. */
public final class Deck {

    public static final int SIZE = 104;

    private Deck() {
    }

    public static List<Card> fullDoubleDeck() {
        List<Card> cards = new ArrayList<>(SIZE);
        for (int copy = 0; copy < 2; copy++) {
            for (Suit suit : Suit.values()) {
                for (Rank rank : Rank.values()) {
                    cards.add(new Card(rank, suit, copy));
                }
            }
        }
        return cards;
    }
}
