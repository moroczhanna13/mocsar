package hu.mocsar.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Egy kör: az osztástól addig, amíg egy kivételével mindenkinek elfogy a lapja.
 *
 * <p>Szabályok:
 * <ul>
 *   <li>Annyi azonos értékű lapot kell letenni, amennyivel a kupacot kezdték. Üres kupacra 1 vagy több mehet.</li>
 *   <li>A letett lap értéke ugyanakkora vagy nagyobb, mint a kupac teteje.</li>
 *   <li>Passzolni mindig lehet, ha van kupac. Üres kupacra a kezdőnek tennie kell.
 *       Aki passzolt, a kupac lezárásáig kimarad, akkor is, ha később tudna tenni.</li>
 *   <li>A kupac lezárul, ha a legutóbbi lerakón kívül már senki sem tehet (mindenki más passzolt
 *       vagy kifogyott). Az új kupacot a legutóbbi lerakó kezdi; ha neki elfogyott a lapja,
 *       a sorban utána következő.</li>
 *   <li>Akinek elfogy a lapja, kiesik a lépéssorrendből. Az utolsó, akinél marad lap, a Mocsár.</li>
 * </ul>
 */
public final class Round {

    private final List<String> seating;
    private final Map<String, List<Card>> hands;
    private final Pile pile = new Pile();
    private final List<Card> discarded = new ArrayList<>();
    private final List<String> finishOrder = new ArrayList<>();

    private final Set<String> passed = new HashSet<>();

    private String currentPlayer;
    private String lastPlacer;
    private boolean finished;

    public Round(List<String> seating, Map<String, List<Card>> hands, String starter) {
        if (seating.size() < 2) {
            throw new IllegalArgumentException("Legalább két játékos kell");
        }
        if (new HashSet<>(seating).size() != seating.size()) {
            throw new IllegalArgumentException("Ismétlődő játékosazonosító");
        }
        if (!seating.contains(starter)) {
            throw new IllegalArgumentException("A kezdő játékos nincs az ülésrendben: " + starter);
        }
        this.seating = List.copyOf(seating);
        this.hands = new LinkedHashMap<>();
        for (String id : seating) {
            List<Card> hand = hands.get(id);
            if (hand == null || hand.isEmpty()) {
                throw new IllegalArgumentException("Üres kéz: " + id);
            }
            List<Card> copy = new ArrayList<>(hand);
            Collections.sort(copy);
            this.hands.put(id, copy);
        }
        this.currentPlayer = starter;
    }

    // ---- lekérdezések ----

    public List<String> seating() {
        return seating;
    }

    public String currentPlayer() {
        return finished ? null : currentPlayer;
    }

    public String lastPlacer() {
        return lastPlacer;
    }

    /** Akik ebben a kupacban már passzoltak: a lezárásig nem tehetnek. */
    public Set<String> passedPlayers() {
        return Collections.unmodifiableSet(passed);
    }

    public boolean hasPassed(String playerId) {
        return passed.contains(playerId);
    }

    public Pile pile() {
        return pile;
    }

    public List<Card> hand(String playerId) {
        List<Card> hand = hands.get(playerId);
        if (hand == null) {
            throw new IllegalArgumentException("Ismeretlen játékos: " + playerId);
        }
        return Collections.unmodifiableList(hand);
    }

    public Map<String, List<Card>> hands() {
        Map<String, List<Card>> copy = new LinkedHashMap<>();
        hands.forEach((id, hand) -> copy.put(id, List.copyOf(hand)));
        return copy;
    }

    public Map<String, Integer> handSizes() {
        Map<String, Integer> sizes = new LinkedHashMap<>();
        hands.forEach((id, hand) -> sizes.put(id, hand.size()));
        return sizes;
    }

    /** A lezárt kupacok lapjai. */
    public List<Card> discarded() {
        return Collections.unmodifiableList(discarded);
    }

    public List<String> finishOrder() {
        return Collections.unmodifiableList(finishOrder);
    }

    public boolean isFinished() {
        return finished;
    }

    public boolean isActive(String playerId) {
        return !hands.get(playerId).isEmpty();
    }

    public List<String> activePlayers() {
        List<String> active = new ArrayList<>();
        for (String id : seating) {
            if (isActive(id)) {
                active.add(id);
            }
        }
        return active;
    }

    /** Szerepek a kifogyás sorrendje alapján; csak befejezett körre. */
    public Map<String, Role> roles() {
        if (!finished) {
            throw new IllegalStateException("A kör még tart");
        }
        return rolesFor(finishOrder);
    }

