package com.orijua.overgearedmod.feature.capsule;

import com.mojang.serialization.JsonOps;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.world.level.Level;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CapsuleRulesTest {
    private static final BlockPos BAIRAN = new BlockPos(10, 70, 20);
    private static final BlockPos SAVED = new BlockPos(-300, 80, 512);

    @Test
    void theFirstConnectionLandsInBairan() {
        CapsuleRules.Spawn spawn = CapsuleRules.spawn(Optional.empty(), Optional.of(BAIRAN)).orElseThrow();
        assertEquals(BAIRAN, spawn.pos());
        assertEquals(CapsuleRules.Source.BAIRAN, spawn.source());
    }

    @Test
    void laterConnectionsStartWhereYouLastDisconnected() {
        CapsuleRules.Spawn spawn = CapsuleRules.spawn(Optional.of(SAVED), Optional.of(BAIRAN)).orElseThrow();
        assertEquals(SAVED, spawn.pos());
        assertEquals(CapsuleRules.Source.LAST_SAVE, spawn.source());
    }

    @Test
    void aSavedSpotWorksEvenWithoutBairan() {
        assertEquals(CapsuleRules.Source.LAST_SAVE, CapsuleRules.spawn(Optional.of(SAVED), Optional.empty()).orElseThrow().source());
    }

    @Test
    void withNeitherThereIsNowhereToAppear() {
        assertTrue(CapsuleRules.spawn(Optional.empty(), Optional.empty()).isEmpty());
    }

    @Test
    void inReachStopsAtMaxDistance() {
        assertTrue(CapsuleRules.inReach(CapsuleRules.MAX_DISTANCE * CapsuleRules.MAX_DISTANCE));
        assertFalse(CapsuleRules.inReach(CapsuleRules.MAX_DISTANCE * CapsuleRules.MAX_DISTANCE + 0.01));
    }

    @Test
    void stateRoundTripsThroughItsCodec() {
        GlobalPos capsule = GlobalPos.of(Level.OVERWORLD, new BlockPos(1, 64, 2));
        GlobalPos exit = GlobalPos.of(Level.OVERWORLD, new BlockPos(1, 64, 3));
        CapsuleState connected = CapsuleState.EMPTY.connected(capsule, exit);
        assertEquals(connected, roundTrip(connected));
        CapsuleState back = connected.disconnected(SAVED);
        assertEquals(back, roundTrip(back));
        assertEquals(Optional.of(SAVED), back.lastSave());
        assertTrue(back.capsule().isEmpty() && back.exit().isEmpty(), "the body is back in the real world");
        assertEquals(CapsuleState.EMPTY, roundTrip(CapsuleState.EMPTY));
    }

    @Test
    void connectingKeepsTheLastSave() {
        CapsuleState state = CapsuleState.EMPTY.disconnected(SAVED)
                .connected(GlobalPos.of(Level.OVERWORLD, BlockPos.ZERO), GlobalPos.of(Level.OVERWORLD, BlockPos.ZERO.above()));
        assertEquals(Optional.of(SAVED), state.lastSave());
    }

    @Test
    void withoutBodyKeepsTheLastSave() {
        GlobalPos capsule = GlobalPos.of(Level.OVERWORLD, BlockPos.ZERO);
        GlobalPos exit = GlobalPos.of(Level.OVERWORLD, BlockPos.ZERO.above());
        CapsuleState freed = CapsuleState.EMPTY.disconnected(SAVED).connected(capsule, exit).withoutBody();
        assertTrue(freed.capsule().isEmpty() && freed.exit().isEmpty());
        assertEquals(Optional.of(SAVED), freed.lastSave());
        assertEquals(CapsuleState.EMPTY, CapsuleState.EMPTY.connected(capsule, exit).withoutBody());
    }

    @Test
    void withoutBodyRoundTripsThroughItsCodec() {
        CapsuleState freed = CapsuleState.EMPTY.disconnected(SAVED)
                .connected(GlobalPos.of(Level.OVERWORLD, BlockPos.ZERO), GlobalPos.of(Level.OVERWORLD, BlockPos.ZERO.above()))
                .withoutBody();
        assertEquals(freed, roundTrip(freed));
    }

    @Test
    void isConnectedFollowsTheCapsule() {
        GlobalPos spot = GlobalPos.of(Level.OVERWORLD, BlockPos.ZERO);
        assertFalse(CapsuleState.EMPTY.isConnected());
        assertFalse(CapsuleState.EMPTY.disconnected(SAVED).isConnected());
        assertTrue(CapsuleState.EMPTY.connected(spot, spot).isConnected());
    }

    @Test
    void coherenceCoversTheFourCases() {
        GlobalPos spot = GlobalPos.of(Level.OVERWORLD, BlockPos.ZERO);
        CapsuleState connected = CapsuleState.EMPTY.connected(spot, spot);
        assertEquals(CapsuleRules.Coherence.OK, CapsuleRules.coherence(connected, true));
        assertEquals(CapsuleRules.Coherence.CONNECTED_OUTSIDE, CapsuleRules.coherence(connected, false));
        assertEquals(CapsuleRules.Coherence.DISCONNECTED_INSIDE, CapsuleRules.coherence(CapsuleState.EMPTY, true));
        assertEquals(CapsuleRules.Coherence.OK, CapsuleRules.coherence(CapsuleState.EMPTY, false));
    }

    @Test
    void theClientScreenFollowsTheServerWait() {
        try {
            ClientCapsule.connecting(0, 1_000);
            assertEquals(-1.0f, ClientCapsule.fraction(1_000), "nothing connecting");
            ClientCapsule.connecting(-5, 1_000);
            assertEquals(-1.0f, ClientCapsule.fraction(1_000), "negative ticks mean nothing connecting");
            long duration = CapsuleRules.CONNECT_TICKS * 50L;
            ClientCapsule.connecting(CapsuleRules.CONNECT_TICKS, 10_000);
            assertEquals(0.0f, ClientCapsule.fraction(9_000), 1e-6, "before the start: 0");
            assertEquals(0.0f, ClientCapsule.fraction(10_000), 1e-6);
            assertEquals(0.5f, ClientCapsule.fraction(10_000 + duration / 2), 1e-3);
            assertEquals(1.0f, ClientCapsule.fraction(10_000 + duration + 100), 1e-6);
            assertEquals(1.0f, ClientCapsule.fraction(10_000 + duration + 1_000), 1e-6, "last moment the screen stays at 1");
            assertEquals(-1.0f, ClientCapsule.fraction(10_000 + duration + 1_001), "a lost packet must not leave the screen up");
        } finally {
            ClientCapsule.connecting(0, 0);
        }
    }

    @Test
    void onlyTheTwoCrossingsWithoutPermissionAreBlocked() {
        assertFalse(CapsuleRules.crossingBlocked(false, false, false), "real to real");
        assertFalse(CapsuleRules.crossingBlocked(true, true, false), "Satisfy to Satisfy");
        assertTrue(CapsuleRules.crossingBlocked(false, true, false), "real to Satisfy");
        assertTrue(CapsuleRules.crossingBlocked(true, false, false), "Satisfy to real");
        assertFalse(CapsuleRules.crossingBlocked(false, false, true));
        assertFalse(CapsuleRules.crossingBlocked(true, true, true));
        assertFalse(CapsuleRules.crossingBlocked(false, true, true), "allowed: the capsule");
        assertFalse(CapsuleRules.crossingBlocked(true, false, true), "allowed: the capsule");
    }

    private static CapsuleState roundTrip(CapsuleState state) {
        var json = CapsuleState.CODEC.encodeStart(JsonOps.INSTANCE, state).getOrThrow();
        return CapsuleState.CODEC.parse(JsonOps.INSTANCE, json).getOrThrow();
    }
}
