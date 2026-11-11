package hu.mocsar.core;

import java.util.List;
import java.util.Map;

/** Egy befejezett kör eredménye: ebből készül a ranglista. */
public record RoundRecord(int number, int playerCount, int removedCards, List<String> finishOrder,
                          Map<String, Role> roles) {

    public RoundRecord {
        finishOrder = List.copyOf(finishOrder);
        roles = Map.copyOf(roles);
    }
}
