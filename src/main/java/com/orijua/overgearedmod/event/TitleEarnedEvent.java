package com.orijua.overgearedmod.event;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

/** A player met a title's goal and now has it (its bonuses already apply). */
public final class TitleEarnedEvent extends SatisfyPlayerEvent {
    private final ResourceLocation title;

    public TitleEarnedEvent(ServerPlayer player, ResourceLocation title) {
        super(player);
        this.title = title;
    }

    public ResourceLocation title() {
        return title;
    }
}
