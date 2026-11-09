package com.orijua.overgearedmod.feature.economy;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;

import java.util.List;
import java.util.Optional;

/**
 * The numbers of the account terminal, pure: counting Satisfy emeralds in a saved inventory (a list of item tags, as
 * {@code Inventory.save} writes it), adding to it when buying, and the daily work. Nothing here takes emeralds out: they
 * are only for spending in Satisfy.
 */
public final class TerminalRules {
    /** The id of the Satisfy emerald as it appears in a saved inventory. */
    public static final String EMERALD_ID = "overgearedmod:satisfy_emerald";
    private static final long MINUTE_TICKS = 1200L;
    /** The most items an order can ask for: 36 inventory slots plus the offhand, 37 stacks of 64. */
    public static final int MAX_ORDER_COUNT = 37 * 64;

    private TerminalRules() {
    }

    private static int countOf(CompoundTag entry) {
        return entry.contains("count", Tag.TAG_ANY_NUMERIC) ? entry.getInt("count") : 1;
    }

    /** How many Satisfy emeralds the saved inventory holds (an entry without count counts 1). */
    public static int countEmeralds(ListTag inventory) {
        long total = 0L;
        for (int i = 0; i < inventory.size(); i++) {
            CompoundTag entry = inventory.getCompound(i);
            if (EMERALD_ID.equals(entry.getString("id"))) {
                total += countOf(entry);
            }
        }
        return (int) Math.min(total, Integer.MAX_VALUE);
    }

    // ---- Buying: writing into a saved inventory ----

    /** The main inventory slots (0 to 35); armor (100-103) and offhand (150) are never written by a purchase. */
    public static final int MAIN_SLOTS = 36;

    private static int slotOf(CompoundTag entry) {
        return entry.getByte("Slot") & 255;
    }

    private static boolean[] occupied(ListTag inventory) {
        boolean[] used = new boolean[MAIN_SLOTS];
        for (int i = 0; i < inventory.size(); i++) {
            int slot = slotOf(inventory.getCompound(i));
            if (slot < MAIN_SLOTS) {
                used[slot] = true;
            }
        }
        return used;
    }

    /** The first free slot among 0 to 35, if any. */
    public static java.util.OptionalInt firstFreeSlot(ListTag inventory) {
        boolean[] used = occupied(inventory);
        for (int slot = 0; slot < MAIN_SLOTS; slot++) {
            if (!used[slot]) {
                return java.util.OptionalInt.of(slot);
            }
        }
        return java.util.OptionalInt.empty();
    }

    /** How many of the slots 0 to 35 are free. */
    public static int freeSlots(ListTag inventory) {
        boolean[] used = occupied(inventory);
        int free = 0;
        for (boolean slot : used) {
            if (!slot) {
                free++;
            }
        }
        return free;
    }

    /** A clean emerald stack is one with no data components: only those can be completed. */
    private static boolean cleanEmerald(CompoundTag entry) {
        return EMERALD_ID.equals(entry.getString("id")) && !entry.contains("components") && slotOf(entry) < MAIN_SLOTS;
    }

    /** How many emeralds fit: the room left in clean emerald stacks of slots 0 to 35, plus a full stack for every free slot. */
    public static int emeraldRoom(ListTag inventory, int maxStack) {
        long room = 0L;
        for (int i = 0; i < inventory.size(); i++) {
            CompoundTag entry = inventory.getCompound(i);
            if (cleanEmerald(entry)) {
                room += Math.max(0, maxStack - countOf(entry));
            }
        }
        room += (long) freeSlots(inventory) * maxStack;
        return (int) Math.min(room, Integer.MAX_VALUE);
    }

