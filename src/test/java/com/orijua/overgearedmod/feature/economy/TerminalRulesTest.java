package com.orijua.overgearedmod.feature.economy;

import com.orijua.overgearedmod.core.config.TerminalBalance;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TerminalRulesTest {
    private static CompoundTag entry(String id, int count) {
        CompoundTag tag = new CompoundTag();
        tag.putString("id", id);
        tag.putInt("count", count);
        return tag;
    }

    private static ListTag list(CompoundTag... entries) {
        ListTag list = new ListTag();
        for (CompoundTag entry : entries) {
            list.add(entry);
        }
        return list;
    }

    // ---- Counting ----

    @Test
    void countsOnlyTheSatisfyEmeraldById() {
        ListTag inventory = list(entry(TerminalRules.EMERALD_ID, 10), entry("minecraft:emerald", 7), entry(TerminalRules.EMERALD_ID, 5));
        assertEquals(15, TerminalRules.countEmeralds(inventory));
    }

    @Test
    void anEntryWithComponentsStillCounts() {
        CompoundTag withComponents = entry(TerminalRules.EMERALD_ID, 3);
        CompoundTag components = new CompoundTag();
        components.putString("minecraft:custom_name", "x");
        withComponents.put("components", components);
        assertEquals(3, TerminalRules.countEmeralds(list(withComponents)));
    }

    @Test
    void emeraldsInsideAContainerDoNotCount() {
        CompoundTag shulker = entry("minecraft:shulker_box", 1);
        CompoundTag components = new CompoundTag();
        components.put("minecraft:container", list(entry(TerminalRules.EMERALD_ID, 9)));
        shulker.put("components", components);
        assertEquals(0, TerminalRules.countEmeralds(list(shulker)));
        assertEquals(2, TerminalRules.countEmeralds(list(shulker, entry(TerminalRules.EMERALD_ID, 2))));
    }

    @Test
    void anEmptyListHasNoEmeraldsAndAnEntryWithoutCountCountsOne() {
        assertEquals(0, TerminalRules.countEmeralds(new ListTag()));
        CompoundTag noCount = new CompoundTag();
        noCount.putString("id", TerminalRules.EMERALD_ID);
        assertEquals(1, TerminalRules.countEmeralds(list(noCount)));
    }

    // ---- Removing ----

    @Test
    void takesAcrossEntriesInOrderAndDropsTheOnesThatReachZero() {
        ListTag inventory = list(entry(TerminalRules.EMERALD_ID, 10), entry("minecraft:iron_sword", 1), entry(TerminalRules.EMERALD_ID, 5));
        ListTag result = TerminalRules.removeEmeralds(inventory, 12);
        assertEquals(2, result.size());
        assertEquals(entry("minecraft:iron_sword", 1), result.getCompound(0));
        assertEquals(entry(TerminalRules.EMERALD_ID, 3), result.getCompound(1));
        assertEquals(3, TerminalRules.countEmeralds(result));
    }

    @Test
    void takingAnEntryExactlyRemovesIt() {
        ListTag result = TerminalRules.removeEmeralds(list(entry(TerminalRules.EMERALD_ID, 4), entry("minecraft:stick", 2)), 4);
        assertEquals(1, result.size());
        assertEquals(entry("minecraft:stick", 2), result.getCompound(0));
    }

    @Test
    void theRestStaysEqualAndTheArgumentIsNotMutated() {
        CompoundTag sword = entry("minecraft:iron_sword", 1);
        CompoundTag components = new CompoundTag();
        components.putInt("minecraft:damage", 5);
        sword.put("components", components);
        ListTag inventory = list(entry(TerminalRules.EMERALD_ID, 10), sword);
        ListTag before = inventory.copy();
        ListTag result = TerminalRules.removeEmeralds(inventory, 4);
        assertEquals(before, inventory);
        assertEquals(sword, result.getCompound(1));
        assertEquals(6, result.getCompound(0).getInt("count"));
    }

    @Test
    void anInvalidAmountIsRejected() {
        ListTag inventory = list(entry(TerminalRules.EMERALD_ID, 10));
        assertThrows(IllegalArgumentException.class, () -> TerminalRules.removeEmeralds(inventory, 0));
        assertThrows(IllegalArgumentException.class, () -> TerminalRules.removeEmeralds(inventory, 11));
        assertThrows(IllegalArgumentException.class, () -> TerminalRules.removeEmeralds(inventory, -1));
    }

    // ---- Conversion ----

    @Test
    void theNetIsRoundedDownAfterTheCommission() {
        assertEquals(95L, TerminalRules.net(1, 100, 0.05));
        assertEquals(285L, TerminalRules.net(3, 100, 0.05));
        assertEquals(1140L, TerminalRules.net(12, 100, 0.05));
        assertEquals(85L, TerminalRules.net(1, 90, 0.05));
        assertEquals(0L, TerminalRules.net(1, 1, 0.05));
        assertEquals(94L, TerminalRules.net(3, 33, 0.05), "94.05 rounds down");
    }

    @Test
    void commissionOfZeroAndOne() {
        assertEquals(1200L, TerminalRules.net(12, 100, 0.0));
        assertEquals(0L, TerminalRules.net(12, 100, 1.0));
    }

    @Test
    void aHugeAmountDoesNotOverflow() {
        long gross = TerminalRules.gross(Integer.MAX_VALUE, 1_000_000);
        assertEquals(2_147_483_647_000_000L, gross);
        long net = TerminalRules.net(Integer.MAX_VALUE, 1_000_000, 0.05);
        assertEquals(2_040_109_464_650_000L, net);
        long other = TerminalRules.net(Integer.MAX_VALUE, 999_999, 0.07);
        long expected = new java.math.BigDecimal(Integer.MAX_VALUE).multiply(new java.math.BigDecimal(999_999))
                .multiply(new java.math.BigDecimal("0.93")).setScale(0, java.math.RoundingMode.FLOOR).longValueExact();
        assertEquals(expected, other);
    }

    @Test
    void grossMinusNetIsTheCommission() {
        assertEquals(60L, TerminalRules.gross(12, 100) - TerminalRules.net(12, 100, 0.05));
        assertEquals(5L, TerminalRules.gross(1, 100) - TerminalRules.net(1, 100, 0.05));
        assertEquals(15L, TerminalRules.gross(3, 100) - TerminalRules.net(3, 100, 0.05));
    }

    @Test
    void theDebtSplitTakesAtMostHalf() {
        assertEquals(new TerminalRules.Split(0L, 1000L), TerminalRules.split(1000L, 0L));
        assertEquals(new TerminalRules.Split(500L, 500L), TerminalRules.split(1000L, 1_000_000L));
        assertEquals(new TerminalRules.Split(120L, 880L), TerminalRules.split(1000L, 120L));
        assertEquals(new TerminalRules.Split(500L, 501L), TerminalRules.split(1001L, 1_000_000L));
        assertEquals(new TerminalRules.Split(0L, 1000L), TerminalRules.split(1000L, -5L));
        assertEquals(new TerminalRules.Split(0L, 0L), TerminalRules.split(0L, 100L));
    }

    // ---- Work ----

    @Test
    void theOrderOfTheDayIsTheDayModuloTheList() {
        assertEquals(0, TerminalRules.orderIndex(0, 5));
        assertEquals(4, TerminalRules.orderIndex(4, 5));
        assertEquals(0, TerminalRules.orderIndex(5, 5));
        assertEquals(4, TerminalRules.orderIndex(-1, 5));
        List<WorkOrder> orders = TerminalBalance.DEFAULT.workOrders();
        assertEquals(orders.get(2), TerminalRules.orderFor(7, orders));
    }

    @Test
    void workedOnNeedsTheSameDayAndNeverIsNotADay() {
        assertFalse(TerminalRules.workedOn(Long.MIN_VALUE, 0));
        assertFalse(TerminalRules.workedOn(Long.MIN_VALUE, Long.MIN_VALUE));
        assertTrue(TerminalRules.workedOn(3, 3));
        assertFalse(TerminalRules.workedOn(3, 4));
    }

    @Test
    void minutesToTheNextDayRoundUp() {
        assertEquals(20, TerminalRules.minutesToNextDay(0));
        assertEquals(10, TerminalRules.minutesToNextDay(12000));
        assertEquals(1, TerminalRules.minutesToNextDay(23999));
        assertEquals(20, TerminalRules.minutesToNextDay(24000));
    }

    @Test
    void parsesOrders() {
        assertEquals(Optional.of(new WorkOrder(ResourceLocation.parse("c:ingots/iron"), true, 16)), TerminalRules.parseOrder("#c:ingots/iron=16"));
        assertEquals(Optional.of(new WorkOrder(ResourceLocation.parse("minecraft:wheat"), false, 64)), TerminalRules.parseOrder("minecraft:wheat=64"));
        assertTrue(TerminalRules.parseOrder("minecraft:wheat").isEmpty());
        assertTrue(TerminalRules.parseOrder("minecraft:wheat=0").isEmpty());
        assertTrue(TerminalRules.parseOrder("minecraft:wheat=many").isEmpty());
        assertTrue(TerminalRules.parseOrder("Not An Id=3").isEmpty());
        assertTrue(TerminalRules.parseOrder("=3").isEmpty());
        assertTrue(TerminalRules.parseOrder("#=5").isEmpty());
        assertTrue(TerminalRules.parseOrder("minecraft:=5").isEmpty());
        assertTrue(TerminalRules.parseOrder(null).isEmpty());
        assertTrue(TerminalRules.parseOrder("minecraft:wheat=2368").isPresent());
        assertTrue(TerminalRules.parseOrder("minecraft:wheat=2369").isEmpty());
        assertEquals(Optional.of(new WorkOrder(ResourceLocation.parse("minecraft:coals"), true, 32)), TerminalRules.parseOrder(" #minecraft:coals = 32 "));
    }

    @Test
    void anOrderRoundTripsThroughItsTextForm() {
        for (WorkOrder order : TerminalBalance.DEFAULT.workOrders()) {
            assertEquals(Optional.of(order), TerminalRules.parseOrder(order.asString()));
        }
    }
}
