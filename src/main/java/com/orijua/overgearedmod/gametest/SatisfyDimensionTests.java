package com.orijua.overgearedmod.gametest;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import com.orijua.overgearedmod.Overgearedmod;
import com.orijua.overgearedmod.feature.world.AshurManor;
import com.orijua.overgearedmod.feature.world.NorthEndCave;
import com.orijua.overgearedmod.feature.world.SatisfyBiomeSource;
import com.orijua.overgearedmod.feature.world.SatisfyMap;
import com.orijua.overgearedmod.feature.world.StartingVillage;
import com.orijua.overgearedmod.feature.world.Winston;
import com.orijua.overgearedmod.feature.world.WorldSitePlacement;
import com.orijua.overgearedmod.feature.world.WorldSites;
import net.minecraft.core.BlockPos;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.biome.FixedBiomeSource;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.structure.StructureSet;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.io.IOException;
import java.io.Reader;
import java.util.List;
import java.util.Map;

/**
 * The Satisfy dimension: its lore map, the sites a new world chooses in it, and the real world having none. The game-test
 * server drops the datapack's dimensions, so these decode the shipped dimension JSON and work with its generator.
 */
@GameTestHolder(Overgearedmod.MODID)
@PrefixGameTestTemplate(false)
public final class SatisfyDimensionTests {
    private static final LevelHeightAccessor HEIGHT = LevelHeightAccessor.create(-64, 384);

    private SatisfyDimensionTests() {
    }

    /** The generator that data/overgearedmod/dimension/satisfy.json describes. */
    private static ChunkGenerator generator(GameTestHelper helper) {
        var server = helper.getLevel().getServer();
        try (Reader reader = server.getResourceManager().openAsReader(ResourceLocation.fromNamespaceAndPath(Overgearedmod.MODID, "dimension/satisfy.json"))) {
            JsonElement json = JsonParser.parseReader(reader);
            RegistryOps<JsonElement> ops = RegistryOps.create(JsonOps.INSTANCE, server.registryAccess());
            return LevelStem.CODEC.parse(ops, json).getOrThrow(IllegalStateException::new).generator();
        } catch (IOException e) {
            throw new AssertionError("the dimension JSON is missing", e);
        }
    }

    private static RandomState random(RegistryAccess registries, long seed) {
        var settings = registries.registryOrThrow(Registries.NOISE_SETTINGS).getHolderOrThrow(NoiseGeneratorSettings.OVERWORLD);
        return RandomState.create(settings.value(), registries.lookupOrThrow(Registries.NOISE), seed);
    }

    @GameTest(template = TestSupport.EMPTY)
    public static void dimensionUsesTheLoreMap(GameTestHelper helper) {
        ChunkGenerator generator = generator(helper);
        helper.assertTrue(generator.getBiomeSource() instanceof SatisfyBiomeSource, "the lore biome source");
        var sampler = random(helper.getLevel().registryAccess(), 1L).sampler();
        var biomes = generator.getBiomeSource();
        // The middle of a cell is out of the warp's reach: (0, 0) is Bairan's plains, five cells north is snowy taiga.
        helper.assertTrue(biomes.getNoiseBiome(0, 16, 0, sampler).is(Biomes.PLAINS), "Bairan stands on plains");
        helper.assertTrue(biomes.getNoiseBiome(0, 16, -960 >> 2, sampler).is(Biomes.SNOWY_TAIGA), "the far north is snowy");
        helper.assertTrue(biomes.getNoiseBiome(-2112 >> 2, 16, 0, sampler).is(Biomes.FOREST), "the Elf Forest is to the west");
        helper.succeed();
    }

