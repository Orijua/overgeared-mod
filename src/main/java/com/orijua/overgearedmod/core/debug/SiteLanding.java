package com.orijua.overgearedmod.core.debug;

import com.orijua.overgearedmod.feature.world.AshurManor;
import com.orijua.overgearedmod.feature.world.NorthEndCave;
import com.orijua.overgearedmod.feature.world.SatisfyMapData;
import com.orijua.overgearedmod.feature.world.SatisfyPlaces;
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
import java.util.stream.Stream;

/**
 * Where {@code /satisfy debug goto} drops the player: on the surface, in the open, in front of the place. /locate only
 * gives a structure's chunk and no height, so teleporting there tends to bury the player in a mountain.
 * <p>
 * Two kinds of places: the world's structures (Bairan, the cave, the manor, Winston), chosen when the world was made,
 * and the canon places of the map ({@link SatisfyPlaces}), which are only coordinates: the player lands on dry ground
 * near them, or nowhere if they are in the sea. Bairan and Winston are both; their structure wins.
 */
public final class SiteLanding {
    /** The structures that can be visited, by the name typed in the command. */
    public static final List<String> STRUCTURE_SITES = List.of(WorldSites.BAIRAN, WorldSites.NORTH_END_CAVE, WorldSites.ASHUR_MANOR,
            WorldSites.WINSTON);
    /** Everything that can be visited: the structures, then the map's places. */
    public static final List<String> SITES = Stream.concat(STRUCTURE_SITES.stream(), SatisfyPlaces.west().ids().stream())
            .distinct().toList();
    /** How far from the middle of a land cell the landing may move to find dry ground (a cell is 32 blocks). */
    private static final int DRY_SEARCH = 12;
    private static final int DRY_STEP = 6;
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
     * (an old world, or one where no site was found, and no map place by that name either) or it is in the sea. Bairan
     * lands on its plaza; the other structures land just south of their first piece, facing it; map places on dry
     * ground next to them ({@link #findPlace}).
     */
    public static Optional<BlockPos> find(ServerLevel level, String site) {
        return findStructure(level, site).or(() -> findPlace(level, site));
    }

    /** Whether {@code site} is one of the map's places (not only a structure). */
    public static boolean isMapPlace(String site) {
        return SatisfyPlaces.west().get(site).isPresent();
    }

    /**
     * Dry ground near a map place: the middle of the nearest land cell ({@link SatisfyPlaces#landing}), or the closest
     * spot around it whose ground is not water. Empty for an unknown place, a place in the sea, or one whose land the
     * terrain left under water.
     */
    public static Optional<BlockPos> findPlace(ServerLevel level, String site) {
        SatisfyMapData map = SatisfyMapData.west();
        return SatisfyPlaces.west().get(site).flatMap(place -> SatisfyPlaces.landing(map, place)).flatMap(xz -> {
            for (int ring = 0; ring <= DRY_SEARCH; ring += DRY_STEP) {
                for (int dz = -ring; dz <= ring; dz += DRY_STEP) {
                    for (int dx = -ring; dx <= ring; dx += DRY_STEP) {
                        if (Math.max(Math.abs(dx), Math.abs(dz)) != ring) {
                            continue;
                        }
                        BlockPos landing = surface(level, xz[0] + dx, xz[1] + dz);
                        if (level.getFluidState(landing.below()).isEmpty() && level.getFluidState(landing).isEmpty()) {
                            return Optional.of(landing);
                        }
                    }
                }
            }
            return Optional.empty();
        });
    }

    private static Optional<BlockPos> findStructure(ServerLevel level, String site) {
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
