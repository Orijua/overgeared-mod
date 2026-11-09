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

    // ---- Buying: writing into a saved inventory ----

    private static CompoundTag slotted(int slot, String id, int count) {
        CompoundTag tag = entry(id, count);
        tag.putByte("Slot", (byte) slot);
        return tag;
    }

    /** Stone in the slots 0 to 35 except the ones given. */
    private static ListTag fullExcept(int... free) {
        ListTag list = new ListTag();
        for (int slot = 0; slot < 36; slot++) {
            boolean skip = false;
            for (int f : free) {
                skip |= f == slot;
            }
            if (!skip) {
                list.add(slotted(slot, "minecraft:stone", 64));
            }
        }
        return list;
    }

    @Test
    void anEmptyInventoryHasRoomForEverySlot() {
        ListTag empty = new ListTag();
        assertEquals(36 * 64, TerminalRules.emeraldRoom(empty, 64));
        assertEquals(java.util.OptionalInt.of(0), TerminalRules.firstFreeSlot(empty));
        assertEquals(36, TerminalRules.freeSlots(empty));
    }

    @Test
    void roomCountsTheStackToCompleteAndThenNothing() {
        ListTag inv = fullExcept(0);
        inv.add(slotted(0, TerminalRules.EMERALD_ID, 60));
        assertEquals(4, TerminalRules.emeraldRoom(inv, 64));
        assertEquals(java.util.OptionalInt.empty(), TerminalRules.firstFreeSlot(inv));
        ListTag added = TerminalRules.addEmeralds(inv, 4, 64);
        assertEquals(64, TerminalRules.countEmeralds(added));
        assertThrows(IllegalArgumentException.class, () -> TerminalRules.addEmeralds(inv, 5, 64));
    }

    @Test
    void anEmeraldWithComponentsIsNeverCompleted() {
        ListTag inv = fullExcept(0);
        CompoundTag special = slotted(0, TerminalRules.EMERALD_ID, 10);
        special.put("components", new CompoundTag());
        inv.add(special);
        assertEquals(0, TerminalRules.emeraldRoom(inv, 64));
    }

    @Test
    void armorAndOffhandSlotsAreNotRoom() {
        ListTag inv = fullExcept();
        inv.add(slotted(100, "minecraft:iron_helmet", 1));
        inv.add(slotted(103, "minecraft:iron_boots", 1));
        inv.add(slotted(150, "minecraft:shield", 1));
        assertEquals(0, TerminalRules.emeraldRoom(inv, 64));
        assertEquals(0, TerminalRules.freeSlots(inv));
        assertEquals(java.util.OptionalInt.empty(), TerminalRules.firstFreeSlot(inv));
        assertThrows(IllegalArgumentException.class, () -> TerminalRules.withEntryAt(inv, 100, entry("minecraft:stone", 1)));
    }

    @Test
    void emeraldsCompleteStacksBySlotOrderThenFillFreeSlots() {
        ListTag inv = new ListTag();
        inv.add(slotted(5, TerminalRules.EMERALD_ID, 60));
        inv.add(slotted(1, TerminalRules.EMERALD_ID, 62));
        inv.add(slotted(0, "minecraft:stone", 3));
        ListTag before = inv.copy();
        ListTag result = TerminalRules.addEmeralds(inv, 100, 64);
        assertEquals(before, inv, "the argument does not change");
        assertEquals(122 + 100, TerminalRules.countEmeralds(result));
        // Slot 1 first (2), then slot 5 (4), then the free slots 2 and 3 (64 + 30).
        assertEquals(64, result.getCompound(0).getInt("count"));
        assertEquals(64, result.getCompound(1).getInt("count"));
        assertEquals(3, result.getCompound(2).getInt("count"), "what is not an emerald stays equal");
        assertEquals(5, result.size());
        CompoundTag a = result.getCompound(3);
        CompoundTag b = result.getCompound(4);
        assertEquals(2, a.getByte("Slot"));
        assertEquals(64, a.getInt("count"));
        assertEquals(TerminalRules.EMERALD_ID, a.getString("id"));
        assertEquals(3, b.getByte("Slot"));
        assertEquals(30, b.getInt("count"));
    }

    @Test
    void addingNothingOrTooMuchThrows() {
        assertThrows(IllegalArgumentException.class, () -> TerminalRules.addEmeralds(new ListTag(), 0, 64));
        assertThrows(IllegalArgumentException.class, () -> TerminalRules.addEmeralds(new ListTag(), 36 * 64 + 1, 64));
    }

    @Test
    void withEntryAtNeedsAFreeMainSlot() {
        ListTag inv = list(slotted(3, "minecraft:stone", 1));
        assertThrows(IllegalArgumentException.class, () -> TerminalRules.withEntryAt(inv, 3, entry("minecraft:dirt", 1)));
        assertThrows(IllegalArgumentException.class, () -> TerminalRules.withEntryAt(inv, 36, entry("minecraft:dirt", 1)));
        assertThrows(IllegalArgumentException.class, () -> TerminalRules.withEntryAt(inv, -1, entry("minecraft:dirt", 1)));
        ListTag result = TerminalRules.withEntryAt(inv, 4, entry("minecraft:dirt", 2));
        assertEquals(1, inv.size(), "the argument does not change");
        assertEquals(2, result.size());
        assertEquals(4, result.getCompound(1).getByte("Slot"));
        assertEquals("minecraft:dirt", result.getCompound(1).getString("id"));
    }

    @Test
    void whatCanBeBoughtIsTheSmallerOfRoomAndMoney() {
        assertEquals(9, TerminalRules.maxBuyable(10, 3999, 400));
        assertEquals(2, TerminalRules.maxBuyable(2, 1_000_000_000L, 400));
        assertEquals(0, TerminalRules.maxBuyable(10, 0, 400));
        assertEquals(0, TerminalRules.maxBuyable(0, 5000, 400));
        assertEquals(0, TerminalRules.maxBuyable(10, -5, 400));
    }

    @Test
    void borderCasesOfBuyingAreFixed() {
        assertEquals(0, TerminalRules.maxBuyable(10, 1000, 0));
        assertEquals(0, TerminalRules.maxBuyable(10, 1000, -5));
        assertEquals(0L, TerminalRules.buyCost(0, 400));
        assertEquals(-400L, TerminalRules.buyCost(-1, 400), "only called after n >= 1");
    }

    @Test
    void aFullStackOfEmeraldsLeavesNoRoomAndOtherSlotsDoNotCount() {
        ListTag inv = fullExcept(0);
        inv.add(slotted(0, TerminalRules.EMERALD_ID, 64));
        assertEquals(0, TerminalRules.emeraldRoom(inv, 64));
        ListTag odd = fullExcept(0);
        odd.add(slotted(40, "minecraft:stone", 1));
        odd.add(slotted(100, "minecraft:iron_helmet", 1));
        assertEquals(1, TerminalRules.freeSlots(odd));
        assertEquals(java.util.OptionalInt.of(0), TerminalRules.firstFreeSlot(odd));
    }

    @Test
    void theCostOfAFullInventoryDoesNotOverflow() {
        assertEquals(2304L * 1_000_000L, TerminalRules.buyCost(2304, 1_000_000));
        assertEquals(3_000_000_000L, TerminalRules.buyCost(3000, 1_000_000));
    }
}
