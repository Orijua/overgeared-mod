package com.orijua.overgearedmod.feature.economy;

import com.orijua.overgearedmod.core.config.TerminalBalance;
import com.orijua.overgearedmod.feature.equipment.ItemRating;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TerminalBalanceTest {
    @Test
    void defaultsAreTheApprovedNumbers() {
        TerminalBalance b = TerminalBalance.DEFAULT;
        assertEquals(100, b.wonPerEmerald());
        assertEquals(1000L, b.workPay());
        assertEquals(List.of(
                new WorkOrder(ResourceLocation.parse("minecraft:coals"), true, 32),
                new WorkOrder(ResourceLocation.parse("c:ingots/iron"), true, 16),
                new WorkOrder(ResourceLocation.parse("minecraft:wheat"), false, 64),
                new WorkOrder(ResourceLocation.parse("minecraft:logs"), true, 32),
                new WorkOrder(ResourceLocation.parse("c:cobblestones"), true, 64)), b.workOrders());
    }

    @Test
    void anEmptyListOfOrdersIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> new TerminalBalance(100, 1000L, List.of()));
    }

    @Test
    void anOrderAboveWhatFitsIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> new TerminalBalance(100, 1000L,
                List.of(new WorkOrder(ResourceLocation.parse("minecraft:wheat"), false, 2369))));
        new TerminalBalance(100, 1000L, List.of(new WorkOrder(ResourceLocation.parse("minecraft:wheat"), false, 2368)));
    }

    @Test
    void anOrderWithoutItemsIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> new TerminalBalance(100, 1000L,
                List.of(new WorkOrder(ResourceLocation.parse("minecraft:wheat"), false, 0))));
    }

    @Test
    void buyingDefaultsAreTheApprovedNumbers() {
        TerminalBalance b = TerminalBalance.DEFAULT;
        assertEquals(400, b.buyWonPerEmerald());
        assertEquals(4.0, b.buyMultiplier());
        assertEquals(6, b.offerPerRank());
        assertEquals(ItemRating.RARE, b.offerMaxRating());
        assertEquals(32, b.offerItems().size());
        assertEquals(32, new java.util.HashSet<>(b.offerItems()).size(), "no repeats");
        for (ResourceLocation id : b.offerItems()) {
            assertFalse(id.getPath().contains("diamond") || id.getPath().contains("netherite"), id.toString());
        }
    }

    @Test
    void theThreeArgumentConstructorKeepsTheBuyingDefaults() {
        TerminalBalance b = new TerminalBalance(50, 10L, TerminalBalance.DEFAULT.workOrders());
        assertEquals(400, b.buyWonPerEmerald());
        assertEquals(4.0, b.buyMultiplier());
        assertEquals(6, b.offerPerRank());
        assertEquals(ItemRating.RARE, b.offerMaxRating());
        assertEquals(TerminalBalance.DEFAULT.offerItems(), b.offerItems());
    }

    @Test
    void badBuyingNumbersAreRejected() {
        TerminalBalance d = TerminalBalance.DEFAULT;
        assertThrows(IllegalArgumentException.class, () -> d.withBuy(0));
        assertThrows(IllegalArgumentException.class, () -> d.withBuy(-1));
        assertEquals(1.0, new TerminalBalance(100, 1000L, d.workOrders(), 400, 1.0, 6, ItemRating.RARE, d.offerItems()).buyMultiplier());
        assertEquals(OfferBought.MAX_POSITIONS, TerminalBalance.MAX_OFFER_PER_RANK * ItemRating.values().length);
        for (double bad : new double[]{0.5, Double.NaN, Double.POSITIVE_INFINITY}) {
            assertThrows(IllegalArgumentException.class,
                    () -> new TerminalBalance(100, 1000L, d.workOrders(), 400, bad, 6, ItemRating.RARE, d.offerItems()));
        }
        assertThrows(IllegalArgumentException.class, () -> d.withOffer(-1, ItemRating.RARE, d.offerItems()));
        assertThrows(IllegalArgumentException.class, () -> d.withOffer(13, ItemRating.RARE, d.offerItems()));
        assertThrows(NullPointerException.class, () -> d.withOffer(6, null, d.offerItems()));
        assertEquals(0, d.withOffer(0, ItemRating.RARE, List.of()).offerItems().size());
        assertEquals(12, d.withOffer(12, ItemRating.RARE, d.offerItems()).offerPerRank());
    }
}
