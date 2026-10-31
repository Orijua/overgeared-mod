package com.orijua.overgearedmod.progression;

import com.orijua.overgearedmod.Balance;
import com.orijua.overgearedmod.equipment.EquipRequirements;
import com.orijua.overgearedmod.equipment.EquipmentBonuses;
import com.orijua.overgearedmod.equipment.option.RolledOption;
import com.orijua.overgearedmod.event.LevelUpEvent;
import com.orijua.overgearedmod.playerclass.PlayerClassDefinition;
import com.orijua.overgearedmod.registry.Definitions;
import com.orijua.overgearedmod.registry.ModAttachments;
import com.orijua.overgearedmod.registry.ModDataComponents;
import com.orijua.overgearedmod.registry.ModDatapackRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.NeoForge;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Progression of a player: the only place that writes PlayerStats, and the one place that
 * gathers the stat sources (allocated points, class, titles) into the effective stats.
 */
public final class ProgressionService {
    private ProgressionService() {
    }

    /** Allocated stats only, as shown on the "+" rows. */
    public static PlayerStats get(Player player) {
        return player.getData(ModAttachments.PLAYER_STATS);
    }

    /** Allocated stats plus every other source in {@link #modifiers}. Use this for every gameplay check; works on both sides. */
    public static PlayerStats effectiveStats(Player player) {
        return ProgressionRules.effective(get(player), modifiers(player));
    }

    public static int effectiveSmithingLevel(Player player) {
        return get(player).smithingLevel() + playerClass(player).map(PlayerClassDefinition::smithingBonus).orElse(0);
    }

    public static Optional<PlayerClassDefinition> playerClass(Player player) {
        return player.getData(ModAttachments.PLAYER_IDENTITY).playerClass()
                .flatMap(id -> Definitions.get(player.level(), ModDatapackRegistries.PLAYER_CLASS, id));
    }

    /**
     * Every contribution to the base stats, each tagged with where it comes from (the stat tooltip lists them).
     * A new source (equipment, talents, sets, buffs, guild...) only needs to add its modifiers here.
     */
    public static List<StatModifier> modifiers(Player player) {
        List<StatModifier> modifiers = new ArrayList<>(StatModifier.allocated(get(player)));
        playerClass(player).ifPresent(c -> modifiers.addAll(StatModifier.fromBonuses(new StatSource(c.name()), c.bonusStats())));
        for (var titleId : player.getData(ModAttachments.PLAYER_IDENTITY).titles()) {
            Definitions.get(player.level(), ModDatapackRegistries.TITLE, titleId)
                    .ifPresent(t -> modifiers.addAll(StatModifier.fromBonuses(new StatSource(t.name()), t.bonusStats())));
        }
        modifiers.addAll(equipmentModifiers(player, ProgressionRules.effective(get(player), modifiers)));
        return modifiers;
    }

    /**
     * Stat options (e.g. Fracaso's agility) of the equipment the player wears. Requirements are checked against
     * {@code withoutEquipment}, so an item can't meet its own requirements with the stats it gives.
     */
    private static List<StatModifier> equipmentModifiers(Player player, PlayerStats withoutEquipment) {
        List<StatModifier> modifiers = new ArrayList<>();
        for (ItemStack stack : EquipmentBonuses.wornEquipment(player)) {
            EquipRequirements requirements = stack.get(ModDataComponents.REQUIREMENTS);
            if (requirements != null && !requirements.metBy(withoutEquipment)) {
                continue;
            }
            StatSource source = new StatSource(stack.getHoverName());
            for (RolledOption rolled : stack.getOrDefault(ModDataComponents.ITEM_OPTIONS, List.<RolledOption>of())) {
                Definitions.get(player.level(), ModDatapackRegistries.ITEM_OPTION, rolled.option())
                        .ifPresent(def -> def.effect().statModifiers(source, rolled.value(), modifiers::add));
            }
        }
        return modifiers;
    }

    public static void set(ServerPlayer player, PlayerStats stats) {
        player.setData(ModAttachments.PLAYER_STATS, stats);
        refreshEffects(player);
    }

    /** Re-applies attribute modifiers; call after anything that changes effective stats (requirements may change too). */
    public static void refreshEffects(Player player) {
        StatEffects.apply(player, effectiveStats(player));
        EquipmentBonuses.apply(player);
    }

    public static void awardXp(ServerPlayer player, int amount) {
        ProgressionRules.XpResult result = ProgressionRules.addXp(Balance.current(), get(player), amount);
        set(player, result.stats());
        if (result.levelsGained() > 0) {
            player.displayClientMessage(Component.translatable("message.overgearedmod.level_up", result.stats().level()), true);
            player.level().playSound(null, player.blockPosition(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 1.0f, 1.2f);
            int newLevel = result.stats().level();
            NeoForge.EVENT_BUS.post(new LevelUpEvent(player, newLevel - result.levelsGained(), newLevel));
        }
    }

    /** Spends up to {@code amount} free points; the rule caps it at what's left and ignores bad amounts from the client. */
    public static void allocatePoints(ServerPlayer player, StatType stat, int amount) {
        PlayerStats before = get(player);
        PlayerStats after = ProgressionRules.allocate(before, stat, amount);
        if (after != before) {
            set(player, after);
        }
    }

    public static void addSmithingXp(ServerPlayer player, int amount) {
        PlayerStats stats = get(player);
        set(player, stats.withSmithingXp(stats.smithingXp() + amount));
    }
}
