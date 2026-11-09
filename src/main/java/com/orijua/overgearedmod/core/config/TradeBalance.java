package com.orijua.overgearedmod.core.config;

import com.orijua.overgearedmod.feature.equipment.ItemRating;

import java.util.Map;

/**
 * The tunable numbers of the trade (selling equipment for Satisfy emeralds), apart from {@link Balance}. Rules take it as a
 * parameter so they stay pure; the game passes {@link #current()} (the server config), tests pass {@link #DEFAULT}.
 * Every number is the mod's own design, not the canon's.
 *
 * @param basePrice          reference price of a piece of each rating, before material and enhancement
 * @param materialMultiplier multiplier by material (wood, stone, iron, gold, diamond, netherite, leather, chainmail)
 * @param otherMaterial      multiplier of any material that is not in the map
 * @param enhancementBonus   share of the reference added per enhancement level (weapons only)
 * @param sellShare          share of the reference paid in Satisfy emeralds when selling
 * @param commission         the trading site's commission when converting Satisfy emeralds at the terminal (0.05: the player gets 95 %)
 * @param saturationStep     price factor for each point of pressure
 * @param saturationFloor    the price factor never goes below this
 * @param recoveryPerDay     points of pressure that go away each whole game day
 * @param pressureCap        pressure never grows beyond this
 * @param maxSellEmeralds    a sale that would pay more Satisfy emeralds than this is refused before the piece is touched
 */
public record TradeBalance(
        Map<ItemRating, Integer> basePrice,
        Map<String, Double> materialMultiplier,
        double otherMaterial,
        double enhancementBonus,
        double sellShare,
        double commission,
        double saturationStep,
        double saturationFloor,
        int recoveryPerDay,
        int pressureCap,
        long maxSellEmeralds) {

    public static final TradeBalance DEFAULT = new TradeBalance(
            Map.of(ItemRating.NORMAL, 3, ItemRating.RARE, 12, ItemRating.EPIC, 40,
                    ItemRating.UNIQUE, 150, ItemRating.LEGENDARY, 600, ItemRating.MYTH, 2500),
            Map.of("wood", 0.5, "gold", 0.5, "leather", 0.5, "stone", 1.0, "iron", 1.0, "chainmail", 1.0,
                    "diamond", 2.0, "netherite", 3.0),
            1.0, 0.10, 0.40, 0.05, 0.95, 0.15, 2, 37, 10_000L);

    public TradeBalance {
        basePrice = Map.copyOf(basePrice);
        materialMultiplier = Map.copyOf(materialMultiplier);
    }

    /** Set only inside {@link #withOverride}; read by {@link #current()} before the config. */
    private static TradeBalance override;

    /** The active server config, or the defaults before a world is loaded (e.g. on the title screen). */
    public static TradeBalance current() {
        if (override != null) {
            return override;
        }
        return Config.isLoaded() ? Config.tradeBalance() : DEFAULT;
    }

    /** For game tests: {@link #current()} returns {@code balance} while {@code action} runs, then the config again. */
    public static void withOverride(TradeBalance balance, Runnable action) {
        TradeBalance previous = override;
        override = balance;
        try {
            action.run();
        } finally {
            override = previous;
        }
    }
}
