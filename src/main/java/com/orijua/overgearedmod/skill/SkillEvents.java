package com.orijua.overgearedmod.skill;

import com.orijua.overgearedmod.progression.PlayerStats;
import com.orijua.overgearedmod.progression.ProgressionRules;
import com.orijua.overgearedmod.progression.ProgressionService;
import com.orijua.overgearedmod.registry.ModAttachments;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

public final class SkillEvents {
    private SkillEvents() {
    }

    public static void register(IEventBus gameBus) {
        gameBus.addListener(SkillEvents::onPlayerTick);
    }

    /** Mana regen, once per second. Only writes (and so syncs) when the value actually changes. */
    private static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || player.tickCount % 20 != 0) {
            return;
        }
        PlayerStats stats = ProgressionService.get(player);
        SkillState state = player.getData(ModAttachments.SKILL_STATE);
        float max = stats.maxMana();
        float mana = Math.min(max, state.mana() + ProgressionRules.manaRegenPerSecond(stats.intelligence()));
        if (mana != state.mana()) {
            player.setData(ModAttachments.SKILL_STATE, state.withMana(mana));
        }
    }
}
