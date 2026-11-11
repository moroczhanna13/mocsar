package hu.mocsar.core;

import java.util.Comparator;

/**
 * Egy lap a két pakliból. A {@code copy} (0 vagy 1) különbözteti meg a két azonos lapot.
 * Azonosító formátuma: {@code <érték><szín>-<copy>}, pl. {@code 10H-1}.
 */
public record Card(Rank rank, Suit suit, int copy) implements Comparable<Card> {

    private static final Comparator<Card> ORDER = Comparator
            .comparing(Card::rank)
            .thenComparing(Card::suit)
            .thenComparingInt(Card::copy);

    public Card {
        if (copy != 0 && copy != 1) {
            throw new IllegalArgumentException("A copy csak 0 vagy 1 lehet: " + copy);
        }
    }

    public String id() {
        return rank.label() + suit.symbol() + "-" + copy;
    }

    public static Card parse(String id) {
        int dash = id.lastIndexOf('-');
        if (dash < 2 || dash == id.length() - 1) {
            throw new IllegalArgumentException("Hibás lapazonosító: " + id);
        }
        String left = id.substring(0, dash);
        Rank rank = Rank.fromLabel(left.substring(0, left.length() - 1));
        Suit suit = Suit.fromSymbol(left.charAt(left.length() - 1));
        int copy = Integer.parseInt(id.substring(dash + 1));
        return new Card(rank, suit, copy);
    }

    @Override
    public int compareTo(Card other) {
        return ORDER.compare(this, other);
    }

    @Override
    public String toString() {
        return id();
    }
}
