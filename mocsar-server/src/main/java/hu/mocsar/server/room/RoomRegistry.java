package hu.mocsar.server.room;

import java.security.SecureRandom;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

/** A memóriában élő szobák, kóddal. */
@Component
public class RoomRegistry {

    /** Nincs 0/O, 1/I: a kódot diktálni is lehessen. */
    private static final String ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";

    private final Map<String, Room> rooms = new ConcurrentHashMap<>();
    private final SecureRandom random = new SecureRandom();

    public String newCode(int length) {
        while (true) {
            StringBuilder sb = new StringBuilder(length);
            for (int i = 0; i < length; i++) {
                sb.append(ALPHABET.charAt(random.nextInt(ALPHABET.length())));
            }
            String code = sb.toString();
            if (!rooms.containsKey(code)) {
                return code;
            }
        }
    }

    public void put(Room room) {
        rooms.put(room.code(), room);
    }

    public Optional<Room> find(String code) {
        return Optional.ofNullable(rooms.get(code == null ? "" : code.toUpperCase()));
    }

    public Room get(String code) {
        return find(code).orElseThrow(() -> RoomException.notFound(code));
    }

    public void remove(String code) {
        rooms.remove(code);
    }

    public Collection<Room> all() {
        return rooms.values();
    }
}
