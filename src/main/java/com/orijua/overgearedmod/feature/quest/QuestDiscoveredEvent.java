package com.orijua.overgearedmod.feature.quest;

import com.orijua.overgearedmod.core.event.SatisfyPlayerEvent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

/** A player met a hidden quest's secret condition (discover_when): it now shows up and can be accepted. */
public final class QuestDiscoveredEvent extends SatisfyPlayerEvent {
    private final ResourceLocation quest;

    public QuestDiscoveredEvent(ServerPlayer player, ResourceLocation quest) {
        super(player);
        this.quest = quest;
    }

    public ResourceLocation quest() {
        return quest;
    }
}
