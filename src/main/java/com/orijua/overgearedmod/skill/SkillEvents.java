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
        gameBus.addListener(SkillEvents::regenerateMana);
    }

    /** Once per second; only writes (and syncs) when the value changes. */
    private static void regenerateMana(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || player.tickCount % 20 != 0) {
            return;
        }
        PlayerStats stats = ProgressionService.get(player);
        float current = player.getData(ModAttachments.MANA);
        float regenerated = Math.min(stats.maxMana(), current + ProgressionRules.manaRegenPerSecond(stats.intelligence()));
        if (regenerated != current) {
            player.setData(ModAttachments.MANA, regenerated);
        }
    }
}
