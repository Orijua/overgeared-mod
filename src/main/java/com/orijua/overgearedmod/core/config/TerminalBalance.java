package com.orijua.overgearedmod.core.config;

import com.orijua.overgearedmod.feature.economy.TerminalRules;
import com.orijua.overgearedmod.feature.economy.WorkOrder;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

/**
 * The tunable numbers of the account terminal, apart from {@link Balance}. Rules take it as a parameter so they stay pure;
 * the game passes {@link #current()} (the server config), tests pass {@link #DEFAULT}. Every number is the mod's own
 * design, not the canon's. The commission of the conversion is not here: it is the trading site's one,
 * {@link TradeBalance#commission()}.
 *
 * @param wonPerEmerald Won that one Satisfy emerald is worth
 * @param workPay       Won paid for one work shift
 * @param workOrders    the daily orders, taken in turn by game day
 */
public record TerminalBalance(int wonPerEmerald, long workPay, List<WorkOrder> workOrders) {
    public static final TerminalBalance DEFAULT = new TerminalBalance(100, 1000L, List.of(
            new WorkOrder(ResourceLocation.withDefaultNamespace("coals"), true, 32),
            new WorkOrder(ResourceLocation.fromNamespaceAndPath("c", "ingots/iron"), true, 16),
            new WorkOrder(ResourceLocation.withDefaultNamespace("wheat"), false, 64),
            new WorkOrder(ResourceLocation.withDefaultNamespace("logs"), true, 32),
            new WorkOrder(ResourceLocation.fromNamespaceAndPath("c", "cobblestones"), true, 64)));

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
