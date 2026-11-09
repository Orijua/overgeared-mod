package com.orijua.overgearedmod.feature.economy;

import com.orijua.overgearedmod.Overgearedmod;
import com.orijua.overgearedmod.core.config.DebtBalance;
import com.orijua.overgearedmod.core.config.TerminalBalance;
import com.orijua.overgearedmod.core.config.TradeBalance;
import com.orijua.overgearedmod.core.registry.ModAttachments;
import com.orijua.overgearedmod.core.registry.ModItems;
import com.orijua.overgearedmod.feature.character.CharacterSnapshot;
import com.orijua.overgearedmod.feature.character.CharacterVault;
import com.orijua.overgearedmod.feature.character.WorldSide;
import com.orijua.overgearedmod.feature.world.Satisfy;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;

/**
 * The account terminal and the phone: opens the window (never in Satisfy: there they show "No signal") and does the
 * daily work. Satisfy emeralds are only for spending in Satisfy, so nothing converts them to Won. Everything is decided
 * here, in the server: the client says what it saw and the checks come before anything moves. The only thing that
 * touches the stored character is {@link TerminalShop}, an exception to "nothing else moves items between the
 * characters": it only ADDS entries (emeralds and the pieces of the offer).
 */
public final class TerminalService {
    /** Orders already warned about (no known item or tag): the warning is logged once, not at every state. */
    private static final Set<String> WARNED_ORDERS = ConcurrentHashMap.newKeySet();

    private TerminalService() {
    }

    public enum Result { OK, NO_TERMINAL, IN_SATISFY, STALE, NOT_ENOUGH, ALREADY_WORKED }

    /**
     * The result of a work shift.
     *
     * @param won     the Won that went into the account (the pay minus what went to the debt)
     * @param toDebt  the Won that went to the debt
     * @param settled whether that paid the debt off
     */
    public record Outcome(Result result, long won, long toDebt, boolean settled) {
        static Outcome of(Result result) {
            return new Outcome(result, 0L, 0L, false);
        }
    }

    /** The message for the player after an attempt; empty when there is nothing to say. */
    public static Optional<Component> message(Outcome outcome) {
        return switch (outcome.result()) {
            case OK -> Optional.of(outcome.settled() ? DebtService.settledMessage()
                    : outcome.toDebt() > 0
                    ? Component.translatable("message.overgearedmod.terminal.worked_debt", outcome.won(), outcome.toDebt())
                    : Component.translatable("message.overgearedmod.terminal.worked", outcome.won()));
            case NO_TERMINAL -> Optional.empty();
            case IN_SATISFY -> Optional.of(Component.translatable("message.overgearedmod.terminal.no_signal"));
            case STALE -> Optional.of(Component.translatable("message.overgearedmod.terminal.stale"));
            case NOT_ENOUGH -> Optional.of(Component.translatable("message.overgearedmod.terminal.not_enough"));
            case ALREADY_WORKED -> Optional.of(Component.translatable("message.overgearedmod.terminal.already_worked"));
        };
    }

    public static void register(IEventBus gameBus) {
        // After the characters are settled (normal priority): the world the player is in is known.
        gameBus.addListener(EventPriority.LOWEST, TerminalService::onLoggedIn);
        gameBus.addListener(EventPriority.LOWEST, TerminalService::onChangedDimension);
    }

