package com.orijua.overgearedmod.feature.capsule;

import net.minecraft.core.BlockPos;

import java.util.Optional;

/**
 * The pure rules of the VR capsule (own design: the canon only says a capsule is how Satisfy is entered and left; the
 * waiting time and the spawn rules below are the mod's adaptation to Minecraft).
 */
public final class CapsuleRules {
    /** Lying down and connecting takes 3 seconds. */
    public static final int CONNECT_TICKS = 60;
    /** How far the player may get from the capsule while connecting before it is cancelled. */
    public static final double MAX_DISTANCE = 3.0;

    private CapsuleRules() {
    }

    /** Where a connection lands. */
    public enum Source { LAST_SAVE, BAIRAN }

    public record Spawn(BlockPos pos, Source source) {
    }

    /**
     * Where the player appears in Satisfy: where they last disconnected, or, the first time (or if that is not known),
     * Bairan's plaza. Empty if the world has neither (an old world without the Satisfy sites).
     */
    public static Optional<Spawn> spawn(Optional<BlockPos> lastSave, Optional<BlockPos> bairan) {
        if (lastSave.isPresent()) {
            return Optional.of(new Spawn(lastSave.get(), Source.LAST_SAVE));
        }
        return bairan.map(pos -> new Spawn(pos, Source.BAIRAN));
    }

    /** Whether the saved state agrees with where the player is. */
    public enum Coherence { OK, CONNECTED_OUTSIDE, DISCONNECTED_INSIDE }

    /** OK: connected and in Satisfy, or disconnected and outside it. */
    public static Coherence coherence(CapsuleState state, boolean inSatisfy) {
        if (state.isConnected()) {
            return inSatisfy ? Coherence.OK : Coherence.CONNECTED_OUTSIDE;
        }
        return inSatisfy ? Coherence.DISCONNECTED_INSIDE : Coherence.OK;
    }

    /**
     * Whether a trip between dimensions is cancelled: the real world and Satisfy are separate worlds, and the only ways
     * across are the capsule (connect/disconnect) and the debug goto, which are {@code allowed}.
     */
    public static boolean crossingBlocked(boolean fromSatisfy, boolean toSatisfy, boolean allowed) {
        return fromSatisfy != toSatisfy && !allowed;
    }

    /** Whether the player is still close enough to the capsule to keep connecting. */
    public static boolean inReach(double distanceSquared) {
        return distanceSquared <= MAX_DISTANCE * MAX_DISTANCE;
    }
}
