package com.orijua.overgearedmod.structure;

import com.orijua.overgearedmod.Overgearedmod;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.features.MiscOverworldFeatures;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerChunkCache;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;

import java.util.Optional;

/**
 * Bairan as the world's starting village. When a new world is created, it looks for the nearest place around the
 * usual spawn where the whole village fits on dry land in one of its biomes, saves it, and makes that the spawn: the
 * player starts on the square by the well, as Grid starts in Bairan. Bairan then generates there with the terrain,
 * like any village ({@link WorldSitePlacement}). The Northern End Cave's site is chosen at the same time, far to the north
 * ({@link NorthEndCave}); worlds that already had Bairan get theirs the next time they load.
 * <p>
 * Worlds created before this have no site, so no Bairan. So do worlds whose generator doesn't use the structure (a
 * superflat preset, for example) and the rare world with no fitting land nearby; those keep the vanilla spawn.
 */
public final class StartingVillage {
    /** How far around the usual spawn to look: 32 chunks is 512 blocks each way. */
    static final int SEARCH_RADIUS = 32;
    private static final ResourceKey<Structure> VILLAGE = ResourceKey.create(Registries.STRUCTURE, Bairan.STRUCTURE);

    private StartingVillage() {
    }

    /** Where the village is and where the player starts. */
    public record Site(ChunkPos chunk, BlockPos spawn) {
    }

    public static void register(IEventBus gameBus) {
        gameBus.addListener(StartingVillage::onLevelLoad);
        gameBus.addListener(StartingVillage::onCreateSpawn);
        gameBus.addListener(StartingVillage::onServerStopped);
    }

    /**
     * An existing world: its saved sites have to be known before its chunks generate. A world with Bairan but from
     * before the cave gets the cave's site now (far to the north, where nothing has generated yet).
     */
    private static void onLevelLoad(LevelEvent.Load event) {
        if (event.getLevel() instanceof ServerLevel level && level.dimension() == Level.OVERWORLD) {
            WorldSitesData data = WorldSitesData.get(level);
            data.sites().forEach((name, site) -> WorldSites.remember(level.getSeed(), name, site));
            data.site(WorldSites.BAIRAN).filter(bairan -> data.site(WorldSites.NORTH_END_CAVE).isEmpty())
                    .ifPresent(bairan -> chooseCave(level, bairan));
        }
    }

    private static void chooseCave(ServerLevel level, ChunkPos bairan) {
        ServerChunkCache chunks = level.getChunkSource();
        NorthEndCave.findSite(level.registryAccess(), chunks.getGenerator(), chunks.randomState(), level.getStructureManager(),
                level.getSeed(), level, bairan).ifPresent(cave -> {
            WorldSites.remember(level.getSeed(), WorldSites.NORTH_END_CAVE, cave);
            WorldSitesData.get(level).setSite(WorldSites.NORTH_END_CAVE, cave);
            Overgearedmod.LOGGER.info("Northern End Cave at chunk {} ({} blocks north of Bairan)", cave, (bairan.z - cave.z) * 16);
        });
    }

    /** A new world: choose the site and spawn there instead of the vanilla spawn. */
    private static void onCreateSpawn(LevelEvent.CreateSpawnPosition event) {
        if (!(event.getLevel() instanceof ServerLevel level) || level.dimension() != Level.OVERWORLD) {
            return;
        }
        ServerChunkCache chunks = level.getChunkSource();
        Optional<Holder.Reference<Structure>> village = level.registryAccess().registryOrThrow(Registries.STRUCTURE).getHolder(VILLAGE);
        if (village.isEmpty() || chunks.getGeneratorState().getPlacementsForStructure(village.get()).isEmpty()) {
            return; // this world's generator never places it
        }
        ChunkPos usualSpawn = new ChunkPos(chunks.randomState().sampler().findSpawnPosition());
        Optional<Site> site = findSite(level.registryAccess(), chunks.getGenerator(), chunks.randomState(), level.getStructureManager(),
                level.getSeed(), level, usualSpawn);
        if (site.isEmpty()) {
            Overgearedmod.LOGGER.warn("No room for Bairan within {} chunks of spawn; this world has no starting village", SEARCH_RADIUS);
            return;
        }
        WorldSites.remember(level.getSeed(), WorldSites.BAIRAN, site.get().chunk());
        WorldSitesData.get(level).setSite(WorldSites.BAIRAN, site.get().chunk());
        chooseCave(level, site.get().chunk());
        event.getSettings().setSpawn(site.get().spawn(), 0.0f);
        // Start by the well, not anywhere within 10 blocks of it (a roof, the mine).
        level.getGameRules().getRule(GameRules.RULE_SPAWN_RADIUS).set(0, level.getServer());
        // Cancelling skips the vanilla spawn search, and with it the bonus chest: place it the same way.
        if (level.getServer().getWorldData().worldGenOptions().generateBonusChest()) {
            level.registryAccess().registry(Registries.CONFIGURED_FEATURE)
                    .flatMap(features -> features.getHolder(MiscOverworldFeatures.BONUS_CHEST))
                    .ifPresent(chest -> chest.value().place(level, chunks.getGenerator(), level.random, site.get().spawn()));
        }
        event.setCanceled(true);
    }

    private static void onServerStopped(ServerStoppedEvent event) {
        WorldSites.clear();
    }

    /**
     * The chunk nearest {@code near} where the village assembles (its biome check included) with every piece on dry
     * land, and the spawn on its square. Only reads the terrain noise: nothing is generated or placed.
     */
    public static Optional<Site> findSite(RegistryAccess registries, ChunkGenerator generator, RandomState random,
                                          StructureTemplateManager templates, long seed, LevelHeightAccessor height, ChunkPos near) {
        Structure village = registries.registryOrThrow(Registries.STRUCTURE).get(VILLAGE);
        if (village == null) {
            return Optional.empty();
        }
        for (ChunkPos chunk : ChunkSpiral.around(near, SEARCH_RADIUS)) {
            StructureStart start = village.generate(registries, generator, generator.getBiomeSource(), random, templates, seed, chunk, 0,
                    height, village.biomes()::contains);
            if (start.isValid() && start.getPieces().stream().allMatch(piece -> onDryLand(piece, generator, random, height))) {
                return Optional.of(new Site(chunk, spawnOnSquare(start.getPieces().getFirst().getBoundingBox())));
            }
        }
        return Optional.empty();
    }

    /** Whether the ground under the middle of a piece is land, not the bottom of a lake, river or sea. */
    private static boolean onDryLand(StructurePiece piece, ChunkGenerator generator, RandomState random, LevelHeightAccessor height) {
        BlockPos middle = piece.getBoundingBox().getCenter();
        int surface = generator.getBaseHeight(middle.getX(), middle.getZ(), Heightmap.Types.WORLD_SURFACE_WG, height, random);
        int ground = generator.getBaseHeight(middle.getX(), middle.getZ(), Heightmap.Types.OCEAN_FLOOR_WG, height, random);
        return surface == ground;
    }

    /** Three blocks east of the well in the middle of the square: open floor however the square is turned. */
    private static BlockPos spawnOnSquare(BoundingBox square) {
        BlockPos middle = square.getCenter();
        return new BlockPos(middle.getX() + 3, square.minY() + 1, middle.getZ());
    }
}