    private static void onLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && !(player instanceof FakePlayer)) {
            giveIfMissing(player);
        }
    }

    private static void onChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && !(player instanceof FakePlayer)) {
            giveIfMissing(player);
        }
    }

    /**
     * Gives the phone once, to a player who already got their starter capsule, while they carry the real world's things
     * (so it lands in the real inventory, never in Satisfy's). Someone who dies in Satisfy gets it at the next login or
     * change of dimension. Returns whether it was given now.
     */
    public static boolean giveIfMissing(ServerPlayer player) {
        if (!player.getData(ModAttachments.STARTER_CAPSULE_GIVEN.get()) || player.getData(ModAttachments.PHONE_GIVEN.get())
                || !carriesRealWorld(player)) {
            return false;
        }
        player.setData(ModAttachments.PHONE_GIVEN.get(), true);
        ItemStack phone = new ItemStack(ModItems.PHONE.get());
        if (!player.getInventory().add(phone)) {
            player.drop(phone, false);
        }
        player.sendSystemMessage(Component.translatable("message.overgearedmod.phone.given"));
        return true;
    }

    // ---- Opening ----

    /** Opens the terminal's window; in Satisfy nothing opens. {@code terminal} is the block, empty for the phone. */
    public static Result open(ServerPlayer player, Optional<BlockPos> terminal) {
        if (!carriesRealWorld(player)) {
            return Result.IN_SATISFY;
        }
        TerminalState state = state(player, Optional.empty());
        player.openMenu(new SimpleMenuProvider((id, inventory, p) -> {
                    TerminalMenu menu = new TerminalMenu(id, inventory, terminal);
                    menu.markSent(state.today());
                    return menu;
                },
                        Component.translatable("screen.overgearedmod.terminal.title")),
                buf -> TerminalState.STREAM_CODEC.encode(buf, state));
        return Result.OK;
    }

    // ---- State ----

    private static CharacterVault vault(ServerPlayer player) {
        return player.getData(ModAttachments.CHARACTER_VAULT.get());
    }

    /**
     * Whether the player is outside Satisfy AND carries the real world's things (vault live REAL). After a failed
     * settle they can be in the real world still carrying Satisfy's inventory: there the terminal must not touch it.
     */
    static boolean carriesRealWorld(ServerPlayer player) {
        return !Satisfy.is(player.level()) && vault(player).live().equals(Optional.of(WorldSide.REAL));
    }

    /** The stored Satisfy character, only while the player carries the real world's things and the snapshot can be read. */
    static Optional<CompoundTag> storedCharacter(ServerPlayer player) {
        CharacterVault vault = vault(player);
        if (!vault.live().equals(Optional.of(WorldSide.REAL))) {
            return Optional.empty();
        }
        return vault.stored().filter(CharacterSnapshot::isReadable);
    }

    public static long today(ServerPlayer player) {
        return TradeRules.day(player.serverLevel().getGameTime());
    }

    public static TerminalState state(ServerPlayer player, Optional<Component> message) {
        TerminalBalance balance = TerminalBalance.current();
        Optional<CompoundTag> stored = storedCharacter(player);
        int emeralds = stored.map(tag -> TerminalRules.countEmeralds(CharacterSnapshot.inventory(tag))).orElse(0);
        long today = today(player);
        WorkOrder order = TerminalRules.orderFor(today, balance.workOrders());
        ItemStack icon = order.icon();
        if (icon.is(Items.BARRIER) && WARNED_ORDERS.add(order.asString())) {
            Overgearedmod.LOGGER.warn("The work order {} has no known item or tag; it is shown with a barrier", order.asString());
        }
        // Without a character nothing can be bought, but the offer is still shown.
        int room = 0;
        int free = 0;
        if (stored.isPresent()) {
            ListTag inventory = CharacterSnapshot.inventory(stored.get());
            room = TerminalRules.emeraldRoom(inventory, new ItemStack(ModItems.SATISFY_EMERALD.get()).getMaxStackSize());
            free = TerminalRules.freeSlots(inventory);
        }
        TradeBalance trade = TradeBalance.current();
        return new TerminalState(stored.isPresent(), emeralds, balance.workPay(), today, order, icon,
                player.getData(ModAttachments.WORK_DAY.get()), balance.buyWonPerEmerald(), room, free,
                TerminalShop.offer(player.serverLevel(), today, balance, trade),
                player.getData(ModAttachments.OFFER_BOUGHT.get()).positionsOn(today), DebtBalance.current().quotaPercent(), message);
    }

    /** Sends the open menu a fresh state with the message of the last action. */
    public static void sendState(ServerPlayer player, TerminalMenu menu, Optional<Component> message) {
        TerminalState state = state(player, message);
        menu.markSent(state.today());
        PacketDistributor.sendToPlayer(player, new TerminalStatePayload(menu.containerId, state));
    }

    static boolean menuOpen(ServerPlayer player, int containerId) {
        return player.containerMenu instanceof TerminalMenu menu && menu.containerId == containerId && menu.stillValid(player);
    }

    // ---- Working ----

    /** The order of the terminal's window: it must be the open menu of the sender. */
    public static Outcome work(ServerPlayer player, TerminalWorkPayload payload) {
        if (Satisfy.is(player.level())) {
            return Outcome.of(Result.IN_SATISFY);
        }
        if (!menuOpen(player, payload.containerId())) {
            return Outcome.of(Result.NO_TERMINAL);
        }
        return work(player, payload.seen(), today(player), TerminalBalance.current());
    }

    /** Does the shift of {@code today} with the configured debt quota; see the other overload. */
    public static Outcome work(ServerPlayer player, WorkOrder seen, long today, TerminalBalance balance) {
        return work(player, seen, today, balance, DebtBalance.current());
    }

    /**
     * Does the shift of {@code today}: hands over today's order (inventory and offhand, never armor) and pays. While the
     * debt is active, its quota share of the pay goes to the debt and the rest to the balance.
     */
    public static Outcome work(ServerPlayer player, WorkOrder seen, long today, TerminalBalance balance, DebtBalance debt) {
        if (!carriesRealWorld(player)) {
            return Outcome.of(Result.IN_SATISFY);
        }
        if (TerminalRules.workedOn(player.getData(ModAttachments.WORK_DAY.get()), today)) {
            return Outcome.of(Result.ALREADY_WORKED);
        }
        WorkOrder order = TerminalRules.orderFor(today, balance.workOrders());
        if (!order.equals(seen)) {
            return Outcome.of(Result.STALE);
        }
        Inventory inventory = player.getInventory();
        if (countOrder(inventory, order) < order.count()) {
            return Outcome.of(Result.NOT_ENOUGH);
        }
        take(inventory, order);
        player.setData(ModAttachments.WORK_DAY.get(), today);
        AccountService.WorkPay pay = AccountService.payWork(player, balance.workPay(), debt.quotaPercent());
        if (pay.settled()) {
            DebtService.announceSettled(player);
        }
        // The terminal window has no slots: send the whole inventory menu so the "have" count updates.
        player.inventoryMenu.sendAllDataToRemote();
        return new Outcome(Result.OK, pay.paid() - pay.toDebt(), pay.toDebt(), pay.settled());
    }

    /** How many items of the order the inventory (main slots and offhand, not armor) holds. */
    public static int countOrder(Inventory inventory, WorkOrder order) {
        Predicate<ItemStack> matcher = order.matcher();
        long total = 0L;
        for (ItemStack stack : inventory.items) {
            if (matcher.test(stack)) {
                total += stack.getCount();
            }
        }
        for (ItemStack stack : inventory.offhand) {
            if (matcher.test(stack)) {
                total += stack.getCount();
            }
        }
        return (int) Math.min(total, Integer.MAX_VALUE);
    }

    /** Takes exactly the order's count: slots 0 to 35 in order, then the offhand. Only called when there is enough. */
    private static void take(Inventory inventory, WorkOrder order) {
        Predicate<ItemStack> matcher = order.matcher();
        int left = order.count();
        for (ItemStack stack : inventory.items) {
            left -= takeFrom(stack, matcher, left);
        }
        for (ItemStack stack : inventory.offhand) {
            left -= takeFrom(stack, matcher, left);
        }
    }

    private static int takeFrom(ItemStack stack, Predicate<ItemStack> matcher, int left) {
        if (left <= 0 || !matcher.test(stack)) {
            return 0;
        }
        int taken = Math.min(left, stack.getCount());
        stack.shrink(taken);
        return taken;
    }
}
