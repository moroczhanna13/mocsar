package hu.mocsar.bot;

import hu.mocsar.core.PlayerView;

/**
 * Gépi játékos. Csak a saját nézetét kapja meg, mások lapjait nem látja.
 * A visszaadott lépésnek szabályosnak kell lennie.
 */
public interface Bot {

    BotAction decide(PlayerView view);
}
