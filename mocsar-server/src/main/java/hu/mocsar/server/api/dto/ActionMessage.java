package hu.mocsar.server.api.dto;

import java.util.List;

/** Kliens által küldött lépés: a token azonosítja a játékost, a lapok csak lerakásnál és visszaadásnál. */
public record ActionMessage(String token, List<String> cards) {
}
