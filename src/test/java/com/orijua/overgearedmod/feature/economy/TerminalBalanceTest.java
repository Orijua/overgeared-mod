package com.orijua.overgearedmod.feature.economy;

import com.orijua.overgearedmod.core.config.TerminalBalance;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
}
