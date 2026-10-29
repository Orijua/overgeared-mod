package com.orijua.overgearedmod.progression;

import com.orijua.overgearedmod.Config;
import com.orijua.overgearedmod.registry.ModAttachments;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;

/** Server-side operations on a player's progression. The only place that writes PlayerStats. */
public final class ProgressionService {
    private ProgressionService() {
    }

    public static PlayerStats get(ServerPlayer player) {
        return player.getData(ModAttachments.PLAYER_STATS);
    }

    public static void set(ServerPlayer player, PlayerStats stats) {
        player.setData(ModAttachments.PLAYER_STATS, stats); // also syncs to the owning client
        StatEffects.apply(player, stats);
    }

    public static void awardXp(ServerPlayer player, int amount) {
        ProgressionRules.XpResult result = ProgressionRules.addXp(get(player), amount, Config.STAT_POINTS_PER_LEVEL.get());
        set(player, result.stats());
        if (result.levelsGained() > 0) {
            player.displayClientMessage(Component.translatable("message.overgearedmod.level_up", result.stats().level()), true);
            player.level().playSound(null, player.blockPosition(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 1.0f, 1.2f);
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
