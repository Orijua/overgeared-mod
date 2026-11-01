package com.orijua.overgearedmod.feature.npc;

import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.player.Player;

/** What every NPC of the mod shares. */
public final class NpcRules {
    private NpcRules() {
    }

    /** NPCs only take damage from creative players or what bypasses invulnerability (/kill, the void). */
    public static boolean isInvulnerableTo(DamageSource source) {
        boolean creativePlayer = source.getEntity() instanceof Player player && player.isCreative();
        return !creativePlayer && !source.is(DamageTypeTags.BYPASSES_INVULNERABILITY);
    }
}
