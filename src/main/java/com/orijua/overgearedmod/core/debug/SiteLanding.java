package com.orijua.overgearedmod.core.debug;

import com.orijua.overgearedmod.feature.world.AshurManor;
import com.orijua.overgearedmod.feature.world.NorthEndCave;
import com.orijua.overgearedmod.feature.world.Winston;
import com.orijua.overgearedmod.feature.world.WorldSites;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureStart;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Where {@code /satisfy debug goto} drops the player: on the surface, in the open, in front of the place. /locate only
 * gives a structure's chunk and no height, so teleporting there tends to bury the player in a mountain.
 */
public final class SiteLanding {
    /** The places that can be visited, by the name typed in the command. */
    public static final List<String> SITES = List.of(WorldSites.BAIRAN, WorldSites.NORTH_END_CAVE, WorldSites.ASHUR_MANOR,
            WorldSites.WINSTON);
    private static final Map<String, ResourceKey<Structure>> STRUCTURES = Map.of(
            WorldSites.NORTH_END_CAVE, NorthEndCave.KEY,
            WorldSites.ASHUR_MANOR, AshurManor.KEY,
            WorldSites.WINSTON, Winston.KEY);
    /** How far in front of the first piece (the cave mouth, the manor, Winston's square) the player lands. */
    private static final int GAP = 3;

    private SiteLanding() {
    }

    /**
     * The block to stand on near {@code site} in the Satisfy {@code level}, or empty if this world has no such place
     * (an old world, or one where no site was found). Bairan lands on its plaza; the others land just south of their
     * first piece, facing it.
     */
    public static Optional<BlockPos> find(ServerLevel level, String site) {
        if (site.equals(WorldSites.BAIRAN)) {
            return WorldSites.landingFor(level.getSeed()).map(landing -> surface(level, landing.getX(), landing.getZ()));
        }
        ResourceKey<Structure> key = STRUCTURES.get(site);
        if (key == null) {
            return Optional.empty();
        }
        Structure structure = level.registryAccess().registryOrThrow(Registries.STRUCTURE).get(key);
        return WorldSites.siteFor(level.getSeed(), site).flatMap(chunk -> {
            StructureStart start = level.getChunk(chunk.x, chunk.z, ChunkStatus.STRUCTURE_STARTS).getStartForStructure(structure);
            if (structure == null || start == null || !start.isValid()) {
                return Optional.empty();
            }
            BoundingBox first = start.getPieces().getFirst().getBoundingBox();
            return Optional.of(surface(level, first.getCenter().getX(), first.maxZ() + GAP));
        });
    }

    /** The first free block above the ground at (x, z), generating the chunk if needed so its height is real. */
    private static BlockPos surface(ServerLevel level, int x, int z) {
        level.getChunk(x >> 4, z >> 4);
        return new BlockPos(x, level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z), z);
    }
}
