package com.orijua.overgearedmod.feature.capsule;

import com.mojang.serialization.JsonOps;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CapsuleTierTest {
    @Test
    void theCodecRoundTripsEveryTier() {
        for (CapsuleTier tier : CapsuleTier.values()) {
            var json = CapsuleTier.CODEC.encodeStart(JsonOps.INSTANCE, tier).getOrThrow();
            assertEquals(tier, CapsuleTier.CODEC.parse(JsonOps.INSTANCE, json).getOrThrow());
        }
        assertEquals("basic", CapsuleTier.BASIC.getSerializedName());
    }

    @Test
    void anUnknownNameIsRejected() {
        var json = JsonOps.INSTANCE.createString("platinum");
        assertTrue(CapsuleTier.CODEC.parse(JsonOps.INSTANCE, json).error().isPresent());
    }

    @Test
    void previousIsTheTierItIsBuiltFrom() {
        assertEquals(Optional.empty(), CapsuleTier.BASIC.previous());
        assertEquals(Optional.of(CapsuleTier.BASIC), CapsuleTier.ADVANCED.previous());
        assertEquals(Optional.of(CapsuleTier.ADVANCED), CapsuleTier.DIAMOND.previous());
    }

    @Test
    void atLeastFollowsTheOrder() {
        assertTrue(CapsuleTier.DIAMOND.atLeast(CapsuleTier.ADVANCED));
        assertTrue(CapsuleTier.ADVANCED.atLeast(CapsuleTier.ADVANCED));
        assertFalse(CapsuleTier.BASIC.atLeast(CapsuleTier.ADVANCED));
        assertTrue(CapsuleTier.BASIC.atLeast(CapsuleTier.BASIC));
    }
}
