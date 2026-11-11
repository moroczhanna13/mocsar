package hu.mocsar.server.room;

import org.springframework.http.HttpStatus;

public class RoomException extends RuntimeException {

    private final HttpStatus status;

    public RoomException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    public HttpStatus status() {
        return status;
    }

    public static RoomException notFound(String code) {
        return new RoomException(HttpStatus.NOT_FOUND, "Nincs ilyen szoba: " + code);
    }

    public static RoomException forbidden(String message) {
        return new RoomException(HttpStatus.FORBIDDEN, message);
    }

    public static RoomException conflict(String message) {
        return new RoomException(HttpStatus.CONFLICT, message);
    }

    public static RoomException badRequest(String message) {
        return new RoomException(HttpStatus.BAD_REQUEST, message);
    }
}
