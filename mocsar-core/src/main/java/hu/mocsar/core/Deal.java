package hu.mocsar.core;

import java.util.List;
import java.util.Map;

/** Egy osztás eredménye: ki milyen lapokat kapott, és melyek maradtak ki. */
public record Deal(Map<String, List<Card>> hands, List<Card> removed) {
}
