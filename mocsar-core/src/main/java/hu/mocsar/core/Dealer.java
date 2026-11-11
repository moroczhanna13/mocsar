package hu.mocsar.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Osztás: mindenki 104 / játékosszám lapot kap lefelé kerekítve.
 * A maradékot véletlenszerűen kivesszük, a játékosok nem tudják, mely lapok hiányoznak.
 */
public final class Dealer {

    public static final int MIN_PLAYERS = 7;
    public static final int MAX_PLAYERS = 15;

    private Dealer() {
    }

    public static int cardsPerPlayer(int playerCount) {
        checkPlayerCount(playerCount);
        return Deck.SIZE / playerCount;
    }

    public static int removedCount(int playerCount) {
        return Deck.SIZE - playerCount * cardsPerPlayer(playerCount);
    }

    public static Deal deal(List<String> playerIds, Random random) {
        int n = playerIds.size();
        int perPlayer = cardsPerPlayer(n);
        int removed = removedCount(n);

        List<Card> deck = Deck.fullDoubleDeck();
        Collections.shuffle(deck, random);

        List<Card> removedCards = List.copyOf(deck.subList(0, removed));
        Map<String, List<Card>> hands = new LinkedHashMap<>();
        int offset = removed;
        for (String id : playerIds) {
            List<Card> hand = new ArrayList<>(deck.subList(offset, offset + perPlayer));
            Collections.sort(hand);
            hands.put(id, hand);
            offset += perPlayer;
        }
        return new Deal(hands, removedCards);
    }

    public static void checkPlayerCount(int playerCount) {
        if (playerCount < MIN_PLAYERS || playerCount > MAX_PLAYERS) {
            throw new IllegalArgumentException(
                    "A játékosok száma " + MIN_PLAYERS + " és " + MAX_PLAYERS + " között lehet, nem " + playerCount);
        }
    }
}
