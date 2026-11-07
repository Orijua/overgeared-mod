package com.orijua.overgearedmod.core.debug;

import com.orijua.overgearedmod.Overgearedmod;
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
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.structure.PoolElementStructurePiece;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
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

    /** Reads the map's places the first time they are asked for, not when the class loads. */
    private static final class Names {
        static final List<String> ALL = names();

        private static List<String> names() {
            try {
                return Stream.concat(STRUCTURE_SITES.stream(), SatisfyPlaces.west().ids().stream()).distinct().toList();
            } catch (RuntimeException e) {
                Overgearedmod.LOGGER.error("Cannot read the map's places: only the structures can be visited", e);
                return STRUCTURE_SITES;
            }
        }
    }

    /** Everything that can be visited: the structures, then the map's places. */
    public static List<String> sites() {
        return Names.ALL;
    }
    /** How far from the middle of a land cell the landing may move to find dry ground (a cell is 32 blocks). */
    public static final int DRY_SEARCH = 12;
    private static final int DRY_STEP = 6;
    private static final Map<String, ResourceKey<Structure>> STRUCTURES = Map.of(
            WorldSites.NORTH_END_CAVE, NorthEndCave.KEY,
            WorldSites.ASHUR_MANOR, AshurManor.KEY,
            WorldSites.WINSTON, Winston.KEY);
    /** How far in front of the first piece (the cave mouth, the manor, Winston's square) the player lands. */
    static final int GAP = 3;

    private SiteLanding() {
    }

    /** Why a landing was found or not. */
    public enum Reason {
        OK, UNKNOWN, NO_SITE, SEA
    }

    /** The outcome of looking for a landing: where to stand and the yaw that faces the place, or why there is none. */
    public record Landing(Optional<BlockPos> pos, float yaw, Reason reason) {
        static Landing at(BlockPos pos, float yaw) {
            return new Landing(Optional.of(pos), yaw, Reason.OK);
        }

        static Landing none(Reason reason) {
            return new Landing(Optional.empty(), 180.0f, reason);
        }
    }

    /**
     * The block to stand on near {@code site} in the Satisfy {@code level}, or empty if this world has no such place
     * (see {@link #locate} for why).
     */
    public static Optional<BlockPos> find(ServerLevel level, String site) {
        return locate(level, site).pos();
    }

    /**
     * Where to land for {@code site}, with the reason when there is nowhere: {@code UNKNOWN} (not a name we know),
     * {@code NO_SITE} (a structure this world never placed, or whose start is gone; a map place of the same name is the
     * fallback for Bairan and Winston) or {@code SEA} (a map place that is in the sea or whose land the terrain left
     * under water). Bairan lands on its plaza; the other structures in front of their first piece, facing it; map places
     * on dry ground next to them ({@link #findPlace}).
     */
    public static Landing locate(ServerLevel level, String site) {
        if (!sites().contains(site)) {
            return Landing.none(Reason.UNKNOWN);
        }
        if (STRUCTURE_SITES.contains(site)) {
            if (!hasSite(level, site)) {
                return Landing.none(Reason.NO_SITE);
            }
            Landing structure = findStructure(level, site);
            if (structure.reason() == Reason.OK) {
                return structure;
            }
            return findPlace(level, site).map(pos -> Landing.at(pos, 180.0f)).orElse(Landing.none(Reason.NO_SITE));
        }
        return findPlace(level, site).map(pos -> Landing.at(pos, 180.0f)).orElse(Landing.none(Reason.SEA));
    }

    private static boolean hasSite(ServerLevel level, String site) {
        return site.equals(WorldSites.BAIRAN) ? WorldSites.landingFor(level.getSeed()).isPresent()
                : WorldSites.siteFor(level.getSeed(), site).isPresent();
    }

    /** Whether {@code site} is one of the map's places (not only a structure). */
    public static boolean isMapPlace(String site) {
        return Names.ALL.contains(site) && SatisfyPlaces.west().get(site).isPresent();
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

    private static Landing findStructure(ServerLevel level, String site) {
        if (site.equals(WorldSites.BAIRAN)) {
            return WorldSites.landingFor(level.getSeed()).map(landing -> Landing.at(surface(level, landing.getX(), landing.getZ()), 180.0f))
                    .orElse(Landing.none(Reason.NO_SITE));
        }
        ResourceKey<Structure> key = STRUCTURES.get(site);
        if (key == null) {
            return Landing.none(Reason.NO_SITE);
        }
        Structure structure = level.registryAccess().registryOrThrow(Registries.STRUCTURE).get(key);
        if (structure == null) {
            return Landing.none(Reason.NO_SITE);
        }
        return WorldSites.siteFor(level.getSeed(), site).map(chunk -> {
            StructureStart start = level.getChunk(chunk.x, chunk.z, ChunkStatus.STRUCTURE_STARTS).getStartForStructure(structure);
            if (start == null || !start.isValid()) {
                return Landing.none(Reason.NO_SITE);
            }
            StructurePiece first = start.getPieces().getFirst();
            Rotation rotation = first instanceof PoolElementStructurePiece pool ? pool.getRotation() : Rotation.NONE;
            BlockPos spot = SiteFront.spot(first.getBoundingBox(), rotation, GAP);
            return Landing.at(surface(level, spot.getX(), spot.getZ()), SiteFront.yawToPiece(rotation));
        }).orElse(Landing.none(Reason.NO_SITE));
    }

    /** The first free block above the ground at (x, z), generating the chunk if needed so its height is real. */
    private static BlockPos surface(ServerLevel level, int x, int z) {
        level.getChunk(x >> 4, z >> 4);
        return new BlockPos(x, level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z), z);
    }
}
