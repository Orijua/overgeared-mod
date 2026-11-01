package com.orijua.overgearedmod.feature.progression;

import com.orijua.overgearedmod.core.event.SatisfyPlayerEvent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

/**
 * A player got a class: chosen at the start ({@code granted} false) or given by a quest's
 * class_reward ({@code granted} true). Posted after its skills and bonuses apply.
 */
public final class ClassChangedEvent extends SatisfyPlayerEvent {
    private final ResourceLocation playerClass;
    private final boolean granted;

    public ClassChangedEvent(ServerPlayer player, ResourceLocation playerClass, boolean granted) {
        super(player);
        this.playerClass = playerClass;
        this.granted = granted;
    }

    public ResourceLocation playerClass() {
        return playerClass;
    }

    public boolean granted() {
        return granted;
    }
}
