package hu.mocsar.server.room;

import hu.mocsar.core.RoundRecord;
import hu.mocsar.server.persistence.GameEntity;
import hu.mocsar.server.persistence.GameRepository;
import hu.mocsar.server.persistence.GameStatus;
import hu.mocsar.server.persistence.PlayerEntity;
import hu.mocsar.server.persistence.PlayerRepository;
import hu.mocsar.server.persistence.RoundEntity;
import hu.mocsar.server.persistence.RoundRepository;
import hu.mocsar.server.persistence.RoundResultEntity;
import hu.mocsar.server.persistence.RoundResultRepository;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Az adatbázisba csak a játék, a játékosok és a befejezett körök kerülnek. */
@Service
public class GameRecorder {

    private final GameRepository games;
    private final PlayerRepository players;
    private final RoundRepository rounds;
    private final RoundResultRepository results;

    public GameRecorder(GameRepository games, PlayerRepository players, RoundRepository rounds,
                        RoundResultRepository results) {
        this.games = games;
        this.players = players;
        this.rounds = rounds;
        this.results = results;
    }

    @Transactional
    public Long createGame(String roomCode) {
        return games.save(new GameEntity(roomCode)).getId();
    }

    @Transactional
    public Long addPlayer(Long gameId, String nickname, int joinOrder, boolean bot) {
        GameEntity game = games.getReferenceById(gameId);
        return players.save(new PlayerEntity(nickname, game, joinOrder, bot)).getId();
    }

    @Transactional
    public void setStatus(Long gameId, GameStatus status) {
        GameEntity game = games.findById(gameId).orElseThrow();
        game.setStatus(status);
    }

    /**
     * Egy befejezett kör mentése.
     *
     * @param dbIds játékosazonosító (p1, ...) és a player tábla sora
     */
    @Transactional
    public void recordRound(Long gameId, RoundRecord record, Map<String, Long> dbIds) {
        GameEntity game = games.getReferenceById(gameId);
        RoundEntity round = rounds.save(
                new RoundEntity(game, record.number(), record.playerCount(), record.removedCards()));
        for (int i = 0; i < record.finishOrder().size(); i++) {
            String playerId = record.finishOrder().get(i);
            PlayerEntity player = players.getReferenceById(dbIds.get(playerId));
            results.save(new RoundResultEntity(round, player, i + 1, record.roles().get(playerId)));
        }
    }
}
