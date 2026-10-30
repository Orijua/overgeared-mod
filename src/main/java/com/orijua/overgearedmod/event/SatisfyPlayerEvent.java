package com.orijua.overgearedmod.event;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.Event;

/**
 * Base of the mod's own domain events: "something happened to this player" on the server.
 * Systems post them on {@code NeoForge.EVENT_BUS} and others subscribe, so the poster doesn't
 * know who reacts (like a MediatR notification in .NET).
 */
public abstract class SatisfyPlayerEvent extends Event {
    private final ServerPlayer player;

    protected SatisfyPlayerEvent(ServerPlayer player) {
        this.player = player;
    }

    public ServerPlayer player() {
        return player;
    }
}
