package com.orijua.overgearedmod.feature.economy;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.mojang.serialization.JsonOps;
import com.orijua.overgearedmod.core.config.TradeBalance;
import com.orijua.overgearedmod.feature.equipment.ItemRating;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TradeSaturationTest {
    private static final TradeBalance B = TradeBalance.DEFAULT;
    private static final TradeKey SWORD = new TradeKey(TradeCategory.SWORD, ItemRating.EPIC);
    private static final TradeKey HELMET = new TradeKey(TradeCategory.HELMET, ItemRating.EPIC);

    @Test
    void anUnknownKeyHasNoPressure() {
        assertEquals(0, TradeSaturation.EMPTY.pressure(SWORD, 100, B));
    }

    @Test
    void aSaleAddsOneAndOnlyToItsKey() {
        TradeSaturation saturation = TradeSaturation.EMPTY.recordSale(SWORD, 0, B);
        assertEquals(1, saturation.pressure(SWORD, 0, B));
        assertEquals(0, saturation.pressure(HELMET, 0, B));
    }

    @Test
    void readingRecoversWithTheDays() {
        TradeSaturation saturation = TradeSaturation.EMPTY;
        for (int i = 0; i < 5; i++) {
            saturation = saturation.recordSale(SWORD, 0, B);
        }
        assertEquals(5, saturation.pressure(SWORD, 0, B));
        assertEquals(3, saturation.pressure(SWORD, 1, B));
        assertEquals(0, saturation.pressure(SWORD, 3, B));
        assertEquals(5, saturation.pressure(SWORD, -2, B));
    }

    @Test
    void aSaleRecoversBeforeAddingAndStampsTheDay() {
        TradeSaturation saturation = TradeSaturation.EMPTY;
        for (int i = 0; i < 5; i++) {
            saturation = saturation.recordSale(SWORD, 0, B);
        }
        saturation = saturation.recordSale(SWORD, 1, B);
        assertEquals(new TradeSaturation.Entry(4, 1), saturation.entries().get(SWORD));
    }

    @Test
    void thePressureStopsAtTheCap() {
        TradeSaturation saturation = TradeSaturation.EMPTY;
        for (int i = 0; i < 60; i++) {
            saturation = saturation.recordSale(SWORD, 0, B);
        }
        assertEquals(37, saturation.pressure(SWORD, 0, B));
    }

    @Test
    void theCodecRoundTrips() {
        TradeSaturation saturation = TradeSaturation.EMPTY.recordSale(SWORD, 3, B).recordSale(SWORD, 3, B).recordSale(HELMET, 4, B);
        var json = TradeSaturation.CODEC.encodeStart(JsonOps.INSTANCE, saturation).getOrThrow();
        assertEquals(saturation, TradeSaturation.CODEC.parse(JsonOps.INSTANCE, json).getOrThrow());
    }

    @Test
    void readingSkipsUnknownKeys() {
        JsonArray array = new JsonArray();
        array.add(line("sword/epic", 4, 2));
        array.add(line("wand/epic", 9, 2));
        array.add(line("sword/godlike", 9, 2));
        array.add(line("nonsense", 9, 2));
        TradeSaturation read = TradeSaturation.CODEC.parse(JsonOps.INSTANCE, array).getOrThrow();
        assertEquals(1, read.entries().size());
        assertEquals(new TradeSaturation.Entry(4, 2), read.entries().get(SWORD));
        assertTrue(TradeSaturation.CODEC.parse(JsonOps.INSTANCE, new JsonArray()).getOrThrow().entries().isEmpty());
    }

    private static JsonObject line(String key, int pressure, long day) {
        JsonObject object = new JsonObject();
        object.addProperty("key", key);
        object.addProperty("pressure", pressure);
        object.addProperty("day", day);
        return object;
    }
}
