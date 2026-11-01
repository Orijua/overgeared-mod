package com.orijua.overgearedmod.feature.quest;

import com.orijua.overgearedmod.core.event.SatisfyPlayerEvent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

/** The time of a player's quest ran out: it failed for good. Posted after it was closed and its loans taken back. */
public final class QuestFailedEvent extends SatisfyPlayerEvent {
    private final ResourceLocation quest;

    public QuestFailedEvent(ServerPlayer player, ResourceLocation quest) {
        super(player);
        this.quest = quest;
    }

    public ResourceLocation quest() {
        return quest;
    }
}
