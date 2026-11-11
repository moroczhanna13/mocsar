package hu.mocsar.bot;

import static org.junit.jupiter.api.Assertions.assertEquals;

import hu.mocsar.core.Card;
import hu.mocsar.core.Game;
import hu.mocsar.core.PlayerView;
import hu.mocsar.core.Rank;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class LowestCardBotTest {

    private static List<Card> cards(String... ids) {
        return java.util.Arrays.stream(ids).map(Card::parse).toList();
    }

    private static PlayerView view(List<Card> hand, Rank top, int setSize) {
        return new PlayerView("me", Game.Phase.PLAYING, 1, List.of("me", "x"), hand,
                Map.of("me", hand.size(), "x", 5), Map.of(), "me", top, setSize,
                List.of(), setSize, top == null ? null : "x", List.of(), Map.of(), 0);
    }

    @Test
    void playsLowestValidCard() {
        BotAction action = new LowestCardBot().decide(view(cards("3C-0", "9H-0", "KS-0"), Rank.EIGHT, 1));
        assertEquals(BotAction.Type.PLAY, action.type());
        assertEquals(cards("9H-0"), action.cards());
    }

    @Test
    void passesWhenNothingFits() {
        BotAction action = new LowestCardBot().decide(view(cards("3C-0", "9H-0"), Rank.TEN, 1));
        assertEquals(BotAction.Type.PASS, action.type());
    }

    @Test
    void opensWithAllCardsOfLowestRank() {
        BotAction action = new LowestCardBot().decide(view(cards("3C-0", "3D-0", "9H-0"), null, 0));
        assertEquals(cards("3C-0", "3D-0"), action.cards());
    }

    @Test
    void matchesSetSize() {
        BotAction action = new LowestCardBot().decide(view(cards("3C-0", "9H-0", "9D-0", "KS-0"), Rank.EIGHT, 2));
        assertEquals(cards("9H-0", "9D-0"), action.cards());
    }

    @Test
    void givesBackWeakestCards() {
        PlayerView view = new PlayerView("me", Game.Phase.EXCHANGE, 2, List.of("me"), cards("2C-0", "3C-0", "7H-0"),
                Map.of("me", 3), Map.of(), null, null, 0, List.of(), 0, null, List.of(), Map.of("me", 2), 2);
        BotAction action = new LowestCardBot().decide(view);
        assertEquals(BotAction.Type.GIVE_BACK, action.type());
        assertEquals(cards("3C-0", "7H-0"), action.cards());
    }
}
