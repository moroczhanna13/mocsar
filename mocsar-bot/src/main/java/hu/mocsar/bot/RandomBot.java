package hu.mocsar.bot;

import hu.mocsar.core.Card;
import hu.mocsar.core.Game;
import hu.mocsar.core.PlayerView;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * Véletlen bot: szabályos lépések közül véletlenül választ, és néha akkor is passzol, ha tudna tenni.
 * Szimulációs tesztekhez: minden ágat bejár.
 */
public final class RandomBot implements Bot {

    private final Random random;
    private final double passChance;

    public RandomBot(Random random) {
        this(random, 0.25);
    }

    public RandomBot(Random random, double passChance) {
        this.random = random;
        this.passChance = passChance;
    }

    @Override
    public BotAction decide(PlayerView view) {
        if (view.phase() == Game.Phase.EXCHANGE) {
            if (view.myGiveBack() > 0) {
                List<Card> hand = new ArrayList<>(view.hand());
                Collections.shuffle(hand, random);
                return BotAction.giveBack(hand.subList(0, view.myGiveBack()));
            }
            return BotAction.none();
        }
        if (view.phase() != Game.Phase.PLAYING || !view.isMyTurn()) {
            return BotAction.none();
        }
        List<List<Card>> plays = Moves.legalPlays(view);
        boolean mayPass = !view.pileEmpty();
        if (plays.isEmpty() || (mayPass && random.nextDouble() < passChance)) {
            return BotAction.pass();
        }
        return BotAction.play(plays.get(random.nextInt(plays.size())));
    }
}
