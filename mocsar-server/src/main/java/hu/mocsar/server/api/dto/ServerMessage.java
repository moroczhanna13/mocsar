package hu.mocsar.server.api.dto;

/** A privát témára küldött üzenet: állapot vagy hiba. */
public record ServerMessage(String type, RoomView state, String message) {

    public static ServerMessage state(RoomView state) {
        return new ServerMessage("state", state, null);
    }

    public static ServerMessage error(String message) {
        return new ServerMessage("error", null, message);
    }
}
