package com.orijua.overgearedmod.feature.economy;

import com.orijua.overgearedmod.core.config.TradeBalance;
import com.orijua.overgearedmod.feature.equipment.ItemRating;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.IntFunction;
import java.util.function.IntPredicate;

/**
 * The numbers of the trade, pure. Prices round down (the 1e-9 only absorbs floating point noise, so 3800.0000000001 or
 * 3799.9999999999 both mean 3800).
 */
public final class TradeRules {
    /** Ticks in a game day. */
    public static final long DAY_TICKS = 24000L;
    private TradeRules() {
    }

    /** Why a piece cannot be traded; the order of the constants is the order they are checked in. */
    public enum Refusal {
        LOANED("loaned"),
        STACKED("stacked"),
        NO_VALUE("no_value"),
        UNRATED("unrated");

        private final String key;

        Refusal(String key) {
            this.key = key;
        }

        public String langKey() {
            return "trade.overgearedmod.refusal." + key;
        }

        public Component message() {
            return Component.translatable(langKey());
        }
    }

    /** A price multiplier of a single object, as the config writes it: {@code ns:path=N}. */
    public record ItemMultiplier(ResourceLocation id, double multiplier) {
        public String asString() {
            return id + "=" + multiplier;
        }
    }

    /** Reads {@code ns:path=N}; empty if the id is not a valid item id (a tag is not) or N is not a finite number from 0 to 100. */
    public static Optional<ItemMultiplier> parseItemMultiplier(String text) {
        if (text == null) {
            return Optional.empty();
        }
        int equals = text.lastIndexOf('=');
        if (equals < 1) {
            return Optional.empty();
        }
        String idText = text.substring(0, equals).trim();
        if (idText.startsWith("#")) {
            return Optional.empty();
        }
        ResourceLocation id = ResourceLocation.tryParse(idText);
        if (id == null || id.getPath().isEmpty()) {
            return Optional.empty();
        }
        double multiplier;
        try {
            multiplier = Double.parseDouble(text.substring(equals + 1).trim());
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
        return Double.isFinite(multiplier) && multiplier >= 0.0 && multiplier <= 100.0
                ? Optional.of(new ItemMultiplier(id, multiplier)) : Optional.empty();
    }

    /** The multipliers of a config list: the invalid lines are skipped and, if an id repeats, the first one wins. */
    public static Map<ResourceLocation, Double> itemMultipliers(List<? extends String> lines) {
        Map<ResourceLocation, Double> multipliers = new LinkedHashMap<>();
        for (String line : lines) {
            parseItemMultiplier(line).ifPresent(entry -> multipliers.putIfAbsent(entry.id(), entry.multiplier()));
        }
        return multipliers;
    }

    /** The reference price of a piece with all its durability: see the six-argument form. */
    public static long reference(ItemRating rating, double material, int enhancement, boolean weapon, TradeBalance balance) {
        return reference(rating, material, enhancement, weapon, 1.0, balance);
    }

    /**
     * The reference price: base by rating, times the material, plus a share per enhancement level (weapons only), times the
     * {@code durability} factor ({@link #durabilityFactor}) before the one rounding down; at least 1.
     */
    public static long reference(ItemRating rating, double material, int enhancement, boolean weapon, double durability, TradeBalance balance) {
        double levels = weapon ? Math.max(0, enhancement) : 0;
        double value = balance.basePrice().getOrDefault(rating, 1) * material * (1.0 + balance.enhancementBonus() * levels) * durability;
        return Math.max(1L, Money.floor(value));
    }

    /**
     * The price factor of the durability left: {@code minFactor} with none left, 1 with all of it, linear in between. A piece
     * with no durability ({@code maxDamage <= 0}) or a negative damage counts as new; more damage than the maximum, as broken.
     */
    public static double durabilityFactor(int damage, int maxDamage, double minFactor) {
        if (maxDamage <= 0 || damage < 0) {
            return 1.0;
        }
        if (damage > maxDamage) {
            return minFactor;
        }
        return minFactor + (1.0 - minFactor) * ((double) (maxDamage - damage) / maxDamage);
    }

    /** The price factor for a pressure: one step per point, never below the floor; a negative pressure is none. */
    public static double saturation(int pressure, TradeBalance balance) {
        if (pressure <= 0) {
            return 1.0;
        }
        return Math.max(balance.saturationFloor(), Math.pow(balance.saturationStep(), pressure));
    }

    /** Satisfy emeralds for a piece, at least 1. */
    public static long sellPrice(long reference, int pressure, TradeBalance balance) {
        double value = reference * balance.sellShare() * saturation(pressure, balance);
        return Math.max(1L, Money.floor(value));
    }

    /**
     * The pressure left after the days that went by since {@code lastDay}; days that go back count as none, a recovery of
     * 0 or less is none, and it never drops below 0 (nor overflows with extreme days).
     */
    public static int recovered(int pressure, long lastDay, long today, TradeBalance balance) {
        if (pressure <= 0) {
            return 0;
        }
        long days = today <= lastDay ? 0L : today - lastDay;
        if (days < 0) {
            days = Long.MAX_VALUE; // today > lastDay but the subtraction overflowed: a huge number of days
        }
        long perDay = Math.max(0, balance.recoveryPerDay());
        long drop = Math.min(days, Integer.MAX_VALUE) * perDay;
        return (int) Math.max(0L, pressure - drop);
    }

    /** Whether the price can be paid: at least 1 and not above the cap of the balance. */
    public static boolean payable(long price, TradeBalance balance) {
        return price >= 1 && price <= balance.maxSellEmeralds();
    }

    /** The parts a payment is delivered in: stacks of {@code maxStack} and the rest. Only for payable prices. */
    public static long[] payments(long price, int maxStack) {
        int stacks = (int) ((price + maxStack - 1) / maxStack);
        long[] parts = new long[stacks];
        long left = price;
        for (int i = 0; i < stacks; i++) {
            parts[i] = Math.min(maxStack, left);
            left -= parts[i];
        }
        return parts;
    }

    /** A piece of a batch as the price sees it: its saturation key and its reference price. */
    public record Piece(TradeKey key, long reference) {
    }

    /**
     * The price of each piece of a batch sold in order: every piece is priced with the pressure left by the ones before it
     * (same key = lower price). Pure: the saturation it receives is not changed.
     */
    public static long[] sellPrices(List<Piece> pieces, TradeSaturation saturation, long today, TradeBalance balance) {
        long[] prices = new long[pieces.size()];
        TradeSaturation current = saturation;
        for (int i = 0; i < prices.length; i++) {
            Piece piece = pieces.get(i);
            prices[i] = sellPrice(piece.reference(), current.pressure(piece.key(), today, balance), balance);
            current = current.recordSale(piece.key(), today, balance);
        }
        return prices;
    }

    public static long total(long[] prices) {
        long sum = 0L;
        for (long price : prices) {
            sum += price;
        }
        return sum;
    }

    /** Most pieces one order can carry. It matches the size of the inventory on purpose (pinned by a test). */
    public static final int MAX_BATCH = 36;

    /** Whether the slots of an order make sense: 1 to {@link #MAX_BATCH} entries, each below {@code inventorySize} and none twice. */
    public static boolean validBatch(List<Integer> slots, int inventorySize) {
        if (slots.isEmpty() || slots.size() > MAX_BATCH) {
            return false;
        }
        Set<Integer> seen = new HashSet<>();
        for (int slot : slots) {
            if (slot < 0 || slot >= inventorySize || !seen.add(slot)) {
                return false;
            }
        }
        return true;
    }

    /** The slots (ascending) with a piece that can be sold: not empty and with no refusal. */
    public static List<Integer> sellableSlots(int size, IntPredicate empty, IntFunction<Optional<Refusal>> refusal) {
        List<Integer> slots = new ArrayList<>();
        for (int slot = 0; slot < size; slot++) {
            if (!empty.test(slot) && refusal.apply(slot).isEmpty()) {
                slots.add(slot);
            }
        }
        return slots;
    }

    /** The durability factor as a whole percent, the number the player sees. */
    public static int durabilityPercent(double factor) {
        return (int) Math.round(factor * 100.0);
    }

    /** The saturation factor as a whole percent, the number the player sees. */
    public static int saturationPercent(int pressure, TradeBalance balance) {
        return (int) Math.round(saturation(pressure, balance) * 100.0);
    }

    public static long day(long gameTime) {
        return Math.floorDiv(gameTime, DAY_TICKS);
    }

    public static Optional<Refusal> refusal(int count, boolean damageable, boolean rated, boolean loaned) {
        if (loaned) {
            return Optional.of(Refusal.LOANED);
        }
        if (count > 1) {
            return Optional.of(Refusal.STACKED);
        }
        if (!damageable) {
            return Optional.of(Refusal.NO_VALUE);
        }
        if (!rated) {
            return Optional.of(Refusal.UNRATED);
        }
        return Optional.empty();
    }
}
