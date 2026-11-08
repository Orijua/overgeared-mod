package com.orijua.overgearedmod.feature.economy;

import com.google.gson.JsonObject;
import com.mojang.serialization.JsonOps;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AccountTest {
    @Test
    void emptyIsZeroAndZero() {
        assertEquals(0, Account.EMPTY.won());
        assertEquals(0, Account.EMPTY.debt());
    }

    @Test
    void withWonKeepsTheDebt() {
        Account account = new Account(10, 7).withWon(99);
        assertEquals(99, account.won());
        assertEquals(7, account.debt());
    }

    @Test
    void negativeValuesBecomeZero() {
        Account account = new Account(-5, -1);
        assertEquals(0, account.won());
        assertEquals(0, account.debt());
        assertEquals(0, new Account(3, 2).withWon(-1).won());
    }

    @Test
    void roundTripsThroughJson() {
        Account account = new Account(1234, 50);
        var json = Account.CODEC.encodeStart(JsonOps.INSTANCE, account).getOrThrow();
        assertEquals(account, Account.CODEC.parse(JsonOps.INSTANCE, json).getOrThrow());
    }

    @Test
    void jsonWithoutDebtReadsZeroDebt() {
        JsonObject json = new JsonObject();
        json.addProperty("won", 40L);
        assertEquals(new Account(40, 0), Account.CODEC.parse(JsonOps.INSTANCE, json).getOrThrow());
    }
}
