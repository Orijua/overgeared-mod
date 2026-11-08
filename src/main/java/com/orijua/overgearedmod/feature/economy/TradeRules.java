package com.orijua.overgearedmod.feature.economy;

import com.orijua.overgearedmod.core.config.TradeBalance;
import com.orijua.overgearedmod.feature.equipment.ItemRating;
import net.minecraft.network.chat.Component;

import java.util.Optional;

/**
 * The numbers of the trade, pure. Prices round down (the 1e-9 only absorbs floating point noise, so 3800.0000000001 or
 * 3799.9999999999 both mean 3800). The saturation is shared by selling and exporting.
 */
public final class TradeRules {
    /** Ticks in a game day. */
    public static final long DAY_TICKS = 24000L;
    private static final double EPSILON = 1e-9;

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

    /** The reference price: base by rating, times the material, plus a share per enhancement level (weapons only); at least 1. */
    public static long reference(ItemRating rating, double material, int enhancement, boolean weapon, TradeBalance balance) {
        double levels = weapon ? Math.max(0, enhancement) : 0;
        double value = balance.basePrice().getOrDefault(rating, 1) * material * (1.0 + balance.enhancementBonus() * levels);
        return Math.max(1L, (long) Math.floor(value + EPSILON));
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
        return Math.max(1L, (long) Math.floor(value + EPSILON));
    }

    /** Won for a piece, after the commission. */
    public static long exportPrice(long reference, int pressure, TradeBalance balance) {
        double value = reference * (double) balance.wonPerReference() * (1.0 - balance.exportCommission()) * saturation(pressure, balance);
        return Math.max(0L, (long) Math.floor(value + EPSILON));
    }

    public static long price(TradeMode mode, long reference, int pressure, TradeBalance balance) {
        return mode == TradeMode.SELL ? sellPrice(reference, pressure, balance) : exportPrice(reference, pressure, balance);
    }

    /** The pressure left after the days that went by since {@code lastDay}; days that go back count as none, and it never drops below 0. */
    public static int recovered(int pressure, long lastDay, long today, TradeBalance balance) {
        long days = Math.max(0L, today - lastDay);
        long left = pressure - Math.min(days, Integer.MAX_VALUE) * balance.recoveryPerDay();
        return (int) Math.max(0L, left);
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
