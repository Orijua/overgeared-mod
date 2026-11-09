package com.orijua.overgearedmod.core.config;

import com.orijua.overgearedmod.feature.economy.TerminalRules;
import com.orijua.overgearedmod.feature.economy.WorkOrder;
import com.orijua.overgearedmod.feature.equipment.ItemRating;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * The tunable numbers of the account terminal, apart from {@link Balance}. Rules take it as a parameter so they stay pure;
 * the game passes {@link #current()} (the server config), tests pass {@link #DEFAULT}. Every number is the mod's own
 * design, not the canon's. Satisfy emeralds are only for spending in Satisfy: nothing converts them to Won.
 *
 * @param wonPerEmerald    Won that one Satisfy emerald is worth (the reference of the offer prices)
 * @param workPay          Won paid for one work shift
 * @param workOrders       the daily orders, taken in turn by game day
 * @param buyWonPerEmerald Won that buying one Satisfy emerald costs
 * @param buyMultiplier    how many times its reference a piece of the daily offer costs
 * @param offerPerRank     pieces of the daily offer for each rating, from normal up to {@code offerMaxRating}
 * @param offerMaxRating   the highest rating the daily offer reaches
 * @param offerItems       the item types the daily offer draws from (equipment only; the config does not accept an empty list: to turn the offer off use {@code offerPerRank} 0; the
 *                         record itself tolerates an empty list, which gives an empty offer)
 */
public record TerminalBalance(int wonPerEmerald, long workPay, List<WorkOrder> workOrders, int buyWonPerEmerald, double buyMultiplier,
                              int offerPerRank, ItemRating offerMaxRating, List<ResourceLocation> offerItems) {
    /** The most pieces per rating the offer can have (the bought-today set is sized for it). */
    public static final int MAX_OFFER_PER_RANK = 12;

    private static final List<ResourceLocation> DEFAULT_OFFER_ITEMS = defaultOfferItems();

    public static final TerminalBalance DEFAULT = new TerminalBalance(100, 1000L, List.of(
            new WorkOrder(ResourceLocation.withDefaultNamespace("coals"), true, 32),
            new WorkOrder(ResourceLocation.fromNamespaceAndPath("c", "ingots/iron"), true, 16),
            new WorkOrder(ResourceLocation.withDefaultNamespace("wheat"), false, 64),
            new WorkOrder(ResourceLocation.withDefaultNamespace("logs"), true, 32),
            new WorkOrder(ResourceLocation.fromNamespaceAndPath("c", "cobblestones"), true, 64)),
            400, 4.0, 6, ItemRating.RARE, DEFAULT_OFFER_ITEMS);

    private static List<ResourceLocation> defaultOfferItems() {
        List<ResourceLocation> items = new ArrayList<>();
        for (String material : List.of("wooden", "stone", "iron", "golden")) {
            for (String tool : List.of("sword", "pickaxe", "axe", "shovel")) {
                items.add(ResourceLocation.withDefaultNamespace(material + "_" + tool));
            }
        }
        for (String material : List.of("leather", "chainmail", "iron", "golden")) {
            for (String piece : List.of("helmet", "chestplate", "leggings", "boots")) {
                items.add(ResourceLocation.withDefaultNamespace(material + "_" + piece));
            }
        }
        return List.copyOf(items);
    }

    /** The balance without the buying numbers: they take the defaults (for the code and tests that only care about the rest). */
    public TerminalBalance(int wonPerEmerald, long workPay, List<WorkOrder> workOrders) {
        this(wonPerEmerald, workPay, workOrders, 400, 4.0, 6, ItemRating.RARE, DEFAULT_OFFER_ITEMS);
    }

    public TerminalBalance {
        workOrders = List.copyOf(workOrders);
        if (workOrders.isEmpty()) {
            throw new IllegalArgumentException("a terminal needs at least one work order");
        }
        for (WorkOrder order : workOrders) {
            if (order.count() < 1 || order.count() > TerminalRules.MAX_ORDER_COUNT) {
                throw new IllegalArgumentException("a work order needs a count between 1 and " + TerminalRules.MAX_ORDER_COUNT + ": " + order);
            }
        }
        if (buyWonPerEmerald < 1) {
            throw new IllegalArgumentException("buyWonPerEmerald must be at least 1: " + buyWonPerEmerald);
        }
        if (!Double.isFinite(buyMultiplier) || buyMultiplier < 1.0) {
            throw new IllegalArgumentException("buyMultiplier must be a finite number of at least 1: " + buyMultiplier);
        }
        if (offerPerRank < 0 || offerPerRank > MAX_OFFER_PER_RANK) {
            throw new IllegalArgumentException("offerPerRank must be between 0 and " + MAX_OFFER_PER_RANK + ": " + offerPerRank);
        }
        Objects.requireNonNull(offerMaxRating, "offerMaxRating");
        offerItems = List.copyOf(offerItems);
    }

    /** This balance with another price for buying an emerald (for tests). */
    public TerminalBalance withBuy(int buyWonPerEmerald) {
        return new TerminalBalance(wonPerEmerald, workPay, workOrders, buyWonPerEmerald, buyMultiplier, offerPerRank, offerMaxRating, offerItems);
    }

    /** This balance with another daily offer (for tests). */
    public TerminalBalance withOffer(int offerPerRank, ItemRating offerMaxRating, List<ResourceLocation> offerItems) {
        return new TerminalBalance(wonPerEmerald, workPay, workOrders, buyWonPerEmerald, buyMultiplier, offerPerRank, offerMaxRating, offerItems);
    }

    /** Set only inside {@link #withOverride}; read by {@link #current()} before the config. */
    private static TerminalBalance override;

    /** The active server config, or the defaults before a world is loaded (e.g. on the title screen). */
    public static TerminalBalance current() {
        if (override != null) {
            return override;
        }
        return Config.isLoaded() ? Config.terminalBalance() : DEFAULT;
    }

    /** For game tests: {@link #current()} returns {@code balance} while {@code action} runs, then the config again. */
    public static void withOverride(TerminalBalance balance, Runnable action) {
        TerminalBalance previous = override;
        override = balance;
        try {
            action.run();
        } finally {
            override = previous;
        }
    }
}
