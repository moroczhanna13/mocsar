package hu.mocsar.core;

/**
 * Lapértékek erősségi sorrendben: a 3-as a leggyengébb, a 2-es a legerősebb.
 * Az enum sorrendje (ordinal) adja az erősséget, a szín nem számít.
 */
public enum Rank {
    THREE("3"), FOUR("4"), FIVE("5"), SIX("6"), SEVEN("7"), EIGHT("8"), NINE("9"), TEN("10"),
    JACK("J"), QUEEN("Q"), KING("K"), ACE("A"), TWO("2");

    private final String label;

    Rank(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }

    /** Minél nagyobb, annál erősebb a lap. */
    public int strength() {
        return ordinal();
    }

    /** Igaz, ha ez a lap rátehető a másikra (ugyanakkora vagy nagyobb). */
    public boolean canFollow(Rank other) {
        return ordinal() >= other.ordinal();
    }

    public static Rank fromLabel(String label) {
        for (Rank r : values()) {
            if (r.label.equals(label)) {
                return r;
            }
        }
        throw new IllegalArgumentException("Ismeretlen lapérték: " + label);
    }
}
