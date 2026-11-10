package com.orijua.overgearedmod.feature.economy;

import com.orijua.overgearedmod.core.config.TerminalBalance;
import com.orijua.overgearedmod.core.config.TradeBalance;
import com.orijua.overgearedmod.feature.equipment.ItemRating;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

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
        assertEquals(0.25, b.minDurabilityFactor());
    }

    private static TradeBalance withMinDurability(double value) {
        return TradeBalancesForTests.withMinDurabilityFactor(value);
    }

    private static TradeBalance withItemMultiplier(double value) {
        return TradeBalancesForTests.withItemMultipliers(Map.of(ResourceLocation.withDefaultNamespace("iron_sword"), value));
    }

    @Test
    void theMinimumDurabilityFactorMustBeInZeroToOne() {
        assertThrows(IllegalArgumentException.class, () -> withMinDurability(-0.01));
        assertThrows(IllegalArgumentException.class, () -> withMinDurability(1.01));
        assertThrows(IllegalArgumentException.class, () -> withMinDurability(Double.NaN));
        assertEquals(0.0, withMinDurability(0.0).minDurabilityFactor());
        assertEquals(1.0, withMinDurability(1.0).minDurabilityFactor());
    }

    @Test
    void theDefaultObjectMultipliersAreTheFourUniqueObjects() {
        Map<ResourceLocation, Double> m = TradeBalance.DEFAULT.itemMultiplier();
        assertEquals(4, m.size());
        assertEquals(0.25, m.get(ResourceLocation.fromNamespaceAndPath("overgearedmod", "fracaso_sword")));
        assertEquals(2.0, m.get(ResourceLocation.fromNamespaceAndPath("overgearedmod", "jaffa_sword")));
        assertEquals(2.0, m.get(ResourceLocation.fromNamespaceAndPath("overgearedmod", "jaffa_smithing_hammer")));
        assertEquals(3.0, m.get(ResourceLocation.fromNamespaceAndPath("overgearedmod", "mithril_smithing_hammer")));
    }

    @Test
    void anObjectMultiplierMustBeAFiniteNumberFromZeroToOneHundred() {
        assertThrows(IllegalArgumentException.class, () -> withItemMultiplier(-0.5));
        assertThrows(IllegalArgumentException.class, () -> withItemMultiplier(Double.NaN));
        assertThrows(IllegalArgumentException.class, () -> withItemMultiplier(Double.POSITIVE_INFINITY));
        assertThrows(IllegalArgumentException.class, () -> withItemMultiplier(100.5));
        assertEquals(0.0, withItemMultiplier(0.0).itemMultiplier().values().iterator().next());
        assertEquals(100.0, withItemMultiplier(100.0).itemMultiplier().values().iterator().next());
    }

    @Test
    void theObjectMultipliersNeverShareAnItemWithTheTerminalOffer() {
        assertFalse(TerminalBalance.DEFAULT.offerItems().isEmpty(), "the offer has items, so the check below checks something");
        for (ResourceLocation id : TradeBalance.DEFAULT.itemMultiplier().keySet()) {
            assertFalse(TerminalBalance.DEFAULT.offerItems().contains(id), id + " is in the daily offer");
        }
    }

    @Test
    void currentIsTheDefaultsBeforeTheConfigLoads() {
        assertEquals(TradeBalance.DEFAULT, TradeBalance.current());
    }

    @Test
    void anOverrideAppliesOnlyWhileTheActionRuns() {
        TradeBalance changed = TradeBalancesForTests.withSellShare(0.5);
        TradeBalance[] seen = new TradeBalance[1];
        TradeBalance.withOverride(changed, () -> seen[0] = TradeBalance.current());
        assertEquals(changed, seen[0]);
        assertEquals(TradeBalance.DEFAULT, TradeBalance.current());
    }

    @Test
    void anOverrideIsRestoredEvenWhenTheActionThrows() {
        TradeBalance changed = TradeBalancesForTests.withSellShare(0.5);
        assertThrows(IllegalStateException.class, () -> TradeBalance.withOverride(changed, () -> {
            assertEquals(changed, TradeBalance.current());
            throw new IllegalStateException("boom");
        }));
        assertEquals(TradeBalance.DEFAULT, TradeBalance.current(), "the override did not leak");
    }

    @Test
    void nestedOverridesRestoreTheOuterOne() {
        TradeBalance outer = TradeBalancesForTests.withSellShare(0.5);
        TradeBalance inner = TradeBalancesForTests.withSellShare(0.6);
        TradeBalance.withOverride(outer, () -> {
            TradeBalance.withOverride(inner, () -> assertEquals(inner, TradeBalance.current()));
            assertEquals(outer, TradeBalance.current());
        });
        assertEquals(TradeBalance.DEFAULT, TradeBalance.current());
    }

    @Test
    void theMapsAreImmutableAndCopiedFromTheOnesGiven() {
        TradeBalance defaults = TradeBalance.DEFAULT;
        ResourceLocation sword = ResourceLocation.withDefaultNamespace("iron_sword");
        assertThrows(UnsupportedOperationException.class, () -> defaults.itemMultiplier().put(sword, 2.0));
        assertThrows(UnsupportedOperationException.class, () -> defaults.itemMultiplier().clear());
        assertThrows(UnsupportedOperationException.class, () -> defaults.materialMultiplier().put("wood", 9.0));
        assertThrows(UnsupportedOperationException.class, () -> defaults.basePrice().put(ItemRating.NORMAL, 9));

        Map<ResourceLocation, Double> source = new java.util.HashMap<>();
        source.put(sword, 2.0);
        TradeBalance balance = TradeBalancesForTests.withItemMultipliers(source);
        source.put(ResourceLocation.withDefaultNamespace("bow"), 3.0);
        source.put(sword, 50.0);
        assertEquals(Map.of(sword, 2.0), balance.itemMultiplier(), "changing the source map afterwards does not change the balance");
    }
}
