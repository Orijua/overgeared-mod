package com.orijua.overgearedmod.feature.economy;

import com.orijua.overgearedmod.core.config.TradeBalance;
import com.orijua.overgearedmod.feature.economy.TradeRules.Refusal;
import com.orijua.overgearedmod.feature.equipment.ItemRating;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
        assertEquals(0, TradeRules.recovered(-4, 0, 0, B));
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
    }

    @Test
    void everyRefusalHasALangKey() {
        for (Refusal refusal : Refusal.values()) {
            assertTrue(refusal.langKey().startsWith("trade.overgearedmod.refusal."));
        }
    }
}
