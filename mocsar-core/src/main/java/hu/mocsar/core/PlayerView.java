package hu.mocsar.core;

import java.util.List;
import java.util.Map;

/**
 * Amit egy játékos lát a játékból: a saját lapjai, a többiek lapszáma, a kupac teteje.
 * Mások lapjait sosem tartalmazza. A szerver és a bot is ezt használja.
 *
 * @param playerId        a néző játékos
 * @param phase           a játék fázisa
 * @param roundNumber     a következő vagy aktuális kör száma
 * @param seating         ülésrend (= lépéssorrend)
 * @param hand            a saját kéz, rendezve
 * @param handSizes       minden játékos lapszáma
 * @param roles           szerepek (az első körben üres)
 * @param currentPlayer   aki következik, vagy null
 * @param pileTopRank     a kupac tetejének értéke, vagy null, ha üres
 * @param pileSetSize     hány lapot kell egyszerre tenni (0, ha üres a kupac)
 * @param pileTopCards    a kupac tetején lévő lapok
 * @param pileCardCount   hány lap van a kupacon
 * @param lastPlacer      aki utoljára tett, vagy null
 * @param finishOrder     akik már kifogytak, sorrendben
 * @param pendingGiveBack ki hány lapot ad még vissza a lapcserében
 * @param myGiveBack      nekem hány lapot kell visszaadnom (0, ha nem kell)
 */
public record PlayerView(
        String playerId,
        Game.Phase phase,
        int roundNumber,
        List<String> seating,
        List<Card> hand,
        Map<String, Integer> handSizes,
        Map<String, Role> roles,
        String currentPlayer,
        Rank pileTopRank,
        int pileSetSize,
        List<Card> pileTopCards,
        int pileCardCount,
        String lastPlacer,
        List<String> finishOrder,
        Map<String, Integer> pendingGiveBack,
        int myGiveBack) {

    public static PlayerView of(Game game, String playerId) {
        Round round = game.round();
        Exchange exchange = game.exchange();
        boolean playing = game.phase() == Game.Phase.PLAYING && round != null;
        boolean exchanging = game.phase() == Game.Phase.EXCHANGE && exchange != null;
        Pile pile = playing ? round.pile() : null;
        return new PlayerView(
                playerId,
                game.phase(),
                game.roundNumber(),
                game.seating(),
                game.hasPlayer(playerId) ? game.hand(playerId) : List.of(),
                game.handSizes(),
                game.roles(),
                playing ? round.currentPlayer() : null,
                pile != null ? pile.topRank().orElse(null) : null,
                pile != null ? pile.setSize() : 0,
                pile != null ? pile.top().map(Play::cards).orElse(List.of()) : List.of(),
                pile != null ? pile.cardCount() : 0,
                playing ? round.lastPlacer() : null,
                playing ? round.finishOrder() : List.of(),
                exchanging ? exchange.pendingGiveBack() : Map.of(),
                exchanging ? exchange.pendingGiveBack(playerId) : 0);
    }

    public boolean isMyTurn() {
        return playerId.equals(currentPlayer);
    }

    public boolean pileEmpty() {
        return pileTopRank == null;
    }
}
