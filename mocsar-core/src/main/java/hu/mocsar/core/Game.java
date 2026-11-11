package hu.mocsar.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Egy játék több körön át: játékosok, ülésrend, szerepek, lapcsere, körök.
 *
 * <p>Fázisok:
 * <ul>
 *   <li>{@code WAITING}: még nincs elég játékos, vagy nem indították el.</li>
 *   <li>{@code EXCHANGE}: osztás után lapcsere (a második körtől).</li>
 *   <li>{@code PLAYING}: a kör zajlik.</li>
 *   <li>{@code BETWEEN_ROUNDS}: a kör véget ért, lehet csatlakozni, kilépni, új kört kezdeni.</li>
 * </ul>
 *
 * <p>Ülésrend: az első körben a csatlakozás sorrendje, utána az előző kör kifogyási sorrendje.
 * Új játékos a polgárok végére ül be. A szerepeket mindig az ülésrendből számoljuk, így ha valaki
 * kilép, a többiek szerepe feljebb csúszik.
 */
public final class Game {

    public enum Phase { WAITING, EXCHANGE, PLAYING, BETWEEN_ROUNDS }

    private final Random random;
    private final List<String> seating = new ArrayList<>();
    private final List<RoundRecord> history = new ArrayList<>();
    private Phase phase = Phase.WAITING;
    private Exchange exchange;
    private Round round;
    private int removedCards;

    public Game(Random random) {
        this.random = random;
    }

    // ---- lekérdezések ----

    public Phase phase() {
        return phase;
    }

    public List<String> seating() {
        return Collections.unmodifiableList(seating);
    }

    public int playerCount() {
        return seating.size();
    }

    public boolean hasPlayer(String id) {
        return seating.contains(id);
    }

    public boolean canStart() {
        return (phase == Phase.WAITING || phase == Phase.BETWEEN_ROUNDS)
                && seating.size() >= Dealer.MIN_PLAYERS && seating.size() <= Dealer.MAX_PLAYERS;
    }

    /** A következő kör száma (1-től). */
    public int roundNumber() {
        return history.size() + 1;
    }

    public List<RoundRecord> history() {
        return Collections.unmodifiableList(history);
    }

    /** Az aktuális szerepek az ülésrendből. Az első kör előtt üres. */
    public Map<String, Role> roles() {
        if (history.isEmpty() || seating.size() < Dealer.MIN_PLAYERS) {
            return Map.of();
        }
        return Round.rolesFor(seating);
    }

    public Exchange exchange() {
        return exchange;
    }

    public Round round() {
        return round;
    }

    public int removedCards() {
        return removedCards;
    }

    /** A játékos keze a lapcsere vagy a játék alatt; egyébként üres. */
    public List<Card> hand(String playerId) {
        return switch (phase) {
            case EXCHANGE -> exchange.hand(playerId);
            case PLAYING -> round.hand(playerId);
            default -> List.of();
        };
    }

    public Map<String, Integer> handSizes() {
        Map<String, Integer> sizes = new LinkedHashMap<>();
        for (String id : seating) {
            sizes.put(id, hand(id).size());
        }
        return sizes;
    }

    // ---- játékosok ----

    public void addPlayer(String id) {
        requireBetweenRounds("csatlakozni");
        if (seating.contains(id)) {
            throw new IllegalArgumentException("Már a játékban van: " + id);
        }
        if (seating.size() >= Dealer.MAX_PLAYERS) {
            throw new IllegalStateException("A játék tele van");
        }
        if (history.isEmpty() || seating.size() < Dealer.MIN_PLAYERS) {
            seating.add(id);
        } else {
            seating.add(seating.size() - 3, id);
        }
    }

    public void removePlayer(String id) {
        requireBetweenRounds("kilépni");
        if (!seating.remove(id)) {
            throw new IllegalArgumentException("Nincs a játékban: " + id);
        }
    }

    private void requireBetweenRounds(String action) {
        if (phase != Phase.WAITING && phase != Phase.BETWEEN_ROUNDS) {
            throw new IllegalStateException("Kör közben nem lehet " + action);
        }
    }

    // ---- körök ----

    public void startRound() {
        if (!canStart()) {
            throw new IllegalStateException("Nem indítható kör: fázis " + phase + ", játékosok " + seating.size());
        }
        Deal deal = Dealer.deal(seating, random);
        removedCards = deal.removed().size();
        Map<String, Role> roles = roles();
        if (roles.isEmpty()) {
            String starter = seating.get(random.nextInt(seating.size()));
            round = new Round(seating, deal.hands(), starter);
            exchange = null;
            phase = Phase.PLAYING;
        } else {
            exchange = new Exchange(roles, deal.hands());
            round = null;
            phase = Phase.EXCHANGE;
            finishExchangeIfComplete();
        }
    }

    public void giveBack(String playerId, List<Card> cards) {
        if (phase != Phase.EXCHANGE) {
            throw new IllegalMoveException("Most nincs lapcsere");
        }
        exchange.giveBack(playerId, cards);
        finishExchangeIfComplete();
    }

    private void finishExchangeIfComplete() {
        if (exchange.isComplete()) {
            round = new Round(seating, exchange.hands(), seating.get(0));
            phase = Phase.PLAYING;
        }
    }

    public void play(String playerId, List<Card> cards) {
        requirePlaying();
        round.play(playerId, cards);
        finishRoundIfOver();
    }

    public void pass(String playerId) {
        requirePlaying();
        round.pass(playerId);
        finishRoundIfOver();
    }

    private void requirePlaying() {
        if (phase != Phase.PLAYING) {
            throw new IllegalMoveException("Most nem lehet lépni: " + phase);
        }
    }

    private void finishRoundIfOver() {
        if (!round.isFinished()) {
            return;
        }
        List<String> order = round.finishOrder();
        history.add(new RoundRecord(history.size() + 1, order.size(), removedCards, order, round.roles()));
        seating.clear();
        seating.addAll(order);
        phase = Phase.BETWEEN_ROUNDS;
    }

    /** Az utolsó befejezett kör, vagy null. */
    public RoundRecord lastRound() {
        return history.isEmpty() ? null : history.get(history.size() - 1);
    }
}
