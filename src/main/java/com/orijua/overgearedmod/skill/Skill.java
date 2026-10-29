package com.orijua.overgearedmod.skill;

import com.orijua.overgearedmod.progression.PlayerStats;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

/**
 * An active skill. Implementations only describe the effect; cooldown, mana and validation
 * are handled once in {@link SkillService}. Add a new skill by implementing this and listing it in {@link Skills}.
 */
public interface Skill {
    String id();

    Component displayName();

    float manaCost();

    int cooldownTicks();

    /** Which held item enables this skill (e.g. any sword). */
    boolean canUseWith(ItemStack mainHand);

    /** Runs on the server after all checks passed. */
    void execute(ServerPlayer player, PlayerStats stats);
}
