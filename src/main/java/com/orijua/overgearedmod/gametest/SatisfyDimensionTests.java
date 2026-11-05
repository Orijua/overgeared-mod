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
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.levelgen.DensityFunction;
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
import java.util.ArrayList;
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

    /** The map-driven noise settings of the dimension (vanilla's, with the terrain splines fed by the canon map). */
    private static final ResourceKey<NoiseGeneratorSettings> SATISFY_SETTINGS = ResourceKey.create(Registries.NOISE_SETTINGS, Overgearedmod.id("satisfy"));

    /** The noise of a world with this seed, with the dimension's own noise settings ({@link #SATISFY_SETTINGS}). */
    private static RandomState random(RegistryAccess registries, long seed) {
        var settings = registries.registryOrThrow(Registries.NOISE_SETTINGS).getHolderOrThrow(SATISFY_SETTINGS);
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
            Ground village = Ground.at(generator, random, bairan.getMiddleBlockX(), bairan.getMiddleBlockZ());
            helper.assertTrue(village.dry() && village.height() > generator.getSeaLevel(), "Bairan is on dry land above the sea, seed " + seed + ": " + village);
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
     * The ground at a block: the first solid block from the top ({@code height}, the sea floor at sea), whether it is dry
     * (no water over it), and {@code top}, the terrain's own level, where its depth is 0, without the 3D noise, the
     * jaggedness of peaks and the caves (a cave opening at the surface is not a hole in the terrain).
     */
    private record Ground(int height, boolean dry, int top) {
        static Ground at(ChunkGenerator generator, RandomState random, int x, int z) {
            int surface = generator.getBaseHeight(x, z, Heightmap.Types.WORLD_SURFACE_WG, HEIGHT, random);
            int floor = generator.getBaseHeight(x, z, Heightmap.Types.OCEAN_FLOOR_WG, HEIGHT, random);
            // depth = offset + a gradient that falls by 1 every 128 blocks up, so it is 0 at 128 * depth(y = 0).
            double depth = random.router().depth().compute(new DensityFunction.SinglePointContext(x, 0, z));
            return new Ground(floor, surface == floor, (int) Math.round(128 * depth));
        }
    }

    /**
     * The terrain follows the canon map ({@code overgearedmod:satisfy} noise settings, relief from {@code relief.png}):
     * a deep sea north of Bairan and beyond the map, a low coast where the map's land ends, dry lowland, high mountains
     * in the west, and Bairan's place, (0, 0), on dry plains above the sea. Points from the map's cells (block (0, 0) is
     * Bairan's pixel; a cell is 32 blocks).
     */
    @GameTest(template = TestSupport.EMPTY, timeoutTicks = 1200)
    public static void satisfyTerrainFollowsTheMap(GameTestHelper helper) {
        var registries = helper.getLevel().registryAccess();
        ChunkGenerator generator = generator(helper);
        helper.assertTrue(generator instanceof NoiseBasedChunkGenerator noise && noise.generatorSettings().is(SATISFY_SETTINGS),
                "the dimension uses the map-driven noise settings");
        SatisfyMap map = ((SatisfyBiomeSource) generator.getBiomeSource()).map();
        int sea = generator.getSeaLevel();
        for (long seed : new long[]{1L, 42L, -7_000_123L}) {
            RandomState random = random(registries, seed);
            // The open sea: 7x7 cells of relief 0 north of Bairan (cell 110, 10), and far beyond the map.
            for (int[] at : new int[][]{{-8, -580}, {-6000, 400}, {0, -3000}, {9000, 9000}}) {
                Ground ground = Ground.at(generator, random, at[0], at[1]);
                helper.assertTrue(!ground.dry() && ground.height() < sea - 15, "a deep sea at " + at[0] + "," + at[1] + ", seed " + seed + ": " + ground);
            }
            // Dry lowland (5x5 cells of relief 4 in the plains north of Bairan) and the mountains of the west (5x5 of relief 9).
            Ground lowland = Ground.at(generator, random, 88, -132);
            helper.assertTrue(lowland.dry() && lowland.top() > sea && lowland.top() <= sea + 15, "dry lowland, seed " + seed + ": " + lowland);
            Ground peak = Ground.at(generator, random, -1864, -4);
            helper.assertTrue(peak.dry() && peak.top() > sea + 100, "a high mountain, seed " + seed + ": " + peak);
            Ground eastPeak = Ground.at(generator, random, 88, 284);
            helper.assertTrue(eastPeak.top() > sea + 60, "the mountains south-east of Bairan, seed " + seed + ": " + eastPeak);
            // Bairan's place, the origin: dry plains a little above the sea, with no big drops. Vanilla's caves open at the
            // surface here and there, so only most of the actual ground has to be dry and above the sea.
            List<Integer> tops = new ArrayList<>();
            List<String> rejected = new ArrayList<>();
            for (int x = -24; x <= 24; x += 8) {
                for (int z = -24; z <= 24; z += 8) {
                    Ground ground = Ground.at(generator, random, x, z);
                    tops.add(ground.top());
                    if (!ground.dry() || ground.height() <= sea) {
                        rejected.add(x + "," + z + ": " + ground);
                    }
                }
            }
            tops.sort(null);
            helper.assertTrue(tops.getFirst() > sea && tops.getLast() <= sea + 20 && tops.getLast() - tops.getFirst() <= 8,
                    "Bairan's place is low, gentle plains above the sea, seed " + seed + ": " + tops);
            helper.assertTrue(rejected.size() <= 49 / 3, "Bairan's place is mostly dry ground, seed " + seed + "; not: " + rejected);
            // The north coast on the meridian of Bairan: the first water going north is where the map's land ends, and the
            // last dry ground before it is a low coast.
            int shore = Integer.MIN_VALUE;
            Ground last = null;
            int wet = 0;
            for (int z = -132; z >= -580 && shore == Integer.MIN_VALUE; z--) {
                Ground ground = Ground.at(generator, random, -8, z);
                if (ground.dry()) {
                    wet = 0;
                    last = ground;
                } else if (++wet == 4) {
                    shore = z + 3; // four wet columns in a row: the sea, not a puddle
                }
            }
            int mapShore = Integer.MIN_VALUE;
            for (int z = -132; z >= -580; z--) {
                if (!map.isLand(-8, z)) {
                    mapShore = z;
                    break;
                }
            }
            helper.assertTrue(shore != Integer.MIN_VALUE && Math.abs(shore - mapShore) <= 24,
                    "the sea starts where the map's land ends, seed " + seed + ": z " + shore + " vs " + mapShore);
            helper.assertTrue(last != null && last.top() <= sea + 8, "a low coast, seed " + seed + ": " + last);
        }
        helper.succeed();
    }
}
