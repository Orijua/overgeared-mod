package com.orijua.overgearedmod.progression;

import com.orijua.overgearedmod.Balance;
import com.orijua.overgearedmod.equipment.EquipmentBonuses;
import com.orijua.overgearedmod.event.LevelUpEvent;
import com.orijua.overgearedmod.playerclass.PlayerClassDefinition;
import com.orijua.overgearedmod.registry.Definitions;
import com.orijua.overgearedmod.registry.ModAttachments;
import com.orijua.overgearedmod.registry.ModDatapackRegistries;
import com.orijua.overgearedmod.title.TitleDefinition;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.common.NeoForge;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Progression of a player: the only place that writes PlayerStats, and the one place that
 * knows how class and title bonuses combine into the effective stats.
 */
public final class ProgressionService {
    private ProgressionService() {
    }

    /** Allocated stats only, as shown on the "+" rows. */
    public static PlayerStats get(Player player) {
        return player.getData(ModAttachments.PLAYER_STATS);
    }

    /** Allocated stats plus class and title bonuses. Use this for every gameplay check; works on both sides. */
    public static PlayerStats effectiveStats(Player player) {
        return ProgressionRules.withBonuses(get(player), bonuses(player));
    }

    public static int effectiveSmithingLevel(Player player) {
        return get(player).smithingLevel() + playerClass(player).map(PlayerClassDefinition::smithingBonus).orElse(0);
    }

    public static Optional<PlayerClassDefinition> playerClass(Player player) {
        return player.getData(ModAttachments.PLAYER_IDENTITY).playerClass()
                .flatMap(id -> Definitions.get(player.level(), ModDatapackRegistries.PLAYER_CLASS, id));
    }

    private static List<Map<StatType, Integer>> bonuses(Player player) {
        List<Map<StatType, Integer>> bonuses = new ArrayList<>();
        playerClass(player).ifPresent(c -> bonuses.add(c.bonusStats()));
        for (var titleId : player.getData(ModAttachments.PLAYER_IDENTITY).titles()) {
            Definitions.get(player.level(), ModDatapackRegistries.TITLE, titleId).map(TitleDefinition::bonusStats).ifPresent(bonuses::add);
        }
        return bonuses;
    }

    public static void set(ServerPlayer player, PlayerStats stats) {
        player.setData(ModAttachments.PLAYER_STATS, stats);
        refreshEffects(player);
    }

    /** Re-applies attribute modifiers; call after anything that changes effective stats (requirements may change too). */
    public static void refreshEffects(ServerPlayer player) {
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

    public static void allocatePoint(ServerPlayer player, StatType stat) {
        PlayerStats before = get(player);
        PlayerStats after = ProgressionRules.allocate(before, stat);
        if (after != before) {
            set(player, after);
        }
    }

    public static void addSmithingXp(ServerPlayer player, int amount) {
        PlayerStats stats = get(player);
        set(player, stats.withSmithingXp(stats.smithingXp() + amount));
    }
}
