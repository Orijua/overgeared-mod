package com.orijua.overgearedmod.feature.skill;

import com.orijua.overgearedmod.core.config.Balance;
import com.orijua.overgearedmod.core.registry.ModAttachments;
import com.orijua.overgearedmod.feature.progression.ProgressionRules;
import com.orijua.overgearedmod.feature.progression.ProgressionService;
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
        int intelligence = ProgressionService.effectiveStats(player).intelligence();
        Balance balance = Balance.current();
        float current = player.getData(ModAttachments.MANA);
        float regenerated = Math.min(ProgressionRules.maxMana(balance, intelligence),
                current + ProgressionRules.manaRegenPerSecond(balance, intelligence));
        if (regenerated != current) {
            player.setData(ModAttachments.MANA, regenerated);
        }
    }
}
