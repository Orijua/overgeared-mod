package com.orijua.overgearedmod.gametest;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import com.orijua.overgearedmod.Overgearedmod;
import com.orijua.overgearedmod.feature.world.AshurManor;
import com.orijua.overgearedmod.feature.world.NorthEndCave;
import com.orijua.overgearedmod.feature.world.SatisfyBiomeSource;
import com.orijua.overgearedmod.feature.world.SatisfyMap;
import com.orijua.overgearedmod.feature.world.SatisfyMapData;
import com.orijua.overgearedmod.feature.world.StartingVillage;
import com.orijua.overgearedmod.feature.world.Winston;
import com.orijua.overgearedmod.feature.world.WorldSitePlacement;
import com.orijua.overgearedmod.feature.world.WorldSites;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.QuartPos;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.biome.Climate;
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

    /** The biome at the middle of a source pixel of the canon map (block (0, 0) is Bairan's pixel). */
    private static Holder<Biome> biomeAtPixel(BiomeSource biomes, SatisfyMapData map, int pixelX, int pixelZ, Climate.Sampler sampler) {
        int x = map.blockXOfPixel(pixelX);
        int z = map.blockZOfPixel(pixelZ);
        return biomes.getNoiseBiome(QuartPos.fromBlock(x), 16, QuartPos.fromBlock(z), sampler);
    }

    @GameTest(template = TestSupport.EMPTY)
    public static void dimensionUsesTheLoreMap(GameTestHelper helper) {
        ChunkGenerator generator = generator(helper);
        helper.assertTrue(generator.getBiomeSource() instanceof SatisfyBiomeSource, "the lore biome source");
        SatisfyBiomeSource biomes = (SatisfyBiomeSource) generator.getBiomeSource();
        SatisfyMapData map = biomes.map().data();
        var sampler = random(helper.getLevel().registryAccess(), 1L).sampler();
        // Places in the middle of a 3x3 block of cells of one region, out of the warp's reach.
        helper.assertTrue(biomes.getNoiseBiome(0, 16, 0, sampler).is(Biomes.PLAINS), "Bairan stands on plains");
        helper.assertTrue(biomeAtPixel(biomes, map, 911, 754, sampler).is(Biomes.DESERT), "the Lilton Desert");
        helper.assertTrue(biomeAtPixel(biomes, map, 1752, 344, sampler).is(Biomes.SNOWY_TAIGA), "the snowy north of Bairan");
        helper.assertTrue(biomeAtPixel(biomes, map, 1880, 456, sampler).is(Biomes.SNOWY_PLAINS), "the snowy plains east of Bairan");
        helper.assertTrue(biomeAtPixel(biomes, map, 1688, 904, sampler).is(Biomes.BADLANDS), "the orange mesas are badlands");
        helper.assertTrue(biomeAtPixel(biomes, map, 408, 744, sampler).is(Biomes.STONY_PEAKS), "the volcano in the west");
        // The sea (here the Arctic Glacier's and far off the map) is vanilla's, from the outside source.
        for (int[] pixel : new int[][]{{1382, 178}, {-5000, 400}, {9000, 9000}}) {
            int qx = QuartPos.fromBlock(map.blockXOfPixel(pixel[0]));
            int qz = QuartPos.fromBlock(map.blockZOfPixel(pixel[1]));
            helper.assertTrue(biomes.getNoiseBiome(qx, 16, qz, sampler).equals(biomes.outside().getNoiseBiome(qx, 16, qz, sampler)),
                    "the sea at pixel " + pixel[0] + "," + pixel[1] + " comes from the outside source");
        }
        // Everywhere: the region the map gives, or the outside source in its water.
        for (int x = -3000; x <= 3000; x += 97) {
            for (int z = -1000; z <= 3000; z += 89) {
                int region = biomes.map().regionAt((QuartPos.fromBlock(x) << 2) + 2, (QuartPos.fromBlock(z) << 2) + 2);
                Holder<Biome> biome = biomes.getNoiseBiome(QuartPos.fromBlock(x), 16, QuartPos.fromBlock(z), sampler);
                String expected = region == SatisfyMapData.SEA ? null : map.legend(region);
                helper.assertTrue(expected == null ? biome.equals(biomes.outside().getNoiseBiome(QuartPos.fromBlock(x), 16, QuartPos.fromBlock(z), sampler))
                        : biome.is(ResourceLocation.parse(expected)), "the biome at " + x + "," + z + " follows region " + region);
            }
        }
        helper.succeed();
    }

    /** Every world gets all four sites, in the right places of the map, and a landing on Bairan's plaza. */
    @GameTest(template = TestSupport.EMPTY, timeoutTicks = 1200)
    public static void everyWorldChoosesItsSitesInSatisfy(GameTestHelper helper) {
        var registries = helper.getLevel().registryAccess();
        ChunkGenerator generator = generator(helper);
        SatisfyMap map = ((SatisfyBiomeSource) generator.getBiomeSource()).map();
        ChunkPos winstonOnTheMap = Winston.mapOffset(map.data());
        var cold = TagKey.create(Registries.BIOME, Overgearedmod.id("has_structure/north_end_cave"));
        for (long seed = 1; seed <= 12; seed++) {
            RandomState random = random(registries, seed);
            StartingVillage.Chosen chosen = StartingVillage.choose(registries, generator, random,
                    helper.getLevel().getStructureManager(), seed, HEIGHT);
            Map<String, ChunkPos> sites = chosen.sites();
            for (String site : List.of(WorldSites.BAIRAN, WorldSites.NORTH_END_CAVE, WorldSites.ASHUR_MANOR, WorldSites.WINSTON)) {
                helper.assertTrue(sites.containsKey(site), "seed " + seed + " has no " + site + ": " + sites.keySet());
            }
            ChunkPos bairan = sites.get(WorldSites.BAIRAN);
            helper.assertTrue(map.regionAt(bairan.getMiddleBlockX(), bairan.getMiddleBlockZ()) == SatisfyMapData.PLAINS,
                    "Bairan is in the plains region of the lore map, seed " + seed + ": " + bairan);
            helper.assertTrue(bairan.getChessboardDistance(new ChunkPos(0, 0)) <= StartingVillage.SEARCH_RADIUS, "Bairan is at its place on the map");
            helper.assertTrue(chosen.landing().isPresent(), "a landing for seed " + seed);
            BlockPos landing = chosen.landing().get();
            helper.assertTrue(bairan.getChessboardDistance(new ChunkPos(landing)) <= 2, "the landing is on Bairan's plaza, seed " + seed + ": " + landing + " vs " + bairan);
            ChunkPos cave = sites.get(WorldSites.NORTH_END_CAVE);
            ChunkPos manor = sites.get(WorldSites.ASHUR_MANOR);
            ChunkPos winston = sites.get(WorldSites.WINSTON);
            helper.assertTrue(cave.z <= bairan.z - NorthEndCave.MIN_DISTANCE, "the cave is far north of Bairan, seed " + seed + ": " + cave);
            helper.assertTrue(map.isLand(cave.getMiddleBlockX(), cave.getMiddleBlockZ()), "the cave is on the map's land, seed " + seed + ": " + cave);
            helper.assertTrue(generator.getBiomeSource().getNoiseBiome(QuartPos.fromBlock(cave.getMiddleBlockX()), 16,
                    QuartPos.fromBlock(cave.getMiddleBlockZ()), random.sampler()).is(cold), "the cave is in the snowy north, seed " + seed + ": " + cave);
            int toManor = bairan.getChessboardDistance(manor);
            helper.assertTrue(toManor >= AshurManor.MIN_DISTANCE && toManor <= AshurManor.MAX_DISTANCE, "the manor is " + toManor + " chunks from Bairan, seed " + seed);
            helper.assertTrue(map.isLand(manor.getMiddleBlockX(), manor.getMiddleBlockZ()), "the manor is on the map's land, seed " + seed);
            ChunkPos winstonTarget = new ChunkPos(bairan.x + winstonOnTheMap.x, bairan.z + winstonOnTheMap.z);
            helper.assertTrue(winston.getChessboardDistance(winstonTarget) <= Winston.SEARCH_RADIUS,
                    "Winston near its place on the map, seed " + seed + ": " + winston + " vs " + winstonTarget);
            helper.assertTrue(bairan.getChessboardDistance(winston) >= Winston.KEEP_AWAY, "Winston away from Bairan, seed " + seed);
            helper.assertTrue(winston.getChessboardDistance(cave) >= Winston.KEEP_AWAY, "Winston away from the cave, seed " + seed);
            helper.assertTrue(winston.getChessboardDistance(manor) >= Winston.KEEP_AWAY, "Winston away from the manor, seed " + seed);
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
