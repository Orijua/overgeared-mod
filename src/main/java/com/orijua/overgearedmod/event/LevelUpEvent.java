package com.orijua.overgearedmod.event;

import net.minecraft.server.level.ServerPlayer;

/** A player gained one or more Satisfy levels at once. Posted after the new stats are saved. */
public final class LevelUpEvent extends SatisfyPlayerEvent {
    private final int previousLevel;
    private final int newLevel;

    public LevelUpEvent(ServerPlayer player, int previousLevel, int newLevel) {
        super(player);
        this.previousLevel = previousLevel;
        this.newLevel = newLevel;
    }

    public int previousLevel() {
        return previousLevel;
    }

    public int newLevel() {
        return newLevel;
    }
}
