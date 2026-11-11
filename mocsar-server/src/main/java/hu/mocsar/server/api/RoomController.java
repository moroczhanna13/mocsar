package hu.mocsar.server.api;

import hu.mocsar.server.api.dto.JoinRequest;
import hu.mocsar.server.api.dto.JoinResponse;
import hu.mocsar.server.api.dto.RoomSummary;
import hu.mocsar.server.api.dto.RoomView;
import hu.mocsar.server.persistence.PlayerStats;
import hu.mocsar.server.persistence.StatsService;
import hu.mocsar.server.room.RoomService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Szobák létrehozása és csatlakozás REST-en; a játék maga WebSocketen megy. */
@RestController
@RequestMapping("/api")
public class RoomController {

    private final RoomService rooms;
    private final StatsService stats;

    public RoomController(RoomService rooms, StatsService stats) {
        this.rooms = rooms;
        this.stats = stats;
    }

    @PostMapping("/rooms")
    @ResponseStatus(HttpStatus.CREATED)
    public JoinResponse create(@Valid @RequestBody JoinRequest request) {
        return rooms.createRoom(request.nickname());
    }

    @PostMapping("/rooms/{code}/join")
    public JoinResponse join(@PathVariable String code, @Valid @RequestBody JoinRequest request) {
        return rooms.join(code.toUpperCase(), request.nickname());
    }

    @GetMapping("/rooms/{code}")
    public RoomSummary summary(@PathVariable String code) {
        return rooms.summary(code.toUpperCase());
    }

    /** A saját nézet REST-en, újracsatlakozáshoz. */
    @GetMapping("/rooms/{code}/me")
    public RoomView me(@PathVariable String code, @RequestHeader("X-Player-Token") String token) {
        return rooms.view(code.toUpperCase(), token);
    }

    @PostMapping("/rooms/{code}/bots")
    public RoomSummary addBot(@PathVariable String code, @RequestHeader("X-Player-Token") String token) {
        rooms.addBot(code.toUpperCase(), token);
        return rooms.summary(code.toUpperCase());
    }

    @PostMapping("/rooms/{code}/leave")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void leave(@PathVariable String code, @RequestHeader("X-Player-Token") String token) {
        rooms.leave(code.toUpperCase(), token);
    }

    @PostMapping("/rooms/{code}/end")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void end(@PathVariable String code, @RequestHeader("X-Player-Token") String token) {
        rooms.endGame(code.toUpperCase(), token);
    }

    @GetMapping("/rooms/{code}/stats")
    public List<PlayerStats> roomStats(@PathVariable String code) {
        return stats.leaderboard(rooms.gameId(code.toUpperCase()));
    }

    @GetMapping("/stats")
    public List<PlayerStats> globalStats() {
        return stats.leaderboard();
    }
}
