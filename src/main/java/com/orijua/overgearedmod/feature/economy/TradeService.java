package com.orijua.overgearedmod.feature.economy;

import com.orijua.overgearedmod.Overgearedmod;
import com.orijua.overgearedmod.core.config.TradeBalance;
import com.orijua.overgearedmod.core.registry.ModAttachments;
import com.orijua.overgearedmod.core.registry.ModItems;
import com.orijua.overgearedmod.feature.notification.NotificationEvents;
import com.orijua.overgearedmod.feature.notification.SystemNotification.Kind;
import com.orijua.overgearedmod.feature.world.Satisfy;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.Optional;

/**
 * Selling and exporting equipment: the only place that writes the trade saturation and takes the pieces. Everything is
 * decided here, in the server: the client only says which slot and which way. The piece goes first and comes back
 * if the payment fails, so nothing is lost or paid twice.
 */
public final class TradeService {
    private TradeService() {
    }

    public enum Result { SOLD, OUTSIDE_SATISFY, INVALID_SLOT, EMPTY, REFUSED, FAILED }

    public record Outcome(Result result, Optional<TradeRules.Refusal> refusal, long paid) {
        static Outcome of(Result result) {
            return new Outcome(result, Optional.empty(), 0L);
        }
    }

    /** How the money reaches the player; a seam so a test can make it fail. */
    @FunctionalInterface
    public interface Payout {
        void pay(ServerPlayer player, TradeMode mode, long amount);
    }

    public static TradeSaturation saturation(Player player) {
        return player.getData(ModAttachments.TRADE_SATURATION.get());
    }

    public static Outcome sell(ServerPlayer player, int slot, TradeMode mode) {
        return sell(player, slot, mode, TradeRules.day(player.serverLevel().getGameTime()));
    }

    public static Outcome sell(ServerPlayer player, int slot, TradeMode mode, long today) {
        return sell(player, slot, mode, today, TradeService::pay);
    }

    public static Outcome sell(ServerPlayer player, int slot, TradeMode mode, long today, Payout payout) {
        if (!Satisfy.is(player.level())) {
            return Outcome.of(Result.OUTSIDE_SATISFY);
        }
        Inventory inventory = player.getInventory();
        if (slot < 0 || slot >= Inventory.INVENTORY_SIZE) {
            return Outcome.of(Result.INVALID_SLOT);
        }
        ItemStack stack = inventory.getItem(slot);
        if (stack.isEmpty()) {
            return Outcome.of(Result.EMPTY);
        }
        Optional<TradeRules.Refusal> refusal = TradeGoods.refusal(stack);
        if (refusal.isPresent()) {
            return new Outcome(Result.REFUSED, refusal, 0L);
        }
        TradeBalance balance = TradeBalance.current();
        TradeKey key = TradeGoods.key(stack);
        TradeSaturation saturation = saturation(player);
        long price = TradeGoods.price(stack, mode, saturation, today, balance);
        // Checked before anything moves: the deposit throws on a negative amount, and a sale pays at least 1.
        if (price < 0 || (mode == TradeMode.SELL && price < 1)) {
            return Outcome.of(Result.FAILED);
        }
        ItemStack removed = stack.copy();
        inventory.setItem(slot, ItemStack.EMPTY);
        try {
            payout.pay(player, mode, price);
        } catch (RuntimeException e) {
            inventory.setItem(slot, removed);
            Overgearedmod.LOGGER.error("The {} of {} failed; the piece goes back to its slot", mode, player.getGameProfile().getName(), e);
            return Outcome.of(Result.FAILED);
        }
        player.setData(ModAttachments.TRADE_SATURATION.get(), saturation.recordSale(key, today, balance));
        Component name = removed.getHoverName();
        NotificationEvents.send(player, Kind.TRADE, List.of(Component.translatable(
                mode == TradeMode.SELL ? "notification.overgearedmod.trade.sold" : "notification.overgearedmod.trade.exported", name, price)));
        player.containerMenu.broadcastChanges();
        return new Outcome(Result.SOLD, Optional.empty(), price);
    }

    /** Emeralds of Satisfy into the inventory, in stacks, and what does not fit on the ground; Won into the account. */
    private static void pay(ServerPlayer player, TradeMode mode, long amount) {
        if (mode == TradeMode.EXPORT) {
            AccountService.deposit(player, amount);
            return;
        }
        long left = amount;
        while (left > 0) {
            int max = ModItems.SATISFY_EMERALD.get().getDefaultMaxStackSize();
            ItemStack pile = new ItemStack(ModItems.SATISFY_EMERALD.get(), (int) Math.min(max, left));
            left -= pile.getCount();
            player.getInventory().add(pile);
            if (!pile.isEmpty()) {
                player.drop(pile, false);
            }
        }
    }
}
