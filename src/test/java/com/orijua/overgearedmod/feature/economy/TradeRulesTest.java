package com.orijua.overgearedmod.feature.economy;

import com.orijua.overgearedmod.core.config.TradeBalance;
import com.orijua.overgearedmod.feature.economy.TradeRules.Refusal;
import com.orijua.overgearedmod.feature.equipment.ItemRating;
import com.google.gson.JsonObject;
import com.orijua.overgearedmod.lang.LangFiles;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TradeRulesTest {
    private static final TradeBalance B = TradeBalance.DEFAULT;

    @Test
    void anEpicIronSwordIsWorth40() {
        long ref = TradeRules.reference(ItemRating.EPIC, 1.0, 0, true, B);
        assertEquals(40, ref);
        assertEquals(16, TradeRules.sellPrice(ref, 0, B));
        assertEquals(3800, TradeRules.exportPrice(ref, 0, B));
    }

    @Test
    void aDiamondLegendaryPlusFiveIsWorth1800() {
        long ref = TradeRules.reference(ItemRating.LEGENDARY, 2.0, 5, true, B);
        assertEquals(1800, ref);
        assertEquals(720, TradeRules.sellPrice(ref, 0, B));
        assertEquals(171000, TradeRules.exportPrice(ref, 0, B));
    }

    @Test
    void aWoodenNormalPieceIsWorthAtLeastOne() {
        long ref = TradeRules.reference(ItemRating.NORMAL, 0.5, 0, true, B);
        assertEquals(1, ref);
        assertEquals(1, TradeRules.sellPrice(ref, 0, B));
        assertEquals(95, TradeRules.exportPrice(ref, 0, B));
    }

    @Test
    void enhancementOnlyCountsForWeapons() {
        assertEquals(60, TradeRules.reference(ItemRating.EPIC, 1.0, 5, true, B));
        assertEquals(40, TradeRules.reference(ItemRating.EPIC, 1.0, 5, false, B));
    }

    @Test
    void everyBasePrice() {
        assertEquals(3, TradeRules.reference(ItemRating.NORMAL, 1.0, 0, false, B));
        assertEquals(12, TradeRules.reference(ItemRating.RARE, 1.0, 0, false, B));
        assertEquals(40, TradeRules.reference(ItemRating.EPIC, 1.0, 0, false, B));
        assertEquals(150, TradeRules.reference(ItemRating.UNIQUE, 1.0, 0, false, B));
        assertEquals(600, TradeRules.reference(ItemRating.LEGENDARY, 1.0, 0, false, B));
        assertEquals(2500, TradeRules.reference(ItemRating.MYTH, 1.0, 0, false, B));
    }

    @Test
    void theSaturationFollowsThePressure() {
        assertEquals(1.0, TradeRules.saturation(0, B));
        assertEquals(0.95, TradeRules.saturation(1, B), 1e-12);
        assertEquals(Math.pow(0.95, 36), TradeRules.saturation(36, B), 1e-12);
        assertEquals(0.15, TradeRules.saturation(37, B));
        assertEquals(0.15, TradeRules.saturation(1000, B));
        assertEquals(1.0, TradeRules.saturation(-3, B));
    }

    @Test
    void oneSaleLowersThePriceOfAnEpicIronSword() {
        assertEquals(15, TradeRules.sellPrice(40, 1, B));
        assertEquals(3610, TradeRules.exportPrice(40, 1, B));
    }

    @Test
    void thePriceDispatchesByMode() {
        assertEquals(16, TradeRules.price(TradeMode.SELL, 40, 0, B));
        assertEquals(3800, TradeRules.price(TradeMode.EXPORT, 40, 0, B));
    }

    @Test
    void thePressureRecoversTwoPerDay() {
        assertEquals(3, TradeRules.recovered(5, 0, 1, B));
        assertEquals(0, TradeRules.recovered(5, 0, 3, B));
        assertEquals(0, TradeRules.recovered(5, 0, 100, B));
        assertEquals(5, TradeRules.recovered(5, 4, 4, B));
    }

    @Test
    void daysThatGoBackCountAsNone() {
        assertEquals(5, TradeRules.recovered(5, 10, 3, B));
    }

    @Test
    void aNegativePressureIsNone() {
        assertEquals(0, TradeRules.recovered(-4, 0, 0, B));
    }

    @Test
    void recoveredWithHugeDays() {
        assertEquals(0, TradeRules.recovered(5, Long.MIN_VALUE, Long.MAX_VALUE, B));
        assertEquals(5, TradeRules.recovered(5, Long.MAX_VALUE, Long.MIN_VALUE, B));
        assertEquals(0, TradeRules.recovered(Integer.MAX_VALUE, 0, Long.MAX_VALUE, B));
    }

    @Test
    void noRecoveryWhenPerDayIsZeroOrNegative() {
        assertEquals(5, TradeRules.recovered(5, 0, 10, TradeBalancesForTests.withRecoveryPerDay(0)));
        assertEquals(5, TradeRules.recovered(5, 0, 10, TradeBalancesForTests.withRecoveryPerDay(-3)));
    }

    @Test
    void payableNeedsAtLeastOne() {
        assertFalse(TradeRules.payable(TradeMode.EXPORT, 0, B));
        assertTrue(TradeRules.payable(TradeMode.EXPORT, 1, B));
        assertTrue(TradeRules.payable(TradeMode.SELL, 1, B));
        assertFalse(TradeRules.payable(TradeMode.SELL, 0, B));
        assertFalse(TradeRules.payable(TradeMode.SELL, -5, B));
    }

    @Test
    void payableStopsAtTheSellCap() {
        long cap = B.maxSellEmeralds();
        assertTrue(TradeRules.payable(TradeMode.SELL, cap, B));
        assertFalse(TradeRules.payable(TradeMode.SELL, cap + 1, B));
        assertTrue(TradeRules.payable(TradeMode.EXPORT, cap + 1, B));
    }

    @Test
    void paymentsSplitInStacks() {
        long[] sale = TradeRules.payments(TradeMode.SELL, 720, 64);
        assertEquals(12, sale.length);
        for (int i = 0; i < 11; i++) {
            assertEquals(64, sale[i]);
        }
        assertEquals(16, sale[11]);
        assertArrayEquals(new long[]{64}, TradeRules.payments(TradeMode.SELL, 64, 64));
        assertArrayEquals(new long[]{1}, TradeRules.payments(TradeMode.SELL, 1, 64));
        assertArrayEquals(new long[]{171000}, TradeRules.payments(TradeMode.EXPORT, 171000, 64));
        long total = 0;
        for (long part : TradeRules.payments(TradeMode.SELL, B.maxSellEmeralds(), 64)) {
            total += part;
        }
        assertEquals(B.maxSellEmeralds(), total);
    }

    @Test
    void commissionPercentText() {
        assertEquals("5", TradeRules.commissionPercent(B));
        assertEquals("2.5", TradeRules.commissionPercent(TradeBalancesForTests.withExportCommission(0.025)));
        assertEquals("0", TradeRules.commissionPercent(TradeBalancesForTests.withExportCommission(0)));
        assertEquals("100", TradeRules.commissionPercent(TradeBalancesForTests.withExportCommission(1)));
        assertEquals("12.3", TradeRules.commissionPercent(TradeBalancesForTests.withExportCommission(0.123)));
    }

    @Test
    void saturationPercentRounds() {
        assertEquals(100, TradeRules.saturationPercent(0, B));
        assertEquals(95, TradeRules.saturationPercent(1, B));
        assertEquals(15, TradeRules.saturationPercent(37, B));
        assertEquals(100, TradeRules.saturationPercent(1, TradeBalancesForTests.withSaturationStep(0.999)));
    }

    @Test
    void floorBeatsTheCurveBeforeTheCap() {
        TradeBalance floor = TradeBalancesForTests.withSaturationFloor(0.9);
        assertEquals(0.95, TradeRules.saturation(1, floor), 1e-12);
        assertEquals(0.9, TradeRules.saturation(3, floor), 1e-12);
        assertEquals(0.9, TradeRules.saturation(37, floor), 1e-12);
    }

    @Test
    void priceAtTheCap() {
        assertEquals(2, TradeRules.sellPrice(40, 37, B));
        assertEquals(570, TradeRules.exportPrice(40, 37, B));
    }

    @Test
    void negativeEnhancementCountsAsZero() {
        assertEquals(TradeRules.reference(ItemRating.EPIC, 1.0, 0, true, B), TradeRules.reference(ItemRating.EPIC, 1.0, -5, true, B));
    }

    @Test
    void referenceMatchesExactArithmetic() {
        for (ItemRating rating : ItemRating.values()) {
            for (Map.Entry<String, Double> material : B.materialMultiplier().entrySet()) {
                for (int level = 0; level <= 10; level++) {
                    BigDecimal value = BigDecimal.valueOf(B.basePrice().get(rating))
                            .multiply(BigDecimal.valueOf(material.getValue()))
                            .multiply(BigDecimal.ONE.add(BigDecimal.valueOf(B.enhancementBonus()).multiply(BigDecimal.valueOf(level))));
                    long expected = Math.max(1L, value.setScale(0, RoundingMode.FLOOR).longValueExact());
                    assertEquals(expected, TradeRules.reference(rating, material.getValue(), level, true, B),
                            rating + " " + material.getKey() + " +" + level);
                }
            }
        }
    }

    @Test
    void messagesExistForEveryResult() {
        for (TradeService.Result result : TradeService.Result.values()) {
            Optional<net.minecraft.network.chat.Component> message = TradeService.message(new TradeService.Outcome(
                    result, result == TradeService.Result.REFUSED ? Optional.of(Refusal.LOANED) : Optional.empty(), 0L));
            assertEquals(result == TradeService.Result.SOLD, message.isEmpty(), result.toString());
        }
        assertEquals("message.overgearedmod.trade_stale", key(TradeService.Result.STALE));
        assertEquals("message.overgearedmod.trade_stale", key(TradeService.Result.EMPTY));
        assertEquals("message.overgearedmod.trade_stale", key(TradeService.Result.INVALID_SLOT));
        assertEquals("message.overgearedmod.trade_failed", key(TradeService.Result.FAILED));
        assertEquals("message.overgearedmod.trade_outside", key(TradeService.Result.OUTSIDE_SATISFY));
        assertEquals("message.overgearedmod.trade_refused", key(TradeService.Result.REFUSED));
    }

    private static String key(TradeService.Result result) {
        var message = TradeService.message(new TradeService.Outcome(
                result, result == TradeService.Result.REFUSED ? Optional.of(Refusal.LOANED) : Optional.empty(), 0L)).orElseThrow();
        return ((net.minecraft.network.chat.contents.TranslatableContents) message.getContents()).getKey();
    }

    @Test
    void aDayIs24000Ticks() {
        assertEquals(0, TradeRules.day(23999));
        assertEquals(1, TradeRules.day(24000));
        assertEquals(5, TradeRules.day(5 * 24000L + 7));
    }

    @Test
    void refusalsComeInOrder() {
        assertEquals(Optional.of(Refusal.LOANED), TradeRules.refusal(5, false, false, true));
        assertEquals(Optional.of(Refusal.STACKED), TradeRules.refusal(2, false, false, false));
        assertEquals(Optional.of(Refusal.NO_VALUE), TradeRules.refusal(1, false, false, false));
        assertEquals(Optional.of(Refusal.UNRATED), TradeRules.refusal(1, true, false, false));
        assertTrue(TradeRules.refusal(1, true, true, false).isEmpty());
        assertEquals(Optional.of(Refusal.LOANED), TradeRules.refusal(5, true, true, true));
        assertEquals(Optional.of(Refusal.STACKED), TradeRules.refusal(2, true, false, false));
        assertEquals(Optional.of(Refusal.NO_VALUE), TradeRules.refusal(1, false, true, false));
        // The service cuts before with EMPTY: counts of 0 or less are not a refusal here.
        assertTrue(TradeRules.refusal(0, true, true, false).isEmpty());
        assertTrue(TradeRules.refusal(-1, true, true, false).isEmpty());
    }

    @Test
    void everyTradeKeyExistsInBothLangFiles() {
        List<String> keys = new ArrayList<>();
        for (Refusal refusal : Refusal.values()) {
            assertTrue(refusal.langKey().startsWith("trade.overgearedmod.refusal."));
            keys.add(refusal.langKey());
        }
        keys.addAll(List.of("notification.overgearedmod.trade.sold", "notification.overgearedmod.trade.exported",
                "screen.overgearedmod.trade.confirm_sell", "screen.overgearedmod.trade.confirm_export",
                "message.overgearedmod.trade_refused", "message.overgearedmod.trade_outside",
                "message.overgearedmod.trade_stale", "message.overgearedmod.trade_failed"));
        for (String file : List.of("en_us.json", "es_es.json")) {
            JsonObject lang = LangFiles.load(file);
            for (String key : keys) {
                assertTrue(lang.has(key), key + " missing in " + file);
            }
            assertFalse(lang.get("screen.overgearedmod.trade.confirm_export").getAsString().contains("5 %"), file + ": the commission is an argument");
            assertEquals(2, lang.get("screen.overgearedmod.trade.confirm_export").getAsString().split("%s", -1).length - 1, file);
        }
    }
}
