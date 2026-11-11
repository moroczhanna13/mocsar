package hu.mocsar.bot;

import hu.mocsar.core.Card;
import hu.mocsar.core.PlayerView;
import hu.mocsar.core.Rank;
import java.util.ArrayList;
import java.util.List;

/** Szabályos lerakások felsorolása egy nézetből. */
public final class Moves {

    private Moves() {
    }

    /**
     * Minden szabályos lerakás, gyenge laptól az erős felé.
     * Üres kupacnál minden értékből minden darabszám; egyébként csak a kupac méretével egyező,
     * ugyanakkora vagy nagyobb értékű csoportok.
     */
    public static List<List<Card>> legalPlays(PlayerView view) {
        List<List<Card>> plays = new ArrayList<>();
        for (Rank rank : Rank.values()) {
            List<Card> ofRank = view.hand().stream().filter(c -> c.rank() == rank).toList();
            if (ofRank.isEmpty()) {
                continue;
            }
            if (view.pileEmpty()) {
                for (int k = 1; k <= ofRank.size(); k++) {
                    plays.add(ofRank.subList(0, k));
                }
            } else if (ofRank.size() >= view.pileSetSize() && rank.canFollow(view.pileTopRank())) {
                plays.add(ofRank.subList(0, view.pileSetSize()));
            }
        }
        return plays;
    }

    /** A kéz leggyengébb {@code count} lapja. */
    public static List<Card> weakest(List<Card> hand, int count) {
        List<Card> sorted = new ArrayList<>(hand);
        sorted.sort(null);
        return List.copyOf(sorted.subList(0, Math.min(count, sorted.size())));
    }
}
