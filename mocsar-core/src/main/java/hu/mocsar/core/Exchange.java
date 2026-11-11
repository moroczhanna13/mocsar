package hu.mocsar.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Lapcsere az osztás után, az előző kör szerepei alapján.
 *
 * <p>Első lépés (automatikus): a takarítónő a legjobb lapját a hercegnek adja, a cseléd a két
 * legjobbat a királynőnek, a Mocsár a három legjobbat a királynak. A 2-es a legjobb, az ász a második.
 *
 * <p>Második lépés: a király, a királynő és a herceg ugyanannyi tetszőleges lapot ad vissza.
 * A csere akkor teljes, ha mindhárman visszaadtak.
 */
public final class Exchange {

    /** Egy átadás: kitől, kinek, mely lapok. */
    public record Transfer(String from, String to, List<Card> cards) {
        public Transfer {
            cards = List.copyOf(cards);
        }
    }

    private final Map<String, Role> roles;
    private final Map<String, List<Card>> hands;
    private final List<Transfer> transfers = new ArrayList<>();
    private final Map<String, Integer> pendingGiveBack = new LinkedHashMap<>();

    public Exchange(Map<String, Role> roles, Map<String, List<Card>> hands) {
        this.roles = Map.copyOf(roles);
        this.hands = new LinkedHashMap<>();
        hands.forEach((id, hand) -> {
            List<Card> copy = new ArrayList<>(hand);
            Collections.sort(copy);
            this.hands.put(id, copy);
        });
        for (String id : roles.keySet()) {
            if (!this.hands.containsKey(id)) {
                throw new IllegalArgumentException("Nincs kéz a játékoshoz: " + id);
            }
        }
        forcedTransfers();
    }

    private void forcedTransfers() {
        Map<Role, String> byRole = new LinkedHashMap<>();
        roles.forEach((id, role) -> {
            if (role != Role.CITIZEN) {
                byRole.put(role, id);
            }
        });
        for (Role lower : List.of(Role.CLEANER, Role.MAID, Role.SWAMP)) {
            String from = byRole.get(lower);
            String to = byRole.get(lower.partner());
            if (from == null || to == null) {
                continue;
            }
            List<Card> best = bestCards(hands.get(from), lower.cardsGiven());
            move(from, to, best);
            transfers.add(new Transfer(from, to, best));
            pendingGiveBack.put(to, best.size());
        }
    }

    static List<Card> bestCards(List<Card> hand, int count) {
        List<Card> sorted = new ArrayList<>(hand);
        sorted.sort(Collections.reverseOrder());
        return List.copyOf(sorted.subList(0, Math.min(count, sorted.size())));
    }

    private void move(String from, String to, List<Card> cards) {
        hands.get(from).removeAll(cards);
        List<Card> target = hands.get(to);
        target.addAll(cards);
        Collections.sort(target);
    }

    // ---- lekérdezések ----

    public Map<String, Role> roles() {
        return roles;
    }

    public List<Card> hand(String playerId) {
        return Collections.unmodifiableList(hands.get(playerId));
    }

    public Map<String, List<Card>> hands() {
        Map<String, List<Card>> copy = new LinkedHashMap<>();
        hands.forEach((id, hand) -> copy.put(id, List.copyOf(hand)));
        return copy;
    }

    /** Az eddigi átadások: először a kényszerítettek, utána a visszaadások. */
    public List<Transfer> transfers() {
        return Collections.unmodifiableList(transfers);
    }

    /** Kinek hány lapot kell még visszaadnia. */
    public Map<String, Integer> pendingGiveBack() {
        return Collections.unmodifiableMap(pendingGiveBack);
    }

    public int pendingGiveBack(String playerId) {
        return pendingGiveBack.getOrDefault(playerId, 0);
    }

    public boolean isComplete() {
        return pendingGiveBack.isEmpty();
    }

    // ---- visszaadás ----

    public String validateGiveBack(String playerId, List<Card> cards) {
        Integer count = pendingGiveBack.get(playerId);
        if (count == null) {
            return "Neked nem kell lapot visszaadnod";
        }
        if (cards == null || cards.size() != count) {
            return "Pontosan " + count + " lapot kell visszaadni";
        }
        if (new HashSet<>(cards).size() != cards.size()) {
            return "Ugyanaz a lap kétszer szerepel";
        }
        if (!hands.get(playerId).containsAll(cards)) {
            return "Nincs nálad ez a lap";
        }
        return null;
    }

    public void giveBack(String playerId, List<Card> cards) {
        String error = validateGiveBack(playerId, cards);
        if (error != null) {
            throw new IllegalMoveException(error);
        }
        Role role = roles.get(playerId);
        String to = null;
        for (Map.Entry<String, Role> e : roles.entrySet()) {
            if (e.getValue() == role.partner()) {
                to = e.getKey();
            }
        }
        if (to == null) {
            throw new IllegalStateException("Nincs cserepartner: " + playerId);
        }
        move(playerId, to, cards);
        transfers.add(new Transfer(playerId, to, cards));
        pendingGiveBack.remove(playerId);
    }
}
