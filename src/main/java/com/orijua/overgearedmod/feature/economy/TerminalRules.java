package com.orijua.overgearedmod.feature.economy;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;

import java.util.List;
import java.util.Optional;

/**
 * The numbers of the account terminal, pure: counting and taking Satisfy emeralds out of a saved inventory (a list of
 * item tags, as {@code Inventory.save} writes it), the conversion to Won, the daily work. The commission rounds down
 * (see {@link Money}, which also rounds the Trade tab).
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

    /**
     * A copy of the inventory without {@code n} emeralds, taken in order; an entry that reaches 0 disappears and everything
     * else stays equal. The entries of the argument are not touched.
     *
     * @throws IllegalArgumentException if {@code n} is below 1 or above the emeralds held
     */
    public static ListTag removeEmeralds(ListTag inventory, int n) {
        int held = countEmeralds(inventory);
        if (n < 1 || n > held) {
            throw new IllegalArgumentException("cannot take " + n + " emeralds of " + held);
        }
        int left = n;
        ListTag result = new ListTag();
        for (int i = 0; i < inventory.size(); i++) {
            CompoundTag entry = inventory.getCompound(i);
            if (left > 0 && EMERALD_ID.equals(entry.getString("id"))) {
                int count = countOf(entry);
                int take = Math.min(left, count);
                left -= take;
                if (take == count) {
                    continue;
                }
                CompoundTag copy = entry.copy();
                copy.putInt("count", count - take);
                result.add(copy);
            } else {
                result.add(entry.copy());
            }
        }
        return result;
    }

    /** Won for {@code n} emeralds before the commission. */
    public static long gross(int n, int wonPerEmerald) {
        return (long) n * wonPerEmerald;
    }

    /** Won for {@code n} emeralds after the commission, rounded down. */
    public static long net(int n, int wonPerEmerald, double commission) {
        return Money.afterCommission(gross(n, wonPerEmerald), commission);
    }

    /** How a conversion is shared between the debt and the account. */
    public record Split(long toDebt, long toAccount) {
    }

    /** Provisional until the debt step confirms it: up to half of the net pays the debt, the rest goes to the account. */
    public static Split split(long net, long debt) {
        long toDebt = Math.min(Math.max(0L, debt), net / 2);
        return new Split(toDebt, net - toDebt);
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
