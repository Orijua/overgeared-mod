package com.orijua.overgearedmod.skill;

import com.orijua.overgearedmod.equipment.EquipmentEvents;
import com.orijua.overgearedmod.progression.ProgressionService;
import com.orijua.overgearedmod.registry.Definitions;
import com.orijua.overgearedmod.registry.ModAttachments;
import com.orijua.overgearedmod.registry.ModDatapackRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

import java.util.Optional;

/** Server-side skill operations: learning, slotting and casting. Every rule is checked here. */
public final class SkillService {
    public enum Result { SUCCESS, EMPTY_SLOT, UNKNOWN_SKILL, WRONG_ITEM, REQUIREMENTS_UNMET, ON_COOLDOWN, NO_MANA }

    private SkillService() {
    }

    public static PlayerSkills get(ServerPlayer player) {
        return player.getData(ModAttachments.PLAYER_SKILLS);
    }

    public static Optional<SkillDefinition> definition(ServerPlayer player, ResourceLocation id) {
        return Definitions.get(player.level(), ModDatapackRegistries.SKILL, id);
    }

    /** @return false if the skill doesn't exist or is already known. */
    public static boolean learn(ServerPlayer player, ResourceLocation skill) {
        PlayerSkills skills = get(player);
        if (skills.knows(skill) || definition(player, skill).isEmpty()) {
            return false;
        }
        player.setData(ModAttachments.PLAYER_SKILLS, skills.learn(skill));
        return true;
    }

    public static void assign(ServerPlayer player, int slot, Optional<ResourceLocation> skill) {
        PlayerSkills skills = get(player);
        if (slot < 0 || slot >= PlayerSkills.SLOT_COUNT || skill.filter(s -> !skills.knows(s)).isPresent()) {
            return;
        }
        player.setData(ModAttachments.PLAYER_SKILLS, skills.withSlot(slot, skill));
    }

    public static Result use(ServerPlayer player, int slot) {
        PlayerSkills skills = get(player);
        Optional<ResourceLocation> id = skills.slot(slot);
        if (id.isEmpty()) {
            return Result.EMPTY_SLOT;
        }
        Optional<SkillDefinition> found = definition(player, id.get());
        if (found.isEmpty()) {
            return Result.UNKNOWN_SKILL;
        }
        SkillDefinition skill = found.get();
        long now = player.level().getGameTime();

        if (!skill.canUseWith(player.getMainHandItem())) {
            return fail(player, Result.WRONG_ITEM, Component.translatable("message.overgearedmod.wrong_item", skill.name()));
        }
        if (!EquipmentEvents.meetsRequirements(player, player.getMainHandItem())) {
            return fail(player, Result.REQUIREMENTS_UNMET, Component.translatable("message.overgearedmod.requirements_unmet"));
        }
        long remaining = skills.cooldownRemaining(id.get(), now);
        if (remaining > 0) {
            return fail(player, Result.ON_COOLDOWN, Component.translatable("message.overgearedmod.cooldown",
                    skill.name(), String.format("%.1f", remaining / 20.0)));
        }
        float mana = player.getData(ModAttachments.MANA);
        if (mana < skill.manaCost()) {
            return fail(player, Result.NO_MANA, Component.translatable("message.overgearedmod.no_mana", skill.name()));
        }

        skill.effect().apply(player, ProgressionService.get(player));
        player.setData(ModAttachments.MANA, mana - skill.manaCost());
        player.setData(ModAttachments.PLAYER_SKILLS, skills.withCooldown(id.get(), now + skill.cooldownTicks()));
        player.displayClientMessage(skill.name(), true);
        return Result.SUCCESS;
    }

    private static Result fail(ServerPlayer player, Result result, Component message) {
        player.displayClientMessage(message, true);
        return result;
    }
}
