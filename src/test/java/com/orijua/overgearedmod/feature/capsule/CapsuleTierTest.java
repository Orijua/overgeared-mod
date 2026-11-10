package com.orijua.overgearedmod.feature.capsule;

import com.mojang.serialization.JsonOps;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import net.minecraft.network.chat.contents.TranslatableContents;
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

    @Test
    void theSerializedNamesAreTheStableIds() {
        assertEquals("basic", CapsuleTier.BASIC.getSerializedName());
        assertEquals("advanced", CapsuleTier.ADVANCED.getSerializedName());
        assertEquals("diamond", CapsuleTier.DIAMOND.getSerializedName());
    }

    @Test
    void theStreamCodecRoundTripsEveryTier() {
        for (CapsuleTier tier : CapsuleTier.values()) {
            ByteBuf buf = Unpooled.buffer();
            CapsuleTier.STREAM_CODEC.encode(buf, tier);
            assertEquals(tier, CapsuleTier.STREAM_CODEC.decode(buf), tier.toString());
        }
    }

    @Test
    void anIdOutOfRangeDecodesAsBasicInsteadOfThrowing() {
        for (int id : new int[]{CapsuleTier.values().length, 99, -1, Integer.MAX_VALUE}) {
            ByteBuf buf = Unpooled.buffer();
            net.minecraft.network.VarInt.write(buf, id);
            assertEquals(CapsuleTier.BASIC, CapsuleTier.STREAM_CODEC.decode(buf), "id " + id);
        }
    }

    @Test
    void theNameKeyFollowsTheSerializedName() {
        for (CapsuleTier tier : CapsuleTier.values()) {
            TranslatableContents contents = (TranslatableContents) tier.tierName().getContents();
            assertEquals("capsule_tier.overgearedmod." + tier.getSerializedName(), contents.getKey());
        }
    }
}
