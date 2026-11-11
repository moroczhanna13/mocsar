package hu.mocsar.server.room;

import hu.mocsar.core.Card;
import hu.mocsar.core.Exchange;
import hu.mocsar.core.Game;
import hu.mocsar.core.Pile;
import hu.mocsar.core.Play;
import hu.mocsar.core.Role;
import hu.mocsar.core.Round;
import hu.mocsar.core.RoundRecord;
import hu.mocsar.server.api.dto.RoomSummary;
import hu.mocsar.server.api.dto.RoomView;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** A szoba állapotából játékosonkénti nézetet készít. Mások lapjait sosem teszi bele. */
public final class ViewMapper {

    private ViewMapper() {
    }

    public static RoomView view(Room room, String me) {
        Game game = room.game();
        Round round = game.round();
        Exchange exchange = game.exchange();
        boolean playing = game.phase() == Game.Phase.PLAYING && round != null;
        boolean exchanging = game.phase() == Game.Phase.EXCHANGE && exchange != null;
        Map<String, Role> roles = game.roles();
        List<String> finishOrder = playing ? round.finishOrder() : List.of();

        List<RoomView.PlayerInfo> players = new ArrayList<>();
        for (String id : game.seating()) {
            PlayerSession session = room.byId(id).orElseThrow();
            int place = finishOrder.indexOf(id);
            players.add(new RoomView.PlayerInfo(id, session.nickname(), session.bot(),
                    game.hand(id).size(), roles.get(id), place >= 0 ? place + 1 : null,
                    playing && round.hasPassed(id)));
        }

        RoomView.PileInfo pile = null;
        if (playing) {
            Pile p = round.pile();
            pile = new RoomView.PileInfo(
                    p.topRank().map(r -> r.label()).orElse(null),
                    p.setSize(),
                    ids(p.top().map(Play::cards).orElse(List.of())),
                    p.cardCount());
        }

        List<RoomView.TransferInfo> transfers = new ArrayList<>();
        if (exchange != null && (exchanging || playing)) {
            for (Exchange.Transfer t : exchange.transfers()) {
                boolean involved = me.equals(t.from()) || me.equals(t.to());
                transfers.add(new RoomView.TransferInfo(t.from(), t.to(), t.cards().size(),
                        involved ? ids(t.cards()) : null));
            }
        }

        RoundRecord last = game.lastRound();
        RoomView.RoundSummary lastRound = last == null ? null
                : new RoomView.RoundSummary(last.number(), last.finishOrder(), last.roles());

        return new RoomView(
                room.code(),
                game.phase(),
                game.roundNumber(),
                me,
                room.hostId(),
                game.canStart(),
                players,
                playing ? round.currentPlayer() : null,
                ids(game.hasPlayer(me) ? game.hand(me) : List.of()),
                pile,
                playing ? round.lastPlacer() : null,
                finishOrder,
                exchanging ? exchange.pendingGiveBack() : Map.of(),
                exchanging ? exchange.pendingGiveBack(me) : 0,
                transfers,
                lastRound);
    }

    public static RoomSummary summary(Room room) {
        Game game = room.game();
        List<String> nicknames = room.players().stream().map(PlayerSession::nickname).toList();
        boolean canJoin = !room.isClosed()
                && (game.phase() == Game.Phase.WAITING || game.phase() == Game.Phase.BETWEEN_ROUNDS)
                && game.playerCount() < 15;
        return new RoomSummary(room.code(), game.phase(), game.roundNumber(), game.playerCount(), nicknames, canJoin);
    }

    static List<String> ids(List<Card> cards) {
        return cards.stream().map(Card::id).toList();
    }
}
