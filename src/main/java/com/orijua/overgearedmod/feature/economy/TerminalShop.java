package com.orijua.overgearedmod.feature.economy;

import com.orijua.overgearedmod.Overgearedmod;
import com.orijua.overgearedmod.core.config.TerminalBalance;
import com.orijua.overgearedmod.core.config.TradeBalance;
import com.orijua.overgearedmod.core.registry.Definitions;
import com.orijua.overgearedmod.core.registry.ModAttachments;
import com.orijua.overgearedmod.core.registry.ModItems;
import com.orijua.overgearedmod.feature.character.CharacterSnapshot;
import com.orijua.overgearedmod.feature.character.CharacterVault;
import com.orijua.overgearedmod.feature.equipment.EquipmentService;
import com.orijua.overgearedmod.feature.equipment.ItemRating;
import com.orijua.overgearedmod.feature.equipment.option.ItemOptionDefinition;
import com.orijua.overgearedmod.feature.world.Satisfy;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.levelgen.XoroshiroRandomSource;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Buying at the terminal and the phone: Satisfy emeralds paid with Won, and the daily offer of rated equipment. Both are
 * written into the stored Satisfy character (the vault's {@code stored} snapshot) and only ADD entries to it: nothing
 * takes emeralds out (they are only for spending in Satisfy). Everything is decided here, in the server:
 * the client says what it saw and the checks come before anything moves. The Won is charged first and the vault written
 * after, so a failure can lose money but never duplicate value. All of it is the mod's own design, not the canon's.
 *
 * <p>The offer is a pure function of (game day, rating, position): every player sees the same pieces and nothing is
 * saved; what a player already bought today is the {@code offer_bought} attachment, outside the vault.
 */
public final class TerminalShop {
    /** Item ids already warned about (unknown or not equipment): the warning is logged once, not at every state. */
    private static final Set<String> WARNED_ITEMS = ConcurrentHashMap.newKeySet();

    private TerminalShop() {
    }

    public enum Result { OK, NO_TERMINAL, IN_SATISFY, NO_CHARACTER, STALE, INVALID_AMOUNT, NOT_ENOUGH_WON, NO_ROOM, SOLD_OUT }

    /** {@code won} charged, the {@code amount} of emeralds bought (0 for a piece) and the piece {@code bought} (empty for emeralds). */
    public record Outcome(Result result, long won, int amount, ItemStack bought) {
        static Outcome of(Result result) {
            return new Outcome(result, 0L, 0, ItemStack.EMPTY);
        }
    }

    /** The message for the player after an attempt; empty when there is nothing to say. */
    public static Optional<Component> message(Outcome outcome) {
        return switch (outcome.result()) {
            case OK -> Optional.of(outcome.amount() > 0
                    ? Component.translatable("message.overgearedmod.terminal.bought_emeralds", outcome.amount(), outcome.won())
                    : Component.translatable("message.overgearedmod.terminal.bought_piece", outcome.bought().getHoverName(), outcome.won()));
            case NO_TERMINAL -> Optional.empty();
            case IN_SATISFY -> Optional.of(Component.translatable("message.overgearedmod.terminal.no_signal"));
            case NO_CHARACTER -> Optional.of(Component.translatable("message.overgearedmod.terminal.no_character_buy"));
            case STALE -> Optional.of(Component.translatable("message.overgearedmod.terminal.stale"));
            case INVALID_AMOUNT -> Optional.of(Component.translatable("message.overgearedmod.terminal.invalid_amount"));
            case NOT_ENOUGH_WON -> Optional.of(Component.translatable("message.overgearedmod.terminal.not_enough_won"));
            case NO_ROOM -> Optional.of(Component.translatable("message.overgearedmod.terminal.no_room"));
            case SOLD_OUT -> Optional.of(Component.translatable("message.overgearedmod.terminal.sold_out"));
        };
    }

    // ---- The offer ----

    /**
     * Today's offer, in position order: the first {@code offerPerRank} pieces are normal, the next ones rare, and so on up to
     * {@code offerMaxRating}. Each piece takes its type, rating options and price from its own seed, so the same day gives the
     * same offer to everyone. Types that are unknown or not equipment are left out (warned once); with none left, or with
     * {@code offerPerRank} 0, the offer is empty.
     */
    public static List<OfferEntry> offer(Level level, long day, TerminalBalance balance, TradeBalance trade) {
        List<Item> items = offerTypes(balance);
        int perRank = balance.offerPerRank();
        if (items.isEmpty() || perRank == 0) {
            return List.of();
        }
        int size = OfferRules.size(perRank, balance.offerMaxRating());
        List<Definitions.Entry<ItemOptionDefinition>> all = EquipmentService.itemOptions(level);
        List<OfferEntry> offer = new ArrayList<>(size);
        for (int position = 0; position < size; position++) {
            offerEntry(position, items, balance, trade, day, all).ifPresent(offer::add);
        }
        return List.copyOf(offer);
    }

    /**
     * The piece at {@code position} of today's offer, built alone: same seed as in the whole offer, so it is the same piece.
     * Empty when there are no valid types, {@code offerPerRank} is 0 or the position is outside the offer.
     */
    public static Optional<OfferEntry> offerEntry(Level level, long day, int position, TerminalBalance balance, TradeBalance trade) {
        return offerEntry(position, offerTypes(balance), balance, trade, day, EquipmentService.itemOptions(level));
    }

    private static Optional<OfferEntry> offerEntry(int position, List<Item> items, TerminalBalance balance, TradeBalance trade,
                                                   long day, List<Definitions.Entry<ItemOptionDefinition>> all) {
        int perRank = balance.offerPerRank();
        if (items.isEmpty() || perRank == 0 || position < 0 || position >= OfferRules.size(perRank, balance.offerMaxRating())) {
            return Optional.empty();
        }
        ItemRating rating = OfferRules.ratingAt(position, perRank);
        RandomSource random = new XoroshiroRandomSource(OfferRules.seed(day, rating, position));
        ItemStack stack = new ItemStack(items.get(random.nextInt(items.size())));
        EquipmentService.rateAs(stack, rating, random, all);
        long price = OfferRules.price(TradeGoods.reference(stack, trade), balance.buyMultiplier(), balance.wonPerEmerald());
        return Optional.of(new OfferEntry(position, stack, price));
    }

    /** The configured offer types that exist and are equipment, in config order (the others are warned once). */
    private static List<Item> offerTypes(TerminalBalance balance) {
        List<Item> items = new ArrayList<>();
        for (ResourceLocation id : balance.offerItems()) {
            Optional<Item> item = BuiltInRegistries.ITEM.getOptional(id);
            if (item.isEmpty() || !EquipmentService.isEquipment(new ItemStack(item.get()))) {
                if (WARNED_ITEMS.add(id.toString())) {
                    Overgearedmod.LOGGER.warn("The offer item {} is unknown or is not equipment; it is left out of the terminal's offer", id);
                }
                continue;
            }
            items.add(item.get());
        }
        return items;
    }

    // ---- Buying emeralds ----

    /** The order of the terminal's window: it must be the open menu of the sender. */
    public static Outcome buyEmeralds(ServerPlayer player, TerminalBuyEmeraldsPayload payload) {
        if (Satisfy.is(player.level())) {
            return Outcome.of(Result.IN_SATISFY);
        }
        if (!TerminalService.menuOpen(player, payload.containerId())) {
            return Outcome.of(Result.NO_TERMINAL);
        }
        return buyEmeralds(player, payload.amount(), payload.seenUnitPrice(), TerminalBalance.current());
    }

    /** Buys {@code n} emeralds into the stored character; {@code seenUnit} is the unit price the player saw. */
    public static Outcome buyEmeralds(ServerPlayer player, int n, int seenUnit, TerminalBalance balance) {
        if (!TerminalService.carriesRealWorld(player)) {
            return Outcome.of(Result.IN_SATISFY);
        }
        Optional<CompoundTag> stored = TerminalService.storedCharacter(player);
        if (stored.isEmpty()) {
            return Outcome.of(Result.NO_CHARACTER);
        }
        int unit = balance.buyWonPerEmerald();
        if (seenUnit != unit) {
            return Outcome.of(Result.STALE);
        }
        if (n < 1) {
            return Outcome.of(Result.INVALID_AMOUNT);
        }
        ListTag inventory = CharacterSnapshot.inventory(stored.get());
        int maxStack = new ItemStack(ModItems.SATISFY_EMERALD.get()).getMaxStackSize();
        if (n > TerminalRules.emeraldRoom(inventory, maxStack)) {
            return Outcome.of(Result.NO_ROOM);
        }
        long cost = TerminalRules.buyCost(n, unit);
        if (AccountService.won(player) < cost) {
            return Outcome.of(Result.NOT_ENOUGH_WON);
        }
        // What is pure is prepared first; then the Won and the vault: a failed write would lose money, never duplicate value.
        ListTag next = TerminalRules.addEmeralds(inventory, n, maxStack);
        AccountService.withdraw(player, cost);
        writeStored(player, stored.get(), next);
        return new Outcome(Result.OK, cost, n, ItemStack.EMPTY);
    }

    // ---- Buying a piece of the offer ----

    /** The order of the terminal's window: it must be the open menu of the sender. */
    public static Outcome buyOffer(ServerPlayer player, TerminalBuyOfferPayload payload) {
        if (Satisfy.is(player.level())) {
            return Outcome.of(Result.IN_SATISFY);
        }
        if (!TerminalService.menuOpen(player, payload.containerId())) {
            return Outcome.of(Result.NO_TERMINAL);
        }
        return buyOffer(player, payload.position(), payload.seen(), payload.seenPrice(), TerminalService.today(player),
                TerminalBalance.current(), TradeBalance.current());
    }

    /**
     * Buys the piece at {@code position} of the offer of {@code today}. The server regenerates the offer: the position must
     * exist and the piece and price must be the ones the player saw (else STALE).
     */
    public static Outcome buyOffer(ServerPlayer player, int position, ItemStack seen, long seenPrice, long today,
                                   TerminalBalance balance, TradeBalance trade) {
        if (!TerminalService.carriesRealWorld(player)) {
            return Outcome.of(Result.IN_SATISFY);
        }
        Optional<CompoundTag> stored = TerminalService.storedCharacter(player);
        if (stored.isEmpty()) {
            return Outcome.of(Result.NO_CHARACTER);
        }
        Optional<OfferEntry> found = offerEntry(player.level(), today, position, balance, trade);
        if (found.isEmpty()) {
            return Outcome.of(Result.STALE);
        }
        OfferEntry entry = found.get();
        if (seen.isEmpty() || !ItemStack.matches(entry.stack(), seen) || entry.price() != seenPrice) {
            return Outcome.of(Result.STALE);
        }
        if (player.getData(ModAttachments.OFFER_BOUGHT.get()).has(position, today)) {
            return Outcome.of(Result.SOLD_OUT);
        }
        ListTag inventory = CharacterSnapshot.inventory(stored.get());
        OptionalInt slot = TerminalRules.firstFreeSlot(inventory);
        if (slot.isEmpty()) {
            return Outcome.of(Result.NO_ROOM);
        }
        if (AccountService.won(player) < entry.price()) {
            return Outcome.of(Result.NOT_ENOUGH_WON);
        }
        // What is pure is prepared before charging; then the Won, the mark and the vault, in that order.
        CompoundTag item = (CompoundTag) entry.stack().copy().save(player.registryAccess(), new CompoundTag());
        ListTag next = TerminalRules.withEntryAt(inventory, slot.getAsInt(), item);
        AccountService.withdraw(player, entry.price());
        player.setData(ModAttachments.OFFER_BOUGHT.get(), player.getData(ModAttachments.OFFER_BOUGHT.get()).with(position, today));
        writeStored(player, stored.get(), next);
        return new Outcome(Result.OK, entry.price(), 0, entry.stack().copy());
    }

    private static void writeStored(ServerPlayer player, CompoundTag snapshot, ListTag inventory) {
        CharacterVault vault = player.getData(ModAttachments.CHARACTER_VAULT.get());
        player.setData(ModAttachments.CHARACTER_VAULT.get(), new CharacterVault(vault.live(),
                Optional.of(CharacterSnapshot.withInventory(snapshot, inventory))));
    }
}
