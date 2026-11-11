package hu.mocsar.server.persistence;

/** Ranglista-sor: hány körben játszott, hányszor volt király, hányszor Mocsár. */
public record PlayerStats(String nickname, long rounds, long kings, long swamps) {
}
