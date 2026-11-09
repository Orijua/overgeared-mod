package com.orijua.overgearedmod.feature.economy;

import com.google.gson.JsonObject;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
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
    }

    @Test
    void aNegativeWithWonBecomesZero() {
        assertEquals(0, new Account(3, 2).withWon(-1).won());
    }

    @Test
    void negativeValuesInJsonBecomeZero() {
        JsonObject json = new JsonObject();
        json.addProperty("won", -40L);
        json.addProperty("debt", -7L);
        assertEquals(new Account(0, 0, DebtState.UNASSIGNED), Account.CODEC.parse(JsonOps.INSTANCE, json).getOrThrow());
    }

    @Test
    void anActiveStateWithoutDebtReadsSettled() {
        JsonObject json = new JsonObject();
        json.addProperty("won", 40L);
        json.addProperty("debt", 0L);
        json.addProperty("debt_state", "active");
        assertEquals(new Account(40, 0, DebtState.SETTLED), Account.CODEC.parse(JsonOps.INSTANCE, json).getOrThrow());
    }

    @Test
    void aSettledStateWithDebtReadsActive() {
        JsonObject json = new JsonObject();
        json.addProperty("won", 40L);
        json.addProperty("debt", 9L);
        json.addProperty("debt_state", "settled");
        assertEquals(new Account(40, 9, DebtState.ACTIVE), Account.CODEC.parse(JsonOps.INSTANCE, json).getOrThrow());
    }

    @Test
    void anUnknownStateKeepsTheWonAndTheDebt() {
        JsonObject owing = new JsonObject();
        owing.addProperty("won", 40L);
        owing.addProperty("debt", 9L);
        owing.addProperty("debt_state", "from_the_future");
        assertEquals(new Account(40, 9, DebtState.ACTIVE), Account.CODEC.parse(JsonOps.INSTANCE, owing).getOrThrow());
        JsonObject free = new JsonObject();
        free.addProperty("won", 40L);
        free.addProperty("debt_state", "from_the_future");
        assertEquals(new Account(40, 0, DebtState.UNASSIGNED), Account.CODEC.parse(JsonOps.INSTANCE, free).getOrThrow());
    }

    @Test
    void theStreamCodecCarriesSettledAndBigValues() {
        for (Account account : new Account[]{new Account(Long.MAX_VALUE, 0, DebtState.SETTLED),
                new Account(Long.MAX_VALUE, Long.MAX_VALUE, DebtState.ACTIVE), new Account(0, 1_500_000_000L, DebtState.ACTIVE), Account.EMPTY}) {
            ByteBuf buf = Unpooled.buffer();
            Account.STREAM_CODEC.encode(buf, account);
            assertEquals(account, Account.STREAM_CODEC.decode(buf));
        }
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

    @Test
    void theStateFollowsTheDebt() {
        assertEquals(DebtState.ACTIVE, new Account(10, 7).debtState());
        assertEquals(DebtState.UNASSIGNED, new Account(0, 0).debtState());
        assertEquals(DebtState.SETTLED, new Account(0, 0, DebtState.ACTIVE).debtState());
        assertEquals(DebtState.ACTIVE, new Account(0, 5, DebtState.SETTLED).debtState());
        assertEquals(DebtState.UNASSIGNED, new Account(1, 0, null).debtState());
        assertEquals(DebtState.SETTLED, new Account(0, 0, DebtState.SETTLED).debtState());
        assertEquals(new Account(0, 0, DebtState.UNASSIGNED), Account.EMPTY);
    }

    @Test
    void withDebtAndWithWonKeepTheState() {
        Account settled = new Account(5, 0, DebtState.SETTLED);
        assertEquals(DebtState.SETTLED, settled.withWon(9).debtState());
        assertEquals(DebtState.ACTIVE, settled.withDebt(3).debtState());
        assertEquals(DebtState.SETTLED, new Account(5, 4, DebtState.ACTIVE).withDebt(0).debtState());
    }

    @Test
    void jsonWithoutAStateReadsItFromTheDebt() {
        JsonObject none = new JsonObject();
        none.addProperty("won", 40L);
        assertEquals(DebtState.UNASSIGNED, Account.CODEC.parse(JsonOps.INSTANCE, none).getOrThrow().debtState());
        JsonObject owing = new JsonObject();
        owing.addProperty("won", 40L);
        owing.addProperty("debt", 40L);
        assertEquals(DebtState.ACTIVE, Account.CODEC.parse(JsonOps.INSTANCE, owing).getOrThrow().debtState());
    }

    @Test
    void aSettledAccountRoundTrips() {
        Account settled = new Account(77, 0, DebtState.SETTLED);
        var json = Account.CODEC.encodeStart(JsonOps.INSTANCE, settled).getOrThrow();
        assertEquals(settled, Account.CODEC.parse(JsonOps.INSTANCE, json).getOrThrow());
    }
}
