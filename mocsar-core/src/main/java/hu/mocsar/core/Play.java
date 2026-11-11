package hu.mocsar.core;

import java.util.List;

/** Egy lerakás: ki tette, és milyen (azonos értékű) lapokat. */
public record Play(String playerId, List<Card> cards) {

    public Play {
        cards = List.copyOf(cards);
    }

    public Rank rank() {
        return cards.get(0).rank();
    }

    public int size() {
        return cards.size();
    }
}