    public static Map<String, Role> rolesFor(List<String> order) {
        Map<String, Role> roles = new LinkedHashMap<>();
        for (int i = 0; i < order.size(); i++) {
            roles.put(order.get(i), Role.forPlace(i, order.size()));
        }
        return roles;
    }

    // ---- érvényesség ----

    /** Null, ha a lépés szabályos, egyébként a hiba oka. */
    public String validatePlay(String playerId, List<Card> cards) {
        if (finished) {
            return "A kör véget ért";
        }
        if (!playerId.equals(currentPlayer)) {
            return "Nem te következel";
        }
        if (cards == null || cards.isEmpty()) {
            return "Legalább egy lapot le kell tenni";
        }
        if (new HashSet<>(cards).size() != cards.size()) {
            return "Ugyanaz a lap kétszer szerepel";
        }
        List<Card> hand = hands.get(playerId);
        if (!hand.containsAll(cards)) {
            return "Nincs nálad ez a lap";
        }
        Rank rank = cards.get(0).rank();
        for (Card c : cards) {
            if (c.rank() != rank) {
                return "Csak azonos értékű lapokat lehet együtt letenni";
            }
        }
        if (!pile.isEmpty()) {
            if (cards.size() != pile.setSize()) {
                return "Pontosan " + pile.setSize() + " lapot kell letenni";
            }
            if (!rank.canFollow(pile.topRank().orElseThrow())) {
                return "Kisebb lapot nem lehet tenni, mint " + pile.topRank().orElseThrow().label();
            }
        }
        return null;
    }

    public boolean isValidPlay(String playerId, List<Card> cards) {
        return validatePlay(playerId, cards) == null;
    }

    public String validatePass(String playerId) {
        if (finished) {
            return "A kör véget ért";
        }
        if (!playerId.equals(currentPlayer)) {
            return "Nem te következel";
        }
        if (pile.isEmpty()) {
            return "Üres kupacra tenni kell, passzolni nem lehet";
        }
        return null;
    }

    public boolean canPass(String playerId) {
        return validatePass(playerId) == null;
    }

    // ---- lépések ----

    public void play(String playerId, List<Card> cards) {
        String error = validatePlay(playerId, cards);
        if (error != null) {
            throw new IllegalMoveException(error);
        }
        List<Card> hand = hands.get(playerId);
        hand.removeAll(cards);
        pile.add(new Play(playerId, cards));
        lastPlacer = playerId;

        if (hand.isEmpty()) {
            finishOrder.add(playerId);
        }
        List<String> active = activePlayers();
        if (active.size() == 1) {
            finishOrder.add(active.get(0));
            discarded.addAll(pile.clear());
            finished = true;
            currentPlayer = null;
            return;
        }
        advanceOrClose(playerId);
    }

    public void pass(String playerId) {
        String error = validatePass(playerId);
        if (error != null) {
            throw new IllegalMoveException(error);
        }
        passed.add(playerId);
        advanceOrClose(playerId);
    }

    /**
     * A következő olyan játékos jön, aki még játékban van és ebben a kupacban nem passzolt.
     * Ha ilyen nincs a legutóbbi lerakón kívül, a kupac lezárul.
     */
    private void advanceOrClose(String playerId) {
        String next = nextEligibleAfter(playerId);
        if (next == null || next.equals(lastPlacer)) {
            closePile();
        } else {
            currentPlayer = next;
        }
    }

    private void closePile() {
        discarded.addAll(pile.clear());
        passed.clear();
        currentPlayer = isActive(lastPlacer) ? lastPlacer : nextActiveAfter(lastPlacer);
        lastPlacer = null;
    }

    private String nextActiveAfter(String playerId) {
        int start = seating.indexOf(playerId);
        for (int step = 1; step <= seating.size(); step++) {
            String candidate = seating.get((start + step) % seating.size());
            if (isActive(candidate)) {
                return candidate;
            }
        }
        throw new IllegalStateException("Nincs aktív játékos");
    }

    /** A következő, aki tehet még ebbe a kupacba (nem fogyott ki, nem passzolt), vagy null. */
    private String nextEligibleAfter(String playerId) {
        int start = seating.indexOf(playerId);
        for (int step = 1; step <= seating.size(); step++) {
            String candidate = seating.get((start + step) % seating.size());
            if (isActive(candidate) && !passed.contains(candidate)) {
                return candidate;
            }
        }
        return null;
    }

    /** Minden lap, ami a körben van: kezek + kupac + lezárt kupacok. Teszteléshez. */
    public List<Card> allCards() {
        List<Card> all = new ArrayList<>();
        hands.values().forEach(all::addAll);
        all.addAll(pile.cards());
        all.addAll(discarded);
        return all;
    }

    Set<String> playerIds() {
        return hands.keySet();
    }
}
