package com.orijua.overgearedmod.feature.capsule;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;

import java.util.Optional;

/**
 * What the server remembers of a player's capsule: where their body is while they are in Satisfy (the capsule and the
 * spot they step out to), and where they last disconnected in Satisfy. Saved with the player, never synced.
 */
public record CapsuleState(Optional<GlobalPos> capsule, Optional<GlobalPos> exit, Optional<BlockPos> lastSave) {
    public static final CapsuleState EMPTY = new CapsuleState(Optional.empty(), Optional.empty(), Optional.empty());

    public static final Codec<CapsuleState> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            GlobalPos.CODEC.optionalFieldOf("capsule").forGetter(CapsuleState::capsule),
            GlobalPos.CODEC.optionalFieldOf("exit").forGetter(CapsuleState::exit),
            BlockPos.CODEC.optionalFieldOf("last_save").forGetter(CapsuleState::lastSave)
    ).apply(instance, CapsuleState::new));

    /** Connected: the body stays in {@code capsule}, and the player will step out at {@code exit}. */
    public CapsuleState connected(GlobalPos capsule, GlobalPos exit) {
        return new CapsuleState(Optional.of(capsule), Optional.of(exit), lastSave);
    }

    /** Disconnected in Satisfy at {@code at}: that is where the next connection starts. The body is back in the real world. */
    public CapsuleState disconnected(BlockPos at) {
        return new CapsuleState(Optional.empty(), Optional.empty(), Optional.of(at));
    }

    /** The body is gone from the capsule (the player died in Satisfy): no capsule, no exit; the last save is kept. */
    public CapsuleState withoutBody() {
        return new CapsuleState(Optional.empty(), Optional.empty(), lastSave);
    }

    /** Whether the player is connected, i.e. their body lies in a capsule. The single definition of "connected". */
    public boolean isConnected() {
        return capsule.isPresent();
    }
}