    /** Every world gets all four sites, in the right places relative to each other, and a landing on Bairan's plaza. */
    @GameTest(template = TestSupport.EMPTY, timeoutTicks = 1200)
    public static void everyWorldChoosesItsSitesInSatisfy(GameTestHelper helper) {
        var registries = helper.getLevel().registryAccess();
        ChunkGenerator generator = generator(helper);
        SatisfyMap map = ((SatisfyBiomeSource) generator.getBiomeSource()).map();
        for (long seed = 1; seed <= 12; seed++) {
            RandomState random = random(registries, seed);
            StartingVillage.Chosen chosen = StartingVillage.choose(registries, generator, random,
                    helper.getLevel().getStructureManager(), seed, HEIGHT);
            Map<String, ChunkPos> sites = chosen.sites();
            for (String site : List.of(WorldSites.BAIRAN, WorldSites.NORTH_END_CAVE, WorldSites.ASHUR_MANOR, WorldSites.WINSTON)) {
                helper.assertTrue(sites.containsKey(site), "seed " + seed + " has no " + site + ": " + sites.keySet());
            }
            ChunkPos bairan = sites.get(WorldSites.BAIRAN);
            helper.assertTrue(map.at(bairan.getMiddleBlockX(), bairan.getMiddleBlockZ()) == 'P', "Bairan is in the plains region of the lore map, seed " + seed + ": " + bairan);
            helper.assertTrue(chosen.landing().isPresent(), "a landing for seed " + seed);
            BlockPos landing = chosen.landing().get();
            helper.assertTrue(bairan.getChessboardDistance(new ChunkPos(landing)) <= 2, "the landing is on Bairan's plaza, seed " + seed + ": " + landing + " vs " + bairan);
            ChunkPos cave = sites.get(WorldSites.NORTH_END_CAVE);
            ChunkPos manor = sites.get(WorldSites.ASHUR_MANOR);
            ChunkPos winston = sites.get(WorldSites.WINSTON);
            helper.assertTrue(cave.z <= bairan.z - NorthEndCave.MIN_DISTANCE, "the cave is far north of Bairan");
            int toManor = bairan.getChessboardDistance(manor);
            helper.assertTrue(toManor >= AshurManor.MIN_DISTANCE && toManor <= AshurManor.MAX_DISTANCE, "the manor is " + toManor + " chunks from Bairan, seed " + seed);
            int toWinston = bairan.getChessboardDistance(winston);
            helper.assertTrue(toWinston >= Winston.MIN_DISTANCE && toWinston <= Winston.MAX_DISTANCE, "Winston is " + toWinston + " chunks from Bairan, seed " + seed);
            helper.assertTrue(winston.getChessboardDistance(cave) >= 8, "Winston away from the cave, seed " + seed);
            helper.assertTrue(winston.getChessboardDistance(manor) >= 8, "Winston away from the manor, seed " + seed);
        }
        helper.succeed();
    }

    @GameTest(template = TestSupport.EMPTY)
    public static void theRealWorldPlacesNoSites(GameTestHelper helper) {
        var registries = helper.getLevel().registryAccess();
        ChunkGenerator generator = generator(helper);
        long seed = -5_551_234_567L; // no real world in the test server has it
        RandomState satisfy = random(registries, seed);
        RandomState realWorld = random(registries, seed);
        WorldSites.exclude(realWorld);
        ChunkPos site = new ChunkPos(7, -9);
        for (String name : List.of(WorldSites.BAIRAN, WorldSites.NORTH_END_CAVE, WorldSites.ASHUR_MANOR, WorldSites.WINSTON)) {
            StructureSet set = registries.registryOrThrow(Registries.STRUCTURE_SET).get(Overgearedmod.id(name));
            helper.assertTrue(set != null && set.placement() instanceof WorldSitePlacement, name + " uses the world-site placement");
            WorldSitePlacement placement = (WorldSitePlacement) set.placement();
            WorldSites.remember(seed, name, site);
            try {
                var lookup = registries.lookupOrThrow(Registries.STRUCTURE_SET);
                helper.assertTrue(placement.isStructureChunk(generator.createState(lookup, satisfy, seed), site.x, site.z),
                        name + "'s chunk is a structure chunk in Satisfy");
                helper.assertFalse(placement.isStructureChunk(generator.createState(lookup, realWorld, seed), site.x, site.z),
                        "the same chunk of the real world is not (" + name + ")");
            } finally {
                WorldSites.forget(seed, name);
            }
        }
        helper.succeed();
    }

