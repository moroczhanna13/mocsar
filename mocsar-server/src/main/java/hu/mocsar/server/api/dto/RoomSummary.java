package hu.mocsar.server.api.dto;

import hu.mocsar.core.Game;
import java.util.List;

/** Nyilvános összefoglaló a szobáról: a lobbyhoz, lap nélkül. */
public record RoomSummary(String code, Game.Phase phase, int roundNumber, int playerCount, List<String> nicknames,
                          boolean canJoin) {
}
