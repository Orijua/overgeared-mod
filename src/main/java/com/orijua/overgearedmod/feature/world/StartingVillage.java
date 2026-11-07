package com.orijua.overgearedmod.feature.world;

import com.orijua.overgearedmod.Overgearedmod;
import net.minecraft.core.BlockPos;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerChunkCache;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * Bairan, the starting village of the Satisfy dimension. The dimension's (0, 0) is Bairan's pixel on the canon map
 * ({@link SatisfyMapData}), in its plains. When the dimension first loads in a new world, it looks for the nearest
 * place around (0, 0) where the whole village fits on dry land in the map's plains, and saves it with the
 * landing spot on its square, by the well, where a player entering Satisfy starts, as Grid starts in Bairan. Bairan
 * then generates there with the terrain, like any village ({@link WorldSitePlacement}). The other places are chosen at
 * the same time, from Bairan and the map: the Northern End Cave on the snowy north coast ({@link NorthEndCave}), Earl
 * Ashur's manor near Bairan ({@link AshurManor}) and Winston at its place on the map, west-northwest of Bairan
 * ({@link Winston}).
 * <p>
 * The search runs only once per world (it is remembered even if it finds nothing). Worlds created before the dimension
 * have no site, so no Bairan. So do worlds whose generator doesn't use the structure and the rare world with no fitting
 * land near Bairan's place on the map.
 */
public final class StartingVillage {
    /**
     * How far around Bairan's place on the map, (0, 0), to look, in chunks: the terrain follows the map, so Bairan's
     * plains are there and the village lands at (0, 0) or a chunk or two away.
     */
    public static final int SEARCH_RADIUS = 4;
    private static final ResourceKey<Structure> VILLAGE = ResourceKey.create(Registries.STRUCTURE, Bairan.STRUCTURE);

    private StartingVillage() {
    }

    /** Where the village is and where the player lands. */
    public record Site(ChunkPos chunk, BlockPos spawn) {
    }

    public static void register(IEventBus gameBus) {
        gameBus.addListener(StartingVillage::onLevelLoad);
        gameBus.addListener(StartingVillage::onServerStopped);
    }

    /**
     * The real world's dimensions place no site. The Satisfy dimension: its saved sites have to be known before its
     * chunks generate, and a new world gets them now.
     */
    private static void onLevelLoad(LevelEvent.Load event) {
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }
        if (!Satisfy.is(level)) {
            WorldSites.exclude(level.getChunkSource().randomState());
            return;
        }
        WorldSitesData data = WorldSitesData.get(level.getServer().overworld());
        long seed = level.getSeed();
        if (!data.chosen()) {
            ServerChunkCache chunks = level.getChunkSource();
            Chosen chosen = choose(level.registryAccess(), chunks.getGenerator(), chunks.randomState(), level.getStructureManager(), seed, level);
            chosen.sites().forEach(data::setSite);
            chosen.landing().ifPresent(data::setLanding);
            chosen.sites().forEach((name, site) -> Overgearedmod.LOGGER.info("Satisfy site {} at chunk {}", name, site));
            data.markChosen();
        }
        data.sites().forEach((name, site) -> WorldSites.remember(seed, name, site));
        data.landing().ifPresent(landing -> WorldSites.rememberLanding(seed, landing));
    }

    /** The sites a new Satisfy world gets (none if Bairan has no room), and where a player lands in Bairan. */
    public record Chosen(Map<String, ChunkPos> sites, Optional<BlockPos> landing) {
    }

    /**
     * Chooses every site of a new world, Bairan first and the rest from it and the map: the cave on the snowy north
     * coast, the manor near Bairan and Winston at its place on the map, away from the other two. Only reads the terrain
     * noise and the map: nothing is placed.
     */
    public static Chosen choose(RegistryAccess registries, ChunkGenerator generator, RandomState random, StructureTemplateManager templates,
                                long seed, LevelHeightAccessor height) {
        Optional<Site> bairan = findSite(registries, generator, random, templates, seed, height, new ChunkPos(0, 0));
        if (bairan.isEmpty()) {
            Overgearedmod.LOGGER.warn("No room for Bairan within {} chunks of its place on the map; this world has no starting village", SEARCH_RADIUS);
            return new Chosen(Map.of(), Optional.empty());
        }
        ChunkPos village = bairan.get().chunk();
        Map<String, ChunkPos> sites = new LinkedHashMap<>();
        sites.put(WorldSites.BAIRAN, village);
        NorthEndCave.findSite(registries, generator, random, templates, seed, height, village)
                .ifPresent(cave -> sites.put(WorldSites.NORTH_END_CAVE, cave));
        AshurManor.findSite(registries, generator, random, templates, seed, height, village, AshurManor.MIN_DISTANCE, AshurManor.MAX_DISTANCE)
                .ifPresent(manor -> sites.put(WorldSites.ASHUR_MANOR, manor));
        // Winston keeps away from the cave and the manor.
        List<ChunkPos> taken = Stream.of(WorldSites.NORTH_END_CAVE, WorldSites.ASHUR_MANOR).map(sites::get).filter(Objects::nonNull).toList();
        Winston.findSite(registries, generator, random, templates, seed, height, village, taken)
                .ifPresent(winston -> sites.put(WorldSites.WINSTON, winston));
        return new Chosen(sites, Optional.of(bairan.get().spawn()));
    }

    private static void onServerStopped(ServerStoppedEvent event) {
        WorldSites.clear();
    }

    /**
     * The chunk nearest {@code near}, up to {@link #SEARCH_RADIUS} chunks away, where the village assembles (its biome
     * check included) in the map's plains (in Satisfy, as on the canon map) with every piece on dry land, and the spawn
     * on its square. Only reads the terrain noise: nothing is generated or placed.
     */
    public static Optional<Site> findSite(RegistryAccess registries, ChunkGenerator generator, RandomState random,
                                          StructureTemplateManager templates, long seed, LevelHeightAccessor height, ChunkPos near) {
        return findSite(registries, generator, random, templates, seed, height, near, SEARCH_RADIUS);
    }

    /**
     * The same search up to {@code radius} chunks away: wider on terrain that doesn't follow the map, such as the
     * vanilla overworld of the tests.
     */
    public static Optional<Site> findSite(RegistryAccess registries, ChunkGenerator generator, RandomState random,
                                          StructureTemplateManager templates, long seed, LevelHeightAccessor height, ChunkPos near,
                                          int radius) {
        Structure village = registries.registryOrThrow(Registries.STRUCTURE).get(VILLAGE);
        if (village == null) {
            return Optional.empty();
        }
        for (ChunkPos chunk : ChunkSpiral.around(near, radius)) {
            if (!SatisfyBiomeSource.inMapRegion(generator.getBiomeSource(), chunk.getMiddleBlockX(), chunk.getMiddleBlockZ(), SatisfyMapData.PLAINS)) {
                continue;
            }
            StructureStart start = village.generate(registries, generator, generator.getBiomeSource(), random, templates, seed, chunk, 0,
                    height, village.biomes()::contains);
            if (start.isValid() && start.getPieces().stream().allMatch(piece -> SiteTerrain.onDryLand(piece, generator, random, height))) {
                return Optional.of(new Site(chunk, spawnOnSquare(start.getPieces().getFirst().getBoundingBox())));
            }
        }
        return Optional.empty();
    }

    /** Three blocks east of the well in the middle of the square: open floor however the square is turned. */
    private static BlockPos spawnOnSquare(BoundingBox square) {
        BlockPos middle = square.getCenter();
        return new BlockPos(middle.getX() + 3, square.minY() + 1, middle.getZ());
    }
}
