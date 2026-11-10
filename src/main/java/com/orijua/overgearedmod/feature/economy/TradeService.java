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
import javax.annotation.Nullable;

/**
 * Selling equipment for Satisfy emeralds: the only place that writes the trade saturation and takes the pieces. Everything
 * is decided here, in the server: the client only says which slots, and which piece at which price it saw. Checks come
 * before anything moves (including that the piece and the price are the ones the player saw, and that the price can be
 * paid). The piece goes first and the payment is delivered in parts, each all or nothing: if the first part fails the
 * piece goes back without overwriting anything; if a later one fails the piece is NOT returned (part of the payment is
 * already delivered, giving it back would duplicate value), the sale is counted and the failure is logged with what was
 * paid and what is missing. Only {@link RuntimeException} is caught, as before: an {@link Error} brings the server thread
 * down anyway.
 */
public final class TradeService {
    private TradeService() {
    }

    public enum Result { SOLD, OUTSIDE_SATISFY, INVALID_SLOT, EMPTY, REFUSED, STALE, FAILED }

    public record Outcome(Result result, Optional<TradeRules.Refusal> refusal, long paid) {
        static Outcome of(Result result) {
            return new Outcome(result, Optional.empty(), 0L);
        }
    }

    /** The piece and the price the player saw when he decided; the sale only goes through if both still hold. */
    public record Expected(ItemStack stack, long price) {
    }

    /**
     * What a batch did: {@code sold} = pieces that left the inventory (including a piece whose payment failed halfway),
     * {@code paid} = emeralds really delivered, {@code skipped} = pieces that stay (stale, refused, empty slot),
     * {@code failed} = pieces whose payment failed (total or partial), and the first result that was not a plain sale
     * (SOLD if every piece sold).
     */
    public record Batch(int sold, long paid, int skipped, int failed, Result first) {
    }

    /**
     * How one part of the money reaches the player; a seam so a test can make it fail. Each call is all or nothing:
     * it delivers its whole part or throws without delivering anything.
     */
    @FunctionalInterface
    public interface Payout {
        void give(ServerPlayer player, long amount);
    }

    public static TradeSaturation saturation(Player player) {
        return player.getData(ModAttachments.TRADE_SATURATION.get());
    }

    /** The message for the player after a sale attempt; empty when the notification already says it. */
    public static Optional<Component> message(Outcome outcome) {
        return switch (outcome.result()) {
            case SOLD -> Optional.empty();
            case OUTSIDE_SATISFY -> Optional.of(Component.translatable("message.overgearedmod.trade_outside"));
            case REFUSED -> outcome.refusal().map(refusal ->
                    (Component) Component.translatable("message.overgearedmod.trade_refused", refusal.message()));
            case EMPTY, INVALID_SLOT, STALE -> Optional.of(Component.translatable("message.overgearedmod.trade_stale"));
            case FAILED -> Optional.of(Component.translatable("message.overgearedmod.trade_failed"));
        };
    }

    /** The message for the player after a batch; empty when the notification already says it. */
    public static Optional<Component> message(Batch batch) {
        if (batch.first() == Result.OUTSIDE_SATISFY) {
            return Optional.of(Component.translatable("message.overgearedmod.trade_outside"));
        }
        if (batch.first() == Result.INVALID_SLOT && batch.sold() == 0) {
            return Optional.of(Component.translatable("message.overgearedmod.trade_stale"));
        }
        if (batch.failed() > 0) {
            return Optional.of(Component.translatable("message.overgearedmod.trade_failed"));
        }
        if (batch.skipped() > 0) {
            return Optional.of(Component.translatable("message.overgearedmod.trade_skipped", batch.skipped()));
        }
        return Optional.empty();
    }

    /** The order of the Trade tab: one batch. Today is the server clock; each piece and price must be the ones the player saw. */
    public static Batch sell(ServerPlayer player, TradeSellPayload payload) {
        return sellAll(player, payload.entries(), TradeRules.day(player.serverLevel().getGameTime()), TradeService::give);
    }

    /**
     * Sells the pieces of the batch in order, each as {@link #sellOne}. A bad batch (outside Satisfy, a repeated or out of
     * range slot, too many entries) is refused whole before anything moves; later a piece that went stale or cannot be paid
     * is just skipped. One notification and one inventory update for the whole batch.
     */
    public static Batch sellAll(ServerPlayer player, List<TradeSellPayload.Entry> entries, long today, Payout payout) {
        if (!Satisfy.is(player.level())) {
            return new Batch(0, 0L, entries.size(), 0, Result.OUTSIDE_SATISFY);
        }
        if (!TradeRules.validBatch(entries.stream().map(TradeSellPayload.Entry::slot).toList(), Inventory.INVENTORY_SIZE)) {
            return new Batch(0, 0L, entries.size(), 0, Result.INVALID_SLOT);
        }
        int sold = 0;
        int skipped = 0;
        int failed = 0;
        long paid = 0L;
        Result first = null;
        Component lastName = Component.empty();
        for (TradeSellPayload.Entry entry : entries) {
            Component name = player.getInventory().getItem(entry.slot()).getHoverName();
            Outcome outcome = sellOne(player, entry.slot(), today, new Expected(entry.expected(), entry.expectedPrice()), payout);
            if (outcome.result() == Result.SOLD) {
                sold++;
                paid += outcome.paid();
                lastName = name;
            } else {
                if (outcome.result() == Result.FAILED) {
                    failed++;
                    if (outcome.paid() > 0L) {
                        sold++;
                        paid += outcome.paid();
                        lastName = name;
                    }
                } else {
                    skipped++;
                }
                if (first == null) {
                    first = outcome.result();
                }
            }
        }
        if (sold == 1) {
            NotificationEvents.send(player, Kind.TRADE, List.of(Component.translatable("notification.overgearedmod.trade.sold", lastName, Money.format(paid))));
        } else if (sold > 1) {
            NotificationEvents.send(player, Kind.TRADE, List.of(Component.translatable("notification.overgearedmod.trade.sold_many", sold, Money.format(paid))));
        }
        player.containerMenu.broadcastChanges();
        return new Batch(sold, paid, skipped, failed, first == null ? Result.SOLD : first);
    }

