package com.orijua.overgearedmod.feature.world;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.structure.StructurePiece;

/** What the site searches ask of the terrain, in one place: is the ground at a block land, not the bottom of water. */
final class SiteTerrain {
    private SiteTerrain() {
    }

    /** Whether the ground at (x, z) is dry: no water over it (the surface is the sea or lake floor). */
    static boolean dryAt(ChunkGenerator generator, RandomState random, LevelHeightAccessor height, int x, int z) {
        int surface = generator.getBaseHeight(x, z, Heightmap.Types.WORLD_SURFACE_WG, height, random);
        int ground = generator.getBaseHeight(x, z, Heightmap.Types.OCEAN_FLOOR_WG, height, random);
        return surface == ground;
    }

    /** Whether the ground under the middle of a piece is land, not the bottom of a lake, river or sea. */
    static boolean onDryLand(StructurePiece piece, ChunkGenerator generator, RandomState random, LevelHeightAccessor height) {
        BlockPos middle = piece.getBoundingBox().getCenter();
        return dryAt(generator, random, height, middle.getX(), middle.getZ());
    }
}
