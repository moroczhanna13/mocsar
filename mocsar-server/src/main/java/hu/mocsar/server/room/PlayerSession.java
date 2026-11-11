package hu.mocsar.server.room;

/**
 * Egy játékos egy szobában.
 *
 * @param id        nyilvános azonosító a szobán belül (p1, p2, ...)
 * @param token     titkos azonosító: ezzel lép, és erre a privát témára kapja az állapotot
 * @param nickname  becenév
 * @param bot       gépi játékos-e
 * @param joinOrder csatlakozási sorrend
 * @param dbId      a player tábla sora
 */
public record PlayerSession(String id, String token, String nickname, boolean bot, int joinOrder, Long dbId) {
}
