package hu.mocsar.bot;

import hu.mocsar.core.Card;
import java.util.List;

/** Amit egy bot tehet: lerak, passzol, vagy lapot ad vissza a lapcserében. */
public record BotAction(Type type, List<Card> cards) {

    public enum Type { PLAY, PASS, GIVE_BACK, NONE }

    public BotAction {
        cards = cards == null ? List.of() : List.copyOf(cards);
    }

    public static BotAction play(List<Card> cards) {
        return new BotAction(Type.PLAY, cards);
    }

    public static BotAction pass() {
        return new BotAction(Type.PASS, List.of());
    }

    public static BotAction giveBack(List<Card> cards) {
        return new BotAction(Type.GIVE_BACK, cards);
    }

    /** Nincs tennivaló: nem a bot jön, vagy nem kell visszaadnia. */
    public static BotAction none() {
        return new BotAction(Type.NONE, List.of());
    }
}
