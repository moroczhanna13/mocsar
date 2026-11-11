package hu.mocsar.core;

public enum Suit {
    CLUBS('C'), DIAMONDS('D'), HEARTS('H'), SPADES('S');

    private final char symbol;

    Suit(char symbol) {
        this.symbol = symbol;
    }

    public char symbol() {
        return symbol;
    }

    public static Suit fromSymbol(char symbol) {
        for (Suit s : values()) {
            if (s.symbol == symbol) {
                return s;
            }
        }
        throw new IllegalArgumentException("Ismeretlen szín: " + symbol);
    }
}
