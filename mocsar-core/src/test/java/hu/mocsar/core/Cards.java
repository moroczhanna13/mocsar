package hu.mocsar.core;

import java.util.ArrayList;
import java.util.List;

/** Tesztsegéd: lapok azonosítóból. */
final class Cards {

    private Cards() {
    }

    static Card c(String id) {
        return Card.parse(id);
    }

    static List<Card> cards(String... ids) {
        List<Card> list = new ArrayList<>();
        for (String id : ids) {
            list.add(Card.parse(id));
        }
        return list;
    }
}
