package com.orijua.overgearedmod.title;

import com.orijua.overgearedmod.goal.PlayerCounters;
import com.orijua.overgearedmod.progression.PlayerIdentity;
import com.orijua.overgearedmod.progression.PlayerStats;
import com.orijua.overgearedmod.progression.ProgressionService;
import com.orijua.overgearedmod.registry.Definitions;
import com.orijua.overgearedmod.registry.ModAttachments;
import com.orijua.overgearedmod.registry.ModDatapackRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;

public final class TitleService {
    private TitleService() {
    }

    /** Grants every title whose goal is now met. Called by GoalTracker after any countable event. */
    public static void checkUnlocks(ServerPlayer player) {
        PlayerIdentity identity = player.getData(ModAttachments.PLAYER_IDENTITY);
        PlayerCounters counters = player.getData(ModAttachments.PLAYER_COUNTERS);
        PlayerStats stats = ProgressionService.get(player);
        boolean changed = false;

        for (Definitions.Entry<TitleDefinition> title : Definitions.all(player.level(), ModDatapackRegistries.TITLE)) {
            if (identity.hasTitle(title.id()) || title.value().goal().progress(counters, stats) < title.value().goal().target()) {
                continue;
            }
            identity = identity.withTitle(title.id());
            changed = true;
            player.displayClientMessage(Component.translatable("message.overgearedmod.title_earned", title.value().name()), false);
            player.level().playSound(null, player.blockPosition(), SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.PLAYERS, 0.7f, 1.0f);
        }
        if (changed) {
            player.setData(ModAttachments.PLAYER_IDENTITY, identity);
            ProgressionService.refreshEffects(player);
        }
    }
}
