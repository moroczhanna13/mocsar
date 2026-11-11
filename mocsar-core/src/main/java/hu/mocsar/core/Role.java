package hu.mocsar.core;

import java.util.ArrayList;
import java.util.List;

/**
 * Szerepek a kifogyás sorrendje szerint: király, királynő, herceg, polgárok,
 * takarítónő, cseléd, Mocsár. Legalább 7 játékosnál van értelme.
 */
public enum Role {
    KING(3, 0), QUEEN(2, 0), PRINCE(1, 0), CITIZEN(0, 0), CLEANER(0, 1), MAID(0, 2), SWAMP(0, 3);

    private final int cardsReceived;
    private final int cardsGiven;

    Role(int cardsReceived, int cardsGiven) {
        this.cardsReceived = cardsReceived;
        this.cardsGiven = cardsGiven;
    }

    /** Hány lapot kap a lapcserénél (és ugyanennyit ad vissza). */
    public int cardsReceived() {
        return cardsReceived;
    }

    /** Hány legjobb lapját adja fel a lapcserénél. */
    public int cardsGiven() {
        return cardsGiven;
    }

    /** A cserepartner: ki adja, illetve ki kapja a lapokat. Polgárnak nincs. */
    public Role partner() {
        return switch (this) {
            case KING -> SWAMP;
            case QUEEN -> MAID;
            case PRINCE -> CLEANER;
            case CLEANER -> PRINCE;
            case MAID -> QUEEN;
            case SWAMP -> KING;
            case CITIZEN -> null;
        };
    }

    /** Szerep a helyezés (0-tól) és a játékosszám alapján. */
    public static Role forPlace(int place, int playerCount) {
        Dealer.checkPlayerCount(playerCount);
        if (place < 0 || place >= playerCount) {
            throw new IllegalArgumentException("Hibás helyezés: " + place);
        }
        if (place == 0) {
            return KING;
        }
        if (place == 1) {
            return QUEEN;
        }
        if (place == 2) {
            return PRINCE;
        }
        if (place == playerCount - 1) {
            return SWAMP;
        }
        if (place == playerCount - 2) {
            return MAID;
        }
        if (place == playerCount - 3) {
            return CLEANER;
        }
        return CITIZEN;
    }

    public static List<Role> forPlayerCount(int playerCount) {
        List<Role> roles = new ArrayList<>(playerCount);
        for (int i = 0; i < playerCount; i++) {
            roles.add(forPlace(i, playerCount));
        }
        return roles;
    }
}
