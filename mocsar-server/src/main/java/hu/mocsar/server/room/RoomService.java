package hu.mocsar.server.room;

import hu.mocsar.bot.Bot;
import hu.mocsar.bot.BotDriver;
import hu.mocsar.bot.LowestCardBot;
import hu.mocsar.core.Card;
import hu.mocsar.core.Dealer;
import hu.mocsar.core.Game;
import hu.mocsar.core.IllegalMoveException;
import hu.mocsar.core.RoundRecord;
import hu.mocsar.server.api.dto.JoinResponse;
import hu.mocsar.server.api.dto.RoomSummary;
import hu.mocsar.server.api.dto.RoomView;
import hu.mocsar.server.api.dto.ServerMessage;
import hu.mocsar.server.config.MocsarProperties;
import hu.mocsar.server.persistence.GameStatus;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

/**
 * Szobák kezelése: létrehozás, csatlakozás, lépések ellenőrzése a szabálymotorral,
 * élő állapotküldés minden játékosnak a saját privát témájára, botok léptetése, körök mentése.
 */
@Service
public class RoomService {

    private static final Logger log = LoggerFactory.getLogger(RoomService.class);

    private final RoomRegistry registry;
    private final GameRecorder recorder;
    private final SimpMessagingTemplate messaging;
    private final MocsarProperties properties;
    private final ScheduledExecutorService botExecutor = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "mocsar-bots");
        t.setDaemon(true);
        return t;
    });
    private final SecureRandom secureRandom = new SecureRandom();

    public RoomService(RoomRegistry registry, GameRecorder recorder, SimpMessagingTemplate messaging,
                       MocsarProperties properties) {
        this.registry = registry;
        this.recorder = recorder;
        this.messaging = messaging;
        this.properties = properties;
    }

    public static String topicFor(String code, String token) {
        return "/topic/rooms/" + code + "/players/" + token;
    }

    // ---- szobák ----

    public JoinResponse createRoom(String nickname) {
        String code = registry.newCode(properties.roomCodeLength());
        Long gameId = recorder.createGame(code);
        Room room = new Room(code, gameId, new Game(new Random(secureRandom.nextLong())));
        registry.put(room);
        return join(code, nickname);
    }

    public JoinResponse join(String code, String nickname) {
        Room room = registry.get(code);
        synchronized (room) {
            PlayerSession session = addPlayer(room, nickname, false);
            broadcast(room);
            return new JoinResponse(room.code(), session.id(), session.token());
        }
    }

    public void addBot(String code, String token) {
        Room room = registry.get(code);
        synchronized (room) {
            requireHost(room, token);
            int botNo = (int) room.players().stream().filter(PlayerSession::bot).count() + 1;
            addPlayer(room, "Bot " + botNo, true);
            broadcast(room);
        }
    }

    private PlayerSession addPlayer(Room room, String nickname, boolean bot) {
        String nick = nickname == null ? "" : nickname.trim();
        if (nick.isEmpty() || nick.length() > 32) {
            throw RoomException.badRequest("A becenév 1 és 32 karakter között legyen");
        }
        if (room.isClosed()) {
            throw RoomException.conflict("A szoba bezárt");
        }
        Game game = room.game();
        if (game.phase() == Game.Phase.PLAYING || game.phase() == Game.Phase.EXCHANGE) {
            throw RoomException.conflict("Kör közben nem lehet csatlakozni, várd meg a kör végét");
        }
        if (game.playerCount() >= Dealer.MAX_PLAYERS) {
            throw RoomException.conflict("A szoba tele van (15 játékos)");
        }
        for (PlayerSession p : room.players()) {
            if (p.nickname().equalsIgnoreCase(nick)) {
                throw RoomException.conflict("Ez a becenév már foglalt a szobában");
            }
        }
        String id = room.nextPlayerId();
        int joinOrder = room.players().size() + 1;
        Long dbId = recorder.addPlayer(room.gameId(), nick, joinOrder, bot);
        PlayerSession session = new PlayerSession(id, UUID.randomUUID().toString(), nick, bot, joinOrder, dbId);
        game.addPlayer(id);
        room.add(session, bot ? new LowestCardBot() : null);
        return session;
    }

    public void leave(String code, String token) {
        Room room = registry.get(code);
        synchronized (room) {
            PlayerSession session = requirePlayer(room, token);
            Game game = room.game();
            if (game.phase() == Game.Phase.PLAYING || game.phase() == Game.Phase.EXCHANGE) {
                throw RoomException.conflict("Kör közben nem lehet kilépni");
            }
            game.removePlayer(session.id());
            room.remove(session);
            messaging.convertAndSend(topicFor(code, token), ServerMessage.error("Kiléptél a szobából"));
            if (room.humanCount() == 0) {
                closeRoom(room);
                return;
            }
            broadcast(room);
        }
    }

    public void endGame(String code, String token) {
        Room room = registry.get(code);
        synchronized (room) {
            requireHost(room, token);
            closeRoom(room);
        }
    }

    private void closeRoom(Room room) {
        room.close();
        recorder.setStatus(room.gameId(), GameStatus.FINISHED);
        registry.remove(room.code());
        for (PlayerSession p : room.players()) {
            if (!p.bot()) {
                messaging.convertAndSend(topicFor(room.code(), p.token()), ServerMessage.error("A játék véget ért"));
            }
        }
    }

    // ---- játék ----

    public void start(String code, String token) {
        Room room = registry.get(code);
        synchronized (room) {
            requireHost(room, token);
            Game game = room.game();
            if (!game.canStart()) {
                throw RoomException.conflict("Legalább 7 játékos kell az indításhoz (most " + game.playerCount() + ")");
            }
            game.startRound();
            if (game.roundNumber() == 1) {
                recorder.setStatus(room.gameId(), GameStatus.IN_PROGRESS);
            }
            afterChange(room);
        }
    }

    public void play(String code, String token, List<String> cardIds) {
        Room room = registry.get(code);
        synchronized (room) {
            PlayerSession session = requirePlayer(room, token);
            room.game().play(session.id(), parse(cardIds));
            afterChange(room);
        }
    }

    public void pass(String code, String token) {
        Room room = registry.get(code);
        synchronized (room) {
            PlayerSession session = requirePlayer(room, token);
            room.game().pass(session.id());
            afterChange(room);
        }
    }

    public void giveBack(String code, String token, List<String> cardIds) {
        Room room = registry.get(code);
        synchronized (room) {
            PlayerSession session = requirePlayer(room, token);
            room.game().giveBack(session.id(), parse(cardIds));
            afterChange(room);
        }
    }

    private static List<Card> parse(List<String> ids) {
        if (ids == null) {
            throw new IllegalMoveException("Hiányoznak a lapok");
        }
        List<Card> cards = new ArrayList<>();
        for (String id : ids) {
            try {
                cards.add(Card.parse(id));
            } catch (IllegalArgumentException e) {
                throw new IllegalMoveException("Hibás lapazonosító: " + id);
            }
        }
        return cards;
    }

    // ---- nézetek ----

    public RoomView view(String code, String token) {
        Room room = registry.get(code);
        synchronized (room) {
            PlayerSession session = requirePlayer(room, token);
            return ViewMapper.view(room, session.id());
        }
    }

    public RoomSummary summary(String code) {
        Room room = registry.get(code);
        synchronized (room) {
            return ViewMapper.summary(room);
        }
    }

    public Long gameId(String code) {
        return registry.get(code).gameId();
    }

    /** Minden emberi játékos megkapja a saját nézetét. */
    public void broadcast(Room room) {
        for (PlayerSession p : room.players()) {
            if (!p.bot()) {
                messaging.convertAndSend(topicFor(room.code(), p.token()),
                        ServerMessage.state(ViewMapper.view(room, p.id())));
            }
        }
    }

    public void sendError(String code, String token, String message) {
        if (token != null) {
            messaging.convertAndSend(topicFor(code, token), ServerMessage.error(message));
        }
    }

    public void sync(String code, String token) {
        Room room = registry.get(code);
        synchronized (room) {
            PlayerSession session = requirePlayer(room, token);
            messaging.convertAndSend(topicFor(code, token), ServerMessage.state(ViewMapper.view(room, session.id())));
        }
    }

    // ---- belső ----

    private void afterChange(Room room) {
        persistFinishedRound(room);
        broadcast(room);
        scheduleBots(room);
    }

    private void persistFinishedRound(Room room) {
        Game game = room.game();
        if (game.phase() != Game.Phase.BETWEEN_ROUNDS || room.persistedRounds() >= game.history().size()) {
            return;
        }
        RoundRecord record = game.lastRound();
        Map<String, Long> dbIds = new HashMap<>();
        for (PlayerSession p : room.players()) {
            dbIds.put(p.id(), p.dbId());
        }
        try {
            recorder.recordRound(room.gameId(), record, dbIds);
        } catch (RuntimeException e) {
            log.error("A(z) {} szoba {}. körét nem sikerült menteni", room.code(), record.number(), e);
        }
        room.markPersisted(game.history().size());
    }

    private void scheduleBots(Room room) {
        if (!botHasWork(room)) {
            return;
        }
        if (room.botPending().compareAndSet(false, true)) {
            botExecutor.schedule(() -> runBots(room), properties.botDelayMs(), TimeUnit.MILLISECONDS);
        }
    }

    private boolean botHasWork(Room room) {
        Game game = room.game();
        if (game.phase() == Game.Phase.EXCHANGE) {
            return game.exchange().pendingGiveBack().keySet().stream().anyMatch(room::isBot);
        }
        if (game.phase() == Game.Phase.PLAYING) {
            return room.isBot(game.round().currentPlayer());
        }
        return false;
    }

    private void runBots(Room room) {
        boolean acted = false;
        try {
            synchronized (room) {
                room.botPending().set(false);
                if (!room.isClosed()) {
                    acted = botStep(room);
                    if (acted) {
                        persistFinishedRound(room);
                        broadcast(room);
                    }
                }
            }
        } catch (RuntimeException e) {
            log.error("Bot hiba a(z) {} szobában", room.code(), e);
        }
        if (acted) {
            scheduleBots(room);
        }
    }

    private boolean botStep(Room room) {
        Game game = room.game();
        if (game.phase() == Game.Phase.EXCHANGE) {
            for (String id : List.copyOf(game.exchange().pendingGiveBack().keySet())) {
                Bot bot = room.bot(id);
                if (bot != null) {
                    return BotDriver.act(game, id, bot);
                }
            }
            return false;
        }
        if (game.phase() == Game.Phase.PLAYING) {
            String current = game.round().currentPlayer();
            Bot bot = room.bot(current);
            return bot != null && BotDriver.act(game, current, bot);
        }
        return false;
    }

    private PlayerSession requirePlayer(Room room, String token) {
        return room.byToken(token == null ? "" : token)
                .orElseThrow(() -> new RoomException(HttpStatus.FORBIDDEN, "Érvénytelen játékos-token"));
    }

    private PlayerSession requireHost(Room room, String token) {
        PlayerSession session = requirePlayer(room, token);
        if (!session.id().equals(room.hostId())) {
            throw RoomException.forbidden("Ezt csak a szoba gazdája teheti meg");
        }
        return session;
    }
}
