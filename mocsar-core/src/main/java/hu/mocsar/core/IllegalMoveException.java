package hu.mocsar.core;

/** Szabálytalan lépés: nem a játékos jön, nincs nála a lap, kisebb lapot tenne, stb. */
public class IllegalMoveException extends RuntimeException {

    public IllegalMoveException(String message) {
        super(message);
    }
}
