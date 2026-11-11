package hu.mocsar.server;

import hu.mocsar.core.Card;
import hu.mocsar.core.Rank;
import hu.mocsar.server.api.dto.RoomView;
import java.util.List;
import java.util.Optional;

/** Tesztsegéd: egy emberi játékos egyszerű, szabályos lépése a nézet alapján. */
final class TestMoves {

    private TestMoves() {
    }

    /** A legkisebb szabályos lerakás, vagy üres, ha passzolni kell. */
    static Optional<List<String>> lowestPlay(RoomView view) {
        List<Card> hand = view.hand().stream().map(Card::parse).sorted().toList();
        if (view.pile() == null || view.pile().topRank() == null) {
            Rank lowest = hand.get(0).rank();
            return Optional.of(hand.stream().filter(c -> c.rank() == lowest).map(Card::id).toList());
        }
        Rank top = Rank.fromLabel(view.pile().topRank());
        int size = view.pile().setSize();
        for (Rank rank : Rank.values()) {
            if (!rank.canFollow(top)) {
                continue;
            }
            List<String> ofRank = hand.stream().filter(c -> c.rank() == rank).map(Card::id).toList();
            if (ofRank.size() >= size) {
                return Optional.of(ofRank.subList(0, size));
            }
        }
        return Optional.empty();
    }

    static List<String> weakest(RoomView view, int count) {
        return view.hand().stream().map(Card::parse).sorted().limit(count).map(Card::id).toList();
    }
}
