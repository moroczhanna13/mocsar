package hu.mocsar.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/** Az asztalon lévő kupac: a lerakások sorrendben. */
public final class Pile {

    private final List<Play> plays = new ArrayList<>();

    public boolean isEmpty() {
        return plays.isEmpty();
    }

    public Optional<Play> top() {
        return plays.isEmpty() ? Optional.empty() : Optional.of(plays.get(plays.size() - 1));
    }

    public Optional<Rank> topRank() {
        return top().map(Play::rank);
    }

    /** Hány lapot kell egyszerre letenni; üres kupacnál 0 (bármennyi lehet). */
    public int setSize() {
        return top().map(Play::size).orElse(0);
    }

    public List<Play> plays() {
        return Collections.unmodifiableList(plays);
    }

    public List<Card> cards() {
        List<Card> all = new ArrayList<>();
        for (Play p : plays) {
            all.addAll(p.cards());
        }
        return all;
    }

    public int cardCount() {
        int sum = 0;
        for (Play p : plays) {
            sum += p.size();
        }
        return sum;
    }

    void add(Play play) {
        plays.add(play);
    }

    List<Card> clear() {
        List<Card> all = cards();
        plays.clear();
        return all;
    }
}
