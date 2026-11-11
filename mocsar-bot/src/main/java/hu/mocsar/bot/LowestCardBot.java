package hu.mocsar.bot;

import hu.mocsar.core.Card;
import hu.mocsar.core.Game;
import hu.mocsar.core.PlayerView;
import java.util.List;

/**
 * Egyszerű bot: mindig a legkisebb szabályos lapot (csoportot) teszi le, ha nem tud, passzol.
 * Üres kupacra a leggyengébb értékből az összeset leteszi. A lapcserében a leggyengébb lapjait adja vissza.
 */
public final class LowestCardBot implements Bot {

    @Override
    public BotAction decide(PlayerView view) {
        if (view.phase() == Game.Phase.EXCHANGE) {
            if (view.myGiveBack() > 0) {
                return BotAction.giveBack(Moves.weakest(view.hand(), view.myGiveBack()));
            }
            return BotAction.none();
        }
        if (view.phase() != Game.Phase.PLAYING || !view.isMyTurn()) {
            return BotAction.none();
        }
        List<List<Card>> plays = Moves.legalPlays(view);
        if (plays.isEmpty()) {
            return BotAction.pass();
        }
        if (view.pileEmpty()) {
            // a leggyengébb értékből a legtöbb lap
            List<Card> best = plays.get(0);
            for (List<Card> p : plays) {
                if (p.get(0).rank() != best.get(0).rank()) {
                    break;
                }
                best = p;
            }
            return BotAction.play(best);
        }
        return BotAction.play(plays.get(0));
    }
}
