package com.orijua.overgearedmod.feature.economy;

import com.orijua.overgearedmod.core.config.CapsuleBalance;
import com.orijua.overgearedmod.core.registry.ModAttachments;
import com.orijua.overgearedmod.core.registry.ModItems;
import com.orijua.overgearedmod.feature.capsule.CapsuleLicenseItem;
import com.orijua.overgearedmod.feature.capsule.CapsuleTier;
import com.orijua.overgearedmod.feature.world.Satisfy;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * The capsule licenses on sale at the terminal: the Won is charged and the license goes to the inventory the player
 * carries (the real world's). Nothing is touched until every check has passed, so a refused order changes nothing.
 * The license is then used up, with the capsule of the tier below, in a crafting table (see the recipes).
 */
public final class LicenseShop {
    private LicenseShop() {
    }

    public enum Result {
        OK, OK_DROPPED, NO_TERMINAL, IN_SATISFY, INVALID_TIER, STALE, DEBT_ACTIVE, NEEDS_ADVANCED, ALREADY_HAVE, NOT_ENOUGH_WON
    }

    /** {@code won} charged for the license of {@code tier} (0 when nothing was bought). */
    public record Outcome(Result result, CapsuleTier tier, long won) {
        static Outcome of(Result result, CapsuleTier tier) {
            return new Outcome(result, tier, 0L);
        }
    }

    /** The message for the player after an attempt; empty when there is nothing to say. */
    public static Optional<Component> message(Outcome outcome) {
        return switch (outcome.result()) {
            case OK -> Optional.of(Component.translatable("message.overgearedmod.terminal.license.bought", name(outcome.tier()), outcome.won()));
            case OK_DROPPED -> Optional.of(Component.translatable("message.overgearedmod.terminal.license.dropped", name(outcome.tier()), outcome.won()));
            case NO_TERMINAL -> Optional.empty();
            case IN_SATISFY -> Optional.of(Component.translatable("message.overgearedmod.terminal.no_signal"));
            case INVALID_TIER -> Optional.of(Component.translatable("message.overgearedmod.terminal.license.invalid_tier"));
            case STALE -> Optional.of(Component.translatable("message.overgearedmod.terminal.stale"));
            case DEBT_ACTIVE -> Optional.of(Component.translatable("message.overgearedmod.terminal.license.debt_active"));
            case NEEDS_ADVANCED -> Optional.of(Component.translatable("message.overgearedmod.terminal.license.needs_advanced"));
            case ALREADY_HAVE -> Optional.of(Component.translatable("message.overgearedmod.terminal.license.already_have"));
            case NOT_ENOUGH_WON -> Optional.of(Component.translatable("message.overgearedmod.terminal.not_enough_won"));
        };
    }

    private static Component name(CapsuleTier tier) {
        return tier == CapsuleTier.BASIC ? tier.tierName() : CapsuleLicenseItem.stackFor(tier).getHoverName();
    }

    // ---- The offer ----

    /** The licenses on sale, from the advanced one up, with their price and lock for this player. */
    public static List<LicenseOffer> offers(ServerPlayer player, CapsuleBalance balance) {
        DebtState debt = AccountService.get(player).debtState();
        boolean hasAdvanced = hasAdvanced(player);
        List<LicenseOffer> offers = new ArrayList<>();
        for (CapsuleTier tier : CapsuleTier.values()) {
            if (tier == CapsuleTier.BASIC) {
                continue;
            }
            offers.add(new LicenseOffer(tier, balance.licenseWon(tier),
                    LicenseRules.lock(tier, debt, hasAdvanced, carriesLicense(player, tier))));
        }
        return List.copyOf(offers);
    }

    /** Whether the player owns an advanced capsule or better: one in hand or inventory, or their last connection was with one. */
    static boolean hasAdvanced(ServerPlayer player) {
        Inventory inventory = player.getInventory();
        return player.getData(ModAttachments.CAPSULE_TIER.get()).atLeast(CapsuleTier.ADVANCED)
                || anyOf(inventory, ModItems.ADVANCED_CAPSULE.get()) || anyOf(inventory, ModItems.DIAMOND_CAPSULE.get());
    }

    /** Whether the player carries the license of {@code tier}: in the inventory, the off hand or the cursor. */
    static boolean carriesLicense(ServerPlayer player, CapsuleTier tier) {
        Item license = CapsuleLicenseItem.stackFor(tier).getItem();
        return anyOf(player.getInventory(), license) || player.containerMenu.getCarried().is(license);
    }

    /** The main inventory (hotbar included) and the off hand. */
    private static boolean anyOf(Inventory inventory, Item item) {
        for (ItemStack stack : inventory.items) {
            if (stack.is(item)) {
                return true;
            }
        }
        for (ItemStack stack : inventory.offhand) {
            if (stack.is(item)) {
                return true;
            }
        }
        return false;
    }

    // ---- Buying ----

    /** The order of the terminal's window: it must be the open menu of the sender. */
    public static Outcome buy(ServerPlayer player, TerminalBuyLicensePayload payload) {
        if (Satisfy.is(player.level())) {
            return Outcome.of(Result.IN_SATISFY, payload.tier());
        }
        if (!TerminalService.menuOpen(player, payload.containerId())) {
            return Outcome.of(Result.NO_TERMINAL, payload.tier());
        }
        return buy(player, payload.tier(), payload.seenPrice(), CapsuleBalance.current());
    }

    /** Buys the license of {@code tier}; {@code seenPrice} is the price the player saw. */
    public static Outcome buy(ServerPlayer player, CapsuleTier tier, long seenPrice, CapsuleBalance balance) {
        if (!TerminalService.carriesRealWorld(player)) {
            return Outcome.of(Result.IN_SATISFY, tier);
        }
        if (tier == CapsuleTier.BASIC) {
            return Outcome.of(Result.INVALID_TIER, tier);
        }
        long price = balance.licenseWon(tier);
        if (seenPrice != price) {
            return Outcome.of(Result.STALE, tier);
        }
        switch (LicenseRules.lock(tier, AccountService.get(player).debtState(), hasAdvanced(player), carriesLicense(player, tier))) {
            case DEBT_ACTIVE -> {
                return Outcome.of(Result.DEBT_ACTIVE, tier);
            }
            case NEEDS_ADVANCED -> {
                return Outcome.of(Result.NEEDS_ADVANCED, tier);
            }
            case ALREADY_HAVE -> {
                return Outcome.of(Result.ALREADY_HAVE, tier);
            }
            case OPEN -> {
            }
        }
        if (AccountService.won(player) < price) {
            return Outcome.of(Result.NOT_ENOUGH_WON, tier);
        }
        AccountService.withdraw(player, price);
        ItemStack license = CapsuleLicenseItem.stackFor(tier);
        boolean fitted = player.getInventory().add(license);
        if (!fitted) {
            player.drop(license, false);
        }
        player.inventoryMenu.sendAllDataToRemote();
        return new Outcome(fitted ? Result.OK : Result.OK_DROPPED, tier, price);
    }
}