    /** The real overworld of the test server went through StartingVillage's load event: none of the four sites is placed in it. */
    @GameTest(template = TestSupport.EMPTY)
    public static void theOverworldIsExcludedOnLoad(GameTestHelper helper) {
        var registries = helper.getLevel().registryAccess();
        var level = helper.getLevel();
        long seed = level.getSeed();
        var state = level.getChunkSource().getGeneratorState();
        var sets = registries.registryOrThrow(Registries.STRUCTURE_SET);
        ChunkPos site = new ChunkPos(7, -9);
        for (String name : List.of(WorldSites.BAIRAN, WorldSites.NORTH_END_CAVE, WorldSites.ASHUR_MANOR, WorldSites.WINSTON)) {
            StructureSet set = sets.get(Overgearedmod.id(name));
            helper.assertTrue(set != null && set.placement() instanceof WorldSitePlacement, name + " uses the world-site placement");
            WorldSitePlacement placement = (WorldSitePlacement) set.placement();
            var previous = WorldSites.siteFor(seed, name);
            WorldSites.remember(seed, name, site);
            try {
                helper.assertFalse(placement.isStructureChunk(state, site.x, site.z), name + " must not be placed in the loaded overworld");
            } finally {
                previous.ifPresentOrElse(chunk -> WorldSites.remember(seed, name, chunk), () -> WorldSites.forget(seed, name));
            }
        }
        helper.succeed();
    }

    /**
     * Technical test of the map-driven terrain: the noise settings {@code overgearedmod:satisfy} (vanilla's, with the
     * terrain splines fed by the hand-drawn test island) give a sea floor far out, and land and a mountain inside.
     */
    @GameTest(template = TestSupport.EMPTY, timeoutTicks = 600)
    public static void mapDrivenTerrainHasSeaLandAndMountain(GameTestHelper helper) {
        var registries = helper.getLevel().registryAccess();
        var settings = registries.registryOrThrow(Registries.NOISE_SETTINGS)
                .getHolderOrThrow(net.minecraft.resources.ResourceKey.create(Registries.NOISE_SETTINGS, Overgearedmod.id("satisfy")));
        var plains = registries.registryOrThrow(Registries.BIOME).getHolderOrThrow(Biomes.PLAINS);
        NoiseBasedChunkGenerator generator = new NoiseBasedChunkGenerator(new FixedBiomeSource(plains), settings);
        RandomState random = RandomState.create(settings.value(), registries.lookupOrThrow(Registries.NOISE), 7L);
        int sea = generator.getSeaLevel();
        int farSea = generator.getBaseHeight(6 * 96 + 960, 0, Heightmap.Types.OCEAN_FLOOR_WG, HEIGHT, random);
        int shore = generator.getBaseHeight(5 * 96, 0, Heightmap.Types.OCEAN_FLOOR_WG, HEIGHT, random);
        int lowland = generator.getBaseHeight(4 * 96, 0, Heightmap.Types.OCEAN_FLOOR_WG, HEIGHT, random);
        int peak = generator.getBaseHeight(0, 0, Heightmap.Types.OCEAN_FLOOR_WG, HEIGHT, random);
        helper.assertTrue(farSea < sea - 15, "far out is a deep sea: floor " + farSea + " vs sea level " + sea);
        helper.assertTrue(lowland > sea, "the lowland is dry land: " + lowland + " vs " + sea);
        helper.assertTrue(peak > lowland + 20, "the middle is a mountain: " + peak + " vs lowland " + lowland);
        helper.assertTrue(shore <= lowland, "the coast is not higher than the lowland: " + shore + " vs " + lowland);
        helper.succeed();
    }
}
