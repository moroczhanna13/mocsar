package hu.mocsar.server.room;

import hu.mocsar.bot.Bot;
import hu.mocsar.core.Game;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Egy szoba: a szabálymotor példánya és a benne ülő játékosok.
 * Minden módosítás a szobán szinkronizálva történik.
 */
public final class Room {

    private final String code;
    private final Long gameId;
    private final Game game;
    private final Map<String, PlayerSession> byToken = new LinkedHashMap<>();
    private final Map<String, PlayerSession> byId = new LinkedHashMap<>();
    private final Map<String, Bot> bots = new LinkedHashMap<>();
    private final AtomicBoolean botPending = new AtomicBoolean();
    private int nextPlayerNo = 1;
    private int persistedRounds;
    private String hostId;
    private boolean closed;

    public Room(String code, Long gameId, Game game) {
        this.code = code;
        this.gameId = gameId;
        this.game = game;
    }

    public String code() {
        return code;
    }

    public Long gameId() {
        return gameId;
    }

    public Game game() {
        return game;
    }

    public String hostId() {
        return hostId;
    }

    public boolean isClosed() {
        return closed;
    }

    void close() {
        closed = true;
    }

    String nextPlayerId() {
        return "p" + nextPlayerNo++;
    }

    void add(PlayerSession session, Bot bot) {
        byToken.put(session.token(), session);
        byId.put(session.id(), session);
        if (bot != null) {
            bots.put(session.id(), bot);
        } else if (hostId == null) {
            hostId = session.id();
        }
    }

    void remove(PlayerSession session) {
        byToken.remove(session.token());
        byId.remove(session.id());
        bots.remove(session.id());
        if (session.id().equals(hostId)) {
            hostId = byId.values().stream().filter(p -> !p.bot()).map(PlayerSession::id).findFirst().orElse(null);
        }
    }

    public Optional<PlayerSession> byToken(String token) {
        return Optional.ofNullable(byToken.get(token));
    }

    public Optional<PlayerSession> byId(String id) {
        return Optional.ofNullable(byId.get(id));
    }

    public Collection<PlayerSession> players() {
        return Collections.unmodifiableCollection(byId.values());
    }

    public Bot bot(String playerId) {
        return bots.get(playerId);
    }

    public boolean isBot(String playerId) {
        return bots.containsKey(playerId);
    }

    public int humanCount() {
        return (int) byId.values().stream().filter(p -> !p.bot()).count();
    }

    int persistedRounds() {
        return persistedRounds;
    }

    void markPersisted(int rounds) {
        persistedRounds = rounds;
    }

    AtomicBoolean botPending() {
        return botPending;
    }
}
