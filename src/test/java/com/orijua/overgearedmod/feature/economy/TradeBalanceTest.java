package com.orijua.overgearedmod.feature.economy;

import com.orijua.overgearedmod.core.config.TradeBalance;
import com.orijua.overgearedmod.feature.equipment.ItemRating;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TradeBalanceTest {
    @Test
    void defaultsAreTheApprovedNumbers() {
        TradeBalance b = TradeBalance.DEFAULT;
        assertEquals(3, b.basePrice().get(ItemRating.NORMAL));
        assertEquals(12, b.basePrice().get(ItemRating.RARE));
        assertEquals(40, b.basePrice().get(ItemRating.EPIC));
        assertEquals(150, b.basePrice().get(ItemRating.UNIQUE));
        assertEquals(600, b.basePrice().get(ItemRating.LEGENDARY));
        assertEquals(2500, b.basePrice().get(ItemRating.MYTH));
        assertEquals(Map.of("wood", 0.5, "gold", 0.5, "leather", 0.5, "stone", 1.0, "iron", 1.0, "chainmail", 1.0,
                "diamond", 2.0, "netherite", 3.0), b.materialMultiplier());
        assertEquals(1.0, b.otherMaterial());
        assertEquals(0.10, b.enhancementBonus());
        assertEquals(0.40, b.sellShare());
        assertEquals(0.95, b.saturationStep());
        assertEquals(0.15, b.saturationFloor());
        assertEquals(2, b.recoveryPerDay());
        assertEquals(37, b.pressureCap());
        assertEquals(10_000L, b.maxSellEmeralds());
    }

    @Test
    void currentIsTheDefaultsBeforeTheConfigLoads() {
        assertEquals(TradeBalance.DEFAULT, TradeBalance.current());
    }

    @Test
    void anOverrideAppliesOnlyWhileTheActionRuns() {
        TradeBalance changed = new TradeBalance(TradeBalance.DEFAULT.basePrice(), TradeBalance.DEFAULT.materialMultiplier(),
                1.0, 0.10, 0.5, 0.95, 0.15, 2, 37, 10_000L);
        TradeBalance[] seen = new TradeBalance[1];
        TradeBalance.withOverride(changed, () -> seen[0] = TradeBalance.current());
        assertEquals(changed, seen[0]);
        assertEquals(TradeBalance.DEFAULT, TradeBalance.current());
    }
}