    /**
     * A copy of the inventory with {@code n} more emeralds: first the clean emerald stacks of slots 0 to 35 are completed in
     * slot order, then the free slots (in order) get new entries. The argument is not touched.
     *
     * @throws IllegalArgumentException if {@code n} is below 1 or above {@link #emeraldRoom}
     */
    public static ListTag addEmeralds(ListTag inventory, int n, int maxStack) {
        int room = emeraldRoom(inventory, maxStack);
        if (n < 1 || n > room) {
            throw new IllegalArgumentException("cannot add " + n + " emeralds, room for " + room);
        }
        int left = n;
        // Completing the stacks goes by slot order, whatever the order of the list.
        java.util.List<Integer> stacks = new java.util.ArrayList<>();
        for (int i = 0; i < inventory.size(); i++) {
            if (cleanEmerald(inventory.getCompound(i)) && countOf(inventory.getCompound(i)) < maxStack) {
                stacks.add(i);
            }
        }
        stacks.sort(java.util.Comparator.comparingInt(i -> slotOf(inventory.getCompound(i))));
        ListTag result = new ListTag();
        for (int i = 0; i < inventory.size(); i++) {
            result.add(inventory.getCompound(i).copy());
        }
        for (int i : stacks) {
            if (left == 0) {
                break;
            }
            CompoundTag entry = result.getCompound(i);
            int add = Math.min(left, maxStack - countOf(entry));
            entry.putInt("count", countOf(entry) + add);
            left -= add;
        }
        boolean[] used = occupied(inventory);
        for (int slot = 0; slot < MAIN_SLOTS && left > 0; slot++) {
            if (used[slot]) {
                continue;
            }
            int put = Math.min(left, maxStack);
            CompoundTag entry = new CompoundTag();
            entry.putByte("Slot", (byte) slot);
            entry.putString("id", EMERALD_ID);
            entry.putInt("count", put);
            result.add(entry);
            left -= put;
        }
        return result;
    }

    /**
     * A copy of the inventory with {@code item} (an item tag as {@code ItemStack.save} writes it) put at {@code slot}.
     *
     * @throws IllegalArgumentException if the slot is outside 0 to 35 or already taken
     */
    public static ListTag withEntryAt(ListTag inventory, int slot, CompoundTag item) {
        if (slot < 0 || slot >= MAIN_SLOTS || occupied(inventory)[slot]) {
            throw new IllegalArgumentException("slot " + slot + " is not a free main slot");
        }
        ListTag result = new ListTag();
        for (int i = 0; i < inventory.size(); i++) {
            result.add(inventory.getCompound(i).copy());
        }
        CompoundTag entry = item.copy();
        entry.putByte("Slot", (byte) slot);
        result.add(entry);
        return result;
    }

    /** Won for buying {@code n} units at {@code unit} each; a long, so a full inventory of emeralds never overflows. */
    public static long buyCost(int n, int unit) {
        return (long) n * unit;
    }

    /** The most units that fit ({@code room}) and can be paid ({@code won} at {@code unit} each); never below 0. */
    public static int maxBuyable(int room, long won, int unit) {
        if (unit < 1) {
            return 0;
        }
        return (int) Math.max(0L, Math.min(room, Math.max(0L, won) / unit));
    }

    /** Which order of a list of {@code size} belongs to a day. */
    public static int orderIndex(long day, int size) {
        return (int) Math.floorMod(day, (long) size);
    }

    public static WorkOrder orderFor(long day, List<WorkOrder> orders) {
        return orders.get(orderIndex(day, orders.size()));
    }

    /** Whether the work of {@code today} is done; {@link Long#MIN_VALUE} is "never". */
    public static boolean workedOn(long workDay, long today) {
        return workDay != Long.MIN_VALUE && workDay == today;
    }

    /** Whole minutes (rounded up) until the next game day starts, counting 1 minute = 1200 ticks. */
    public static int minutesToNextDay(long gameTime) {
        long left = TradeRules.DAY_TICKS - Math.floorMod(gameTime, TradeRules.DAY_TICKS);
        return (int) Math.floorDiv(left + MINUTE_TICKS - 1, MINUTE_TICKS);
    }

    /** Reads {@code #ns:path=N} (tag) or {@code ns:path=N} (item); empty if the id or the count (at least 1) is invalid. */
    public static Optional<WorkOrder> parseOrder(String text) {
        if (text == null) {
            return Optional.empty();
        }
        int equals = text.lastIndexOf('=');
        if (equals < 1) {
            return Optional.empty();
        }
        String idText = text.substring(0, equals).trim();
        boolean tag = idText.startsWith("#");
        if (tag) {
            idText = idText.substring(1);
        }
        ResourceLocation id = ResourceLocation.tryParse(idText);
        if (id == null || id.getPath().isEmpty()) {
            return Optional.empty();
        }
        int count;
        try {
            count = Integer.parseInt(text.substring(equals + 1).trim());
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
        return count < 1 || count > MAX_ORDER_COUNT ? Optional.empty() : Optional.of(new WorkOrder(id, tag, count));
    }
}
