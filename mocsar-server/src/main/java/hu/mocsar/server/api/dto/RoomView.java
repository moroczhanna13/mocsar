package hu.mocsar.server.api.dto;

import hu.mocsar.core.Game;
import hu.mocsar.core.Role;
import java.util.List;
import java.util.Map;

/**
 * Amit egy játékos a szobából lát. A {@code hand} csak a saját lapjait tartalmazza,
 * a többiekről csak a lapszám látszik.
 */
public record RoomView(
        String roomCode,
        Game.Phase phase,
        int roundNumber,
        String me,
        String hostId,
        boolean canStart,
        List<PlayerInfo> players,
        String currentPlayer,
        List<String> hand,
        PileInfo pile,
        String lastPlacer,
        List<String> finishOrder,
        Map<String, Integer> pendingGiveBack,
        int myGiveBack,
        List<TransferInfo> exchange,
        RoundSummary lastRound) {

    /** {@code passed}: ebben a kupacban már passzolt, a lezárásig kimarad. */
    public record PlayerInfo(String id, String nickname, boolean bot, int cardCount, Role role, Integer place,
                             boolean passed) {
    }

    public record PileInfo(String topRank, int setSize, List<String> topCards, int cardCount) {
    }

    /** Lapcsere-átadás. A {@code cards} csak a két érintettnek látszik, másnak null. */
    public record TransferInfo(String from, String to, int count, List<String> cards) {
    }

    public record RoundSummary(int number, List<String> finishOrder, Map<String, Role> roles) {
    }
}
