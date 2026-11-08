package com.orijua.overgearedmod.feature.economy;

import com.google.gson.JsonPrimitive;
import com.mojang.serialization.JsonOps;
import com.orijua.overgearedmod.feature.equipment.ItemRating;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TradeKeyTest {
    @Test
    void everyKeyRoundTripsAsText() {
        for (TradeCategory category : TradeCategory.values()) {
            for (ItemRating rating : ItemRating.values()) {
                TradeKey key = new TradeKey(category, rating);
                var json = TradeKey.CODEC.encodeStart(JsonOps.INSTANCE, key).getOrThrow();
                assertEquals(key, TradeKey.CODEC.parse(JsonOps.INSTANCE, json).getOrThrow());
            }
        }
        assertEquals("sword/epic", new TradeKey(TradeCategory.SWORD, ItemRating.EPIC).asString());
    }

    @Test
    void anUnknownKeyFails() {
        assertTrue(TradeKey.CODEC.parse(JsonOps.INSTANCE, new JsonPrimitive("wand/epic")).result().isEmpty());
        assertTrue(TradeKey.CODEC.parse(JsonOps.INSTANCE, new JsonPrimitive("sword/godlike")).result().isEmpty());
        assertTrue(TradeKey.CODEC.parse(JsonOps.INSTANCE, new JsonPrimitive("sword")).result().isEmpty());
    }
}
