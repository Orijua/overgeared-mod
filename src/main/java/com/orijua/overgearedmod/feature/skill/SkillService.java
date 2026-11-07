package com.orijua.overgearedmod.feature.skill;

import com.orijua.overgearedmod.core.registry.Definitions;
import com.orijua.overgearedmod.core.registry.ModAttachments;
import com.orijua.overgearedmod.core.registry.ModDataComponents;
import com.orijua.overgearedmod.core.registry.ModDatapackRegistries;
import com.orijua.overgearedmod.feature.equipment.EquipmentEvents;
import com.orijua.overgearedmod.feature.equipment.option.RolledOption;
import com.orijua.overgearedmod.feature.progression.ProgressionService;
import com.orijua.overgearedmod.feature.world.Satisfy;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.NeoForge;

import java.util.List;
import java.util.Optional;

/** Server-side skill operations: learning, slotting and casting. Every rule is checked here. */
public final class SkillService {
    /** The fourth skill key: casts the held weapon's own skill instead of a learned one. */
    public static final int WEAPON_SLOT = PlayerSkills.SLOT_COUNT;

    public enum Result { SUCCESS, EMPTY_SLOT, UNKNOWN_SKILL, WRONG_ITEM, REQUIREMENTS_UNMET, ON_COOLDOWN, NO_MANA, REAL_WORLD }

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

    /** Casts the skill in key slot {@code slot}, or the held weapon's skill for {@link #WEAPON_SLOT}. */
    public static Result use(ServerPlayer player, int slot) {
        if (!Satisfy.is(player.level())) {
            return Result.REAL_WORLD;
        }
        Optional<ResourceLocation> id = slot == WEAPON_SLOT ? weaponSkill(player.getMainHandItem()) : get(player).slot(slot);
        if (id.isEmpty()) {
            return Result.EMPTY_SLOT;
        }
        return cast(player, id.get());
    }

    /**
     * The skill a weapon lets its holder cast (a skill option, e.g. Bisection on a Legendary Fracaso), if any.
     * Works on both sides, so the HUD can show it.
     */
    public static Optional<ResourceLocation> weaponSkill(ItemStack weapon) {
        for (RolledOption rolled : weapon.getOrDefault(ModDataComponents.ITEM_OPTIONS, List.<RolledOption>of())) {
            Optional<ResourceLocation> skill = Definitions.get(ModDatapackRegistries.ITEM_OPTION, rolled.option())
                    .flatMap(def -> def.effect().grantedSkill());
            if (skill.isPresent()) {
                return skill;
            }
        }
        return Optional.empty();
    }

    /** Checks the item, requirements, cooldown and mana, then casts. The skill need not be learned (weapon skills). */
    private static Result cast(ServerPlayer player, ResourceLocation id) {
        PlayerSkills skills = get(player);
        Optional<SkillDefinition> found = definition(player, id);
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
        long remaining = skills.cooldownRemaining(id, now);
        if (remaining > 0) {
            return fail(player, Result.ON_COOLDOWN, Component.translatable("message.overgearedmod.cooldown",
                    skill.name(), String.format("%.1f", remaining / 20.0)));
        }
        float mana = player.getData(ModAttachments.MANA);
        if (mana < skill.manaCost()) {
            return fail(player, Result.NO_MANA, Component.translatable("message.overgearedmod.no_mana", skill.name()));
        }

        skill.effect().apply(player, ProgressionService.effectiveStats(player));
        player.setData(ModAttachments.MANA, mana - skill.manaCost());
        player.setData(ModAttachments.PLAYER_SKILLS, skills.withCooldown(id, now + skill.cooldownTicks()));
        player.displayClientMessage(skill.name(), true);
        NeoForge.EVENT_BUS.post(new SkillUsedEvent(player, id));
        return Result.SUCCESS;
    }

    private static Result fail(ServerPlayer player, Result result, Component message) {
        player.displayClientMessage(message, true);
        return result;
    }
}
