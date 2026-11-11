package hu.mocsar.server.api;

import hu.mocsar.server.api.dto.ActionMessage;
import hu.mocsar.server.room.RoomService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.stereotype.Controller;

/**
 * STOMP üzenetek: {@code /app/rooms/{code}/...}. A válasz mindig a játékos privát témájára megy:
 * siker esetén mindenki új állapotot kap, hiba esetén csak a küldő kap hibaüzenetet.
 */
@Controller
public class GameSocketController {

    private static final Logger log = LoggerFactory.getLogger(GameSocketController.class);

    private final RoomService rooms;

    public GameSocketController(RoomService rooms) {
        this.rooms = rooms;
    }

    @MessageMapping("/rooms/{code}/sync")
    public void sync(@DestinationVariable String code, ActionMessage msg) {
        guarded(code, msg, () -> rooms.sync(code, msg.token()));
    }

    @MessageMapping("/rooms/{code}/start")
    public void start(@DestinationVariable String code, ActionMessage msg) {
        guarded(code, msg, () -> rooms.start(code, msg.token()));
    }

    @MessageMapping("/rooms/{code}/play")
    public void play(@DestinationVariable String code, ActionMessage msg) {
        guarded(code, msg, () -> rooms.play(code, msg.token(), msg.cards()));
    }

    @MessageMapping("/rooms/{code}/pass")
    public void pass(@DestinationVariable String code, ActionMessage msg) {
        guarded(code, msg, () -> rooms.pass(code, msg.token()));
    }

    @MessageMapping("/rooms/{code}/giveBack")
    public void giveBack(@DestinationVariable String code, ActionMessage msg) {
        guarded(code, msg, () -> rooms.giveBack(code, msg.token(), msg.cards()));
    }

    @MessageMapping("/rooms/{code}/addBot")
    public void addBot(@DestinationVariable String code, ActionMessage msg) {
        guarded(code, msg, () -> rooms.addBot(code, msg.token()));
    }

    @MessageMapping("/rooms/{code}/leave")
    public void leave(@DestinationVariable String code, ActionMessage msg) {
        guarded(code, msg, () -> rooms.leave(code, msg.token()));
    }

    private void guarded(String code, ActionMessage msg, Runnable action) {
        try {
            action.run();
        } catch (RuntimeException e) {
            log.debug("Visszautasított lépés a(z) {} szobában: {}", code, e.getMessage());
            rooms.sendError(code, msg == null ? null : msg.token(), e.getMessage());
        }
    }
}