    public static Outcome sell(ServerPlayer player, int slot) {
        return sell(player, slot, TradeRules.day(player.serverLevel().getGameTime()));
    }

    public static Outcome sell(ServerPlayer player, int slot, long today) {
        return sell(player, slot, today, TradeService::give);
    }

    /** Without the piece and price the player saw: only for tests and commands. */
    public static Outcome sell(ServerPlayer player, int slot, long today, Payout payout) {
        return sell(player, slot, today, null, payout);
    }

    /** With the piece and price the player saw and the real payment. */
    public static Outcome sell(ServerPlayer player, int slot, long today, Expected expected) {
        return sell(player, slot, today, expected, TradeService::give);
    }

    /** One piece with its own notification and inventory update. */
    public static Outcome sell(ServerPlayer player, int slot, long today, @Nullable Expected expected, Payout payout) {
        Component name = slot >= 0 && slot < Inventory.INVENTORY_SIZE ? player.getInventory().getItem(slot).getHoverName() : Component.empty();
        Outcome outcome = sellOne(player, slot, today, expected, payout);
        if (outcome.result() == Result.SOLD || (outcome.result() == Result.FAILED && outcome.paid() > 0L)) {
            NotificationEvents.send(player, Kind.TRADE, List.of(Component.translatable("notification.overgearedmod.trade.sold", name, Money.format(outcome.paid()))));
        }
        player.containerMenu.broadcastChanges();
        return outcome;
    }

    /** One piece, no notification and no inventory update: the caller does them once. */
    private static Outcome sellOne(ServerPlayer player, int slot, long today, @Nullable Expected expected, Payout payout) {
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
        if (expected != null && !ItemStack.matches(stack, expected.stack())) {
            return Outcome.of(Result.STALE);
        }
        Optional<TradeRules.Refusal> refusal = TradeGoods.refusal(stack);
        if (refusal.isPresent()) {
            return new Outcome(Result.REFUSED, refusal, 0L);
        }
        TradeBalance balance = TradeBalance.current();
        TradeKey key = TradeGoods.key(stack);
        TradeSaturation saturation = saturation(player);
        long price = TradeGoods.price(stack, saturation, today, balance);
        if (expected != null && price != expected.price()) {
            return Outcome.of(Result.STALE);
        }
        // Checked before anything moves: a price must be at least 1 and a sale cannot pay above the cap.
        if (!TradeRules.payable(price, balance)) {
            return Outcome.of(Result.FAILED);
        }
        long[] parts = TradeRules.payments(price, ModItems.SATISFY_EMERALD.get().getDefaultMaxStackSize());
        ItemStack removed = stack.copy();
        inventory.setItem(slot, ItemStack.EMPTY);
        long delivered = 0L;
        for (long part : parts) {
            try {
                payout.give(player, part);
            } catch (RuntimeException e) {
                if (delivered == 0L) {
                    giveBack(player, slot, removed);
                    Overgearedmod.LOGGER.error("The sale of {} failed before paying anything; the piece goes back", player.getGameProfile().getName(), e);
                    return Outcome.of(Result.FAILED);
                }
                player.setData(ModAttachments.TRADE_SATURATION.get(), saturation.recordSale(key, today, balance));
                Overgearedmod.LOGGER.error("The sale of {} failed halfway: {} of {} paid, {} missing; the piece is not returned",
                        player.getGameProfile().getName(), delivered, price, price - delivered, e);
                return new Outcome(Result.FAILED, Optional.empty(), delivered);
            }
            delivered += part;
        }
        player.setData(ModAttachments.TRADE_SATURATION.get(), saturation.recordSale(key, today, balance));
        return new Outcome(Result.SOLD, Optional.empty(), price);
    }

    /** Puts the piece back in its slot only if empty; otherwise in the inventory, and what does not fit on the ground. Never overwrites. */
    private static void giveBack(ServerPlayer player, int slot, ItemStack piece) {
        Inventory inventory = player.getInventory();
        if (inventory.getItem(slot).isEmpty()) {
            inventory.setItem(slot, piece);
            return;
        }
        inventory.add(piece);
        if (!piece.isEmpty()) {
            player.drop(piece, false);
        }
    }

    /** One part: emeralds of Satisfy into the inventory (what does not fit on the ground). */
    private static void give(ServerPlayer player, long amount) {
        ItemStack pile = new ItemStack(ModItems.SATISFY_EMERALD.get(), (int) amount);
        player.getInventory().add(pile);
        if (!pile.isEmpty()) {
            player.drop(pile, false);
        }
    }
}
