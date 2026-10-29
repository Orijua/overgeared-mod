package com.orijua.overgearedmod.skill;

import com.orijua.overgearedmod.progression.PlayerStats;
import com.orijua.overgearedmod.progression.ProgressionService;
import com.orijua.overgearedmod.registry.ModAttachments;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.Optional;

/**
 * Server-authoritative skill use. The client only says "I pressed the key";
 * every rule (item, cooldown, mana) is checked here so a modified client can't cheat.
 */
public final class SkillService {
    private SkillService() {
    }

    public enum Result { SUCCESS, NO_SKILL, ON_COOLDOWN, NO_MANA }

    public static Result tryUseHeldSkill(ServerPlayer player) {
        Optional<Skill> found = Skills.forHeldItem(player.getMainHandItem());
        if (found.isEmpty()) {
            return Result.NO_SKILL;
        }
        Skill skill = found.get();
        SkillState state = player.getData(ModAttachments.SKILL_STATE);
        long now = player.level().getGameTime();

        long remaining = state.cooldownRemaining(now);
        if (remaining > 0) {
            player.displayClientMessage(Component.translatable("message.overgearedmod.cooldown",
                    skill.displayName(), String.format("%.1f", remaining / 20.0)), true);
            return Result.ON_COOLDOWN;
        }
        if (state.mana() < skill.manaCost()) {
            player.displayClientMessage(Component.translatable("message.overgearedmod.no_mana", skill.displayName()), true);
            return Result.NO_MANA;
        }

        PlayerStats stats = ProgressionService.get(player);
        skill.execute(player, stats);
        player.setData(ModAttachments.SKILL_STATE,
                new SkillState(state.mana() - skill.manaCost(), now + skill.cooldownTicks()));
        player.displayClientMessage(skill.displayName(), true);
        return Result.SUCCESS;
    }
}
