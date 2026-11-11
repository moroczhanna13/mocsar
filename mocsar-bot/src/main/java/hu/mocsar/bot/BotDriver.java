package hu.mocsar.bot;

import hu.mocsar.core.Game;
import hu.mocsar.core.PlayerView;

/** Egy bot döntését végrehajtja a játékon. */
public final class BotDriver {

    private BotDriver() {
    }

    /** Igaz, ha a bot lépett valamit. */
    public static boolean act(Game game, String playerId, Bot bot) {
        PlayerView view = PlayerView.of(game, playerId);
        BotAction action = bot.decide(view);
        switch (action.type()) {
            case PLAY -> game.play(playerId, action.cards());
            case PASS -> game.pass(playerId);
            case GIVE_BACK -> game.giveBack(playerId, action.cards());
            case NONE -> {
                return false;
            }
        }
        return true;
    }
}
