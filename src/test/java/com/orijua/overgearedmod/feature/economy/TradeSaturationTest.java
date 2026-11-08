package com.orijua.overgearedmod.feature.economy;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.mojang.serialization.JsonOps;
import com.orijua.overgearedmod.core.config.TradeBalance;
import com.orijua.overgearedmod.feature.equipment.ItemRating;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
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

    @Test
    void recordSaleAtMaxIntDoesNotOverflow() {
        JsonArray array = new JsonArray();
        array.add(line("sword/epic", Integer.MAX_VALUE, 0));
        TradeSaturation read = TradeSaturation.CODEC.parse(JsonOps.INSTANCE, array).getOrThrow();
        assertEquals(37, read.pressure(SWORD, 0, B));
        TradeSaturation after = read.recordSale(SWORD, 0, B);
        assertEquals(37, after.pressure(SWORD, 0, B));
        assertTrue(after.entries().get(SWORD).pressure() >= 0);
    }

    @Test
    void aLoweredCapAppliesOnRead() {
        JsonArray array = new JsonArray();
        array.add(line("sword/epic", 30, 0));
        TradeSaturation read = TradeSaturation.CODEC.parse(JsonOps.INSTANCE, array).getOrThrow();
        assertEquals(10, read.pressure(SWORD, 0, TradeBalancesForTests.withPressureCap(10)));
    }

    @Test
    void recordSaleWithEarlierDay() {
        TradeSaturation saturation = TradeSaturation.EMPTY.recordSale(SWORD, 10, B).recordSale(SWORD, 5, B);
        assertEquals(new TradeSaturation.Entry(2, 10), saturation.entries().get(SWORD));
        assertEquals(0, saturation.pressure(SWORD, 11, B));
    }

    @Test
    void extremeDaysOnRead() {
        TradeSaturation saturation = TradeSaturation.EMPTY.recordSale(SWORD, 0, B);
        for (long day : new long[]{Long.MIN_VALUE, Long.MAX_VALUE}) {
            int pressure = saturation.pressure(SWORD, day, B);
            assertTrue(pressure >= 0 && pressure <= 37, "day " + day + ": " + pressure);
        }
    }

    @Test
    void codecClampsNegativePressureAndDefaultsMissingFields() {
        JsonArray array = new JsonArray();
        array.add(line("sword/epic", -3, 2));
        JsonObject bare = new JsonObject();
        bare.addProperty("key", "helmet/epic");
        array.add(bare);
        TradeSaturation read = TradeSaturation.CODEC.parse(JsonOps.INSTANCE, array).getOrThrow();
        assertEquals(new TradeSaturation.Entry(0, 2), read.entries().get(SWORD));
        assertEquals(new TradeSaturation.Entry(0, 0), read.entries().get(HELMET));
    }

    @Test
    void codecDuplicateKeysLastWins() {
        JsonArray array = new JsonArray();
        array.add(line("sword/epic", 4, 1));
        array.add(line("sword/epic", 7, 2));
        TradeSaturation read = TradeSaturation.CODEC.parse(JsonOps.INSTANCE, array).getOrThrow();
        assertEquals(new TradeSaturation.Entry(7, 2), read.entries().get(SWORD));
    }

    @Test
    void codecKeepsExtremeDays() {
        for (long day : new long[]{Long.MIN_VALUE, Long.MAX_VALUE}) {
            TradeSaturation saturation = new TradeSaturation(Map.of(SWORD, new TradeSaturation.Entry(3, day)));
            var json = TradeSaturation.CODEC.encodeStart(JsonOps.INSTANCE, saturation).getOrThrow();
            assertEquals(saturation, TradeSaturation.CODEC.parse(JsonOps.INSTANCE, json).getOrThrow());
        }
    }

    @Test
    void streamCodecRoundTrips() {
        TradeSaturation two = TradeSaturation.EMPTY.recordSale(SWORD, 3, B).recordSale(HELMET, 4, B);
        for (TradeSaturation saturation : new TradeSaturation[]{TradeSaturation.EMPTY, two}) {
            ByteBuf buffer = Unpooled.buffer();
            TradeSaturation.STREAM_CODEC.encode(buffer, saturation);
            assertEquals(saturation, TradeSaturation.STREAM_CODEC.decode(buffer));
        }
    }

    @Test
    void entriesAreImmutable() {
        TradeSaturation saturation = TradeSaturation.EMPTY.recordSale(SWORD, 0, B);
        assertThrows(UnsupportedOperationException.class, () -> saturation.entries().put(HELMET, new TradeSaturation.Entry(1, 0)));
    }
}
