package com.orijua.overgearedmod.feature.mob;

import com.orijua.overgearedmod.core.event.SatisfyPlayerEvent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;

/** A player killed a non-player mob. Posted after the kill XP is awarded. */
public final class MobKilledEvent extends SatisfyPlayerEvent {
    private final LivingEntity victim;

    public MobKilledEvent(ServerPlayer player, LivingEntity victim) {
        super(player);
        this.victim = victim;
    }

    public LivingEntity victim() {
        return victim;
    }
}
