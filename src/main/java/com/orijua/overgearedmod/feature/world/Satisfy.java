package com.orijua.overgearedmod.feature.world;

import com.orijua.overgearedmod.Overgearedmod;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;

import java.util.Optional;

/** The Satisfy dimension: the game world, where Bairan and the canon places are. The overworld is the real world. */
public final class Satisfy {
    public static final ResourceKey<Level> LEVEL = ResourceKey.create(Registries.DIMENSION, Overgearedmod.id("satisfy"));

    /** For tests: the game-test server drops the datapack's dimensions, so a test lends the level that plays Satisfy. */
    private static volatile ServerLevel standIn;

    private Satisfy() {
    }

    public static boolean is(Level level) {
        return level.dimension() == LEVEL || level == standIn;
    }

    /** Same as {@link #is(Level)}, by dimension key (for events that only carry the destination). */
    public static boolean is(ResourceKey<Level> key) {
        return key == LEVEL || (standIn != null && standIn.dimension() == key);
    }

    public static Optional<ServerLevel> of(MinecraftServer server) {
        ServerLevel level = server.getLevel(LEVEL);
        return Optional.ofNullable(level != null ? level : standIn);
    }

    /** Also for tests: {@code null} gives it back. */
    public static void standIn(ServerLevel level) {
        standIn = level;
    }
}
