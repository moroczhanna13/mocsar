package hu.mocsar.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class RoleTest {

    @ParameterizedTest
    @ValueSource(ints = {7, 8, 9, 10, 11, 12, 13, 14, 15})
    void rolesForEveryPlayerCount(int n) {
        List<Role> roles = Role.forPlayerCount(n);
        assertEquals(n, roles.size());
        assertEquals(Role.KING, roles.get(0));
        assertEquals(Role.QUEEN, roles.get(1));
        assertEquals(Role.PRINCE, roles.get(2));
        assertEquals(Role.CLEANER, roles.get(n - 3));
        assertEquals(Role.MAID, roles.get(n - 2));
        assertEquals(Role.SWAMP, roles.get(n - 1));
        assertEquals(n - 6, Collections.frequency(roles, Role.CITIZEN));
        for (int i = 3; i < n - 3; i++) {
            assertEquals(Role.CITIZEN, roles.get(i));
        }
    }

    @Test
    void sevenPlayersHaveOneCitizen() {
        assertEquals(List.of(Role.KING, Role.QUEEN, Role.PRINCE, Role.CITIZEN, Role.CLEANER, Role.MAID, Role.SWAMP),
                Role.forPlayerCount(7));
    }

    @Test
    void exchangeAmounts() {
        assertEquals(3, Role.KING.cardsReceived());
        assertEquals(2, Role.QUEEN.cardsReceived());
        assertEquals(1, Role.PRINCE.cardsReceived());
        assertEquals(1, Role.CLEANER.cardsGiven());
        assertEquals(2, Role.MAID.cardsGiven());
        assertEquals(3, Role.SWAMP.cardsGiven());
        assertEquals(Role.SWAMP, Role.KING.partner());
        assertEquals(Role.KING, Role.SWAMP.partner());
        assertEquals(Role.PRINCE, Role.CLEANER.partner());
    }

    @Test
    void rejectsTooFewPlayers() {
        assertThrows(IllegalArgumentException.class, () -> Role.forPlayerCount(6));
    }
}
