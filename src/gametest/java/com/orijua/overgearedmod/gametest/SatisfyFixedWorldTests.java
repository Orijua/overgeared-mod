package com.orijua.overgearedmod.gametest;

import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import com.orijua.overgearedmod.Overgearedmod;
import com.orijua.overgearedmod.feature.world.AshurManor;
import com.orijua.overgearedmod.feature.world.NorthEndCave;
import com.orijua.overgearedmod.feature.world.SatisfyBiomeSource;
import com.orijua.overgearedmod.feature.world.SatisfyMap;
import com.orijua.overgearedmod.feature.world.SatisfyNoise;
import com.orijua.overgearedmod.feature.world.SatisfyRelief;
import com.orijua.overgearedmod.feature.world.SatisfySites;
import com.orijua.overgearedmod.feature.world.SatisfyTerrain;
import com.orijua.overgearedmod.feature.world.SatisfyTerrainFunction;
import com.orijua.overgearedmod.feature.world.SatisfyTerrains;
import com.orijua.overgearedmod.feature.world.StartingVillage;
import com.orijua.overgearedmod.feature.world.Winston;
import com.orijua.overgearedmod.feature.world.WorldSites;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.RegistryOps;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.RandomState;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Satisfy is a world of the canon with a known map, so it is the same in every new world: the shape of the continent,
 * its coast and relief (the terrain reads the map and a noise with a fixed seed, never the world's), and the places at
 * fixed chunks. What still changes with the seed (vanilla's 3D noise and jaggedness, caves, aquifers, ores, trees) is
 * measured here.
 */
@GameTestHolder(Overgearedmod.MODID)
@PrefixGameTestTemplate(false)
public final class SatisfyFixedWorldTests {
    /** Nine seeds of all kinds: small, large, negative. */
    static final long[] SEEDS = {1L, 2L, 42L, 99L, -5L, -7_000_123L, 123_456_789L, 0xDEAD_BEEFL, 8_675_309_000_123L};

    /**
     * Over {@link #SEEDS}, measured 2026-11-07 (in brackets, the same with the seed's own vanilla noise in the terrain,
     * before it was fixed): the terrain's own level differs by 0 blocks at all 768 points (up to 64; land or sea of the
     * shape differed at 15 points) and meets the sea at the same block on every cut (moving by up to 12 blocks). What
     * the seed still moves, through vanilla's 3D noise, jaggedness and caves: the surface (WORLD_SURFACE_WG) by a median 4,
     * p90 15, p95 22, p99 39 and at most 98 blocks (a cave mouth or a peak); the solid ground (OCEAN_FLOOR_WG) by a median
     * 9, p95 27, at most 98 (10, 37, 99); whether a point is dry changes at 15 of 768 points; and the last dry column of a
     * coast cut by up to 27 blocks (31), the beaches' puddles and cave mouths. Bounds: measured + a margin.
     */
    private static final int MAX_SURFACE_SPREAD_MEDIAN = 6;
    private static final int MAX_SURFACE_SPREAD_P95 = 26;
    private static final int MAX_DRY_CHANGES = 20;
    private static final int MAX_LAST_DRY_SPREAD = 30;
    /** How far the terrain's shore may be from the map's (measured 11 on the south-east cut, 0-14 on the others): as the coast cuts. */
    private static final int SHAPE_SHORE_MARGIN = 24;

    private SatisfyFixedWorldTests() {
    }

    /** A straight cut across the map's coast, like the coast cuts of {@link SatisfyDimensionTests}. */
    private record Cut(String name, int startX, int startZ, int dx, int dz) {
    }

    private static final List<Cut> CUTS = List.of(new Cut("north x=-8", -8, 0, 0, -1), new Cut("north x=120", 120, 0, 0, -1),
            new Cut("north x=-300", -300, 0, 0, -1), new Cut("east", 0, -176, 1, 0), new Cut("south", -1136, 0, 0, 1),
            new Cut("west", 0, 496, -1, 0), new Cut("south-east", -608, 0, 1, 1));

    /** What a seed gives along a cut: where the terrain's own level crosses the sea, and the last dry column. */
    private record CutShape(List<Integer> shapeShores, int lastDry) {
    }

    private static int mapShore(SatisfyMap map, Cut cut) {
        for (int i = 0; i <= 6000; i++) {
            if (!map.isLand(cut.startX() + i * cut.dx(), cut.startZ() + i * cut.dz())) {
                return i;
            }
        }
        throw new AssertionError(cut.name() + ": no coast on the map");
    }

    private static CutShape cutShape(ChunkGenerator generator, RandomState random, Cut cut, int shore) {
        int sea = generator.getSeaLevel();
        List<Integer> shores = new ArrayList<>();
        int lastDry = Integer.MIN_VALUE;
        SatisfyDimensionTests.Ground previous = null;
        for (int i = shore - 120; i <= shore + 120; i++) {
            SatisfyDimensionTests.Ground ground = SatisfyDimensionTests.Ground.at(generator, random, cut.startX() + i * cut.dx(),
                    cut.startZ() + i * cut.dz());
            if (previous != null && (ground.top() > sea) != (previous.top() > sea)) {
                shores.add(i);
            }
            if (ground.dry()) {
                lastDry = i;
            }
            previous = ground;
        }
        return new CutShape(shores, lastDry);
    }

    /**
     * The shape of the terrain is the same in every seed: at 768 points all over the map (every 256 blocks) the terrain's
     * own level (its depth, without the 3D noise) is the same, and along seven cuts across the coast it meets the sea at
     * the same block, within {@link #SHAPE_SHORE_MARGIN} of the map's coast. The actual ground still moves with vanilla's
     * 3D noise and caves, which follow the seed: measured and bounded ({@link #MAX_LAST_DRY_SPREAD} and the rest).
     */
    @GameTest(template = TestSupport.EMPTY, timeoutTicks = 4800)
    public static void theTerrainIsTheSameInEverySeed(GameTestHelper helper) {
        var registries = helper.getLevel().registryAccess();
        ChunkGenerator generator = SatisfyDimensionTests.generator(helper);
        SatisfyMap map = ((SatisfyBiomeSource) generator.getBiomeSource()).map();
        int sea = generator.getSeaLevel();
        RandomState[] randoms = Arrays.stream(SEEDS).mapToObj(seed -> SatisfyDimensionTests.random(registries, seed)).toArray(RandomState[]::new);
        int points = 0;
        int maxTopSpread = 0;
        int shapeChanges = 0;
        int dryChanges = 0;
        List<Integer> surfaceSpreads = new ArrayList<>();
        List<Integer> floorSpreads = new ArrayList<>();
        for (int x = -3500; x <= 4600; x += 256) {
            for (int z = -880; z <= 5200; z += 256) {
                points++;
                int minTop = Integer.MAX_VALUE, maxTop = Integer.MIN_VALUE;
                int minFloor = Integer.MAX_VALUE, maxFloor = Integer.MIN_VALUE;
                int minSurface = Integer.MAX_VALUE, maxSurface = Integer.MIN_VALUE;
                int shapeLand = 0;
                int dry = 0;
                for (RandomState random : randoms) {
                    SatisfyDimensionTests.Ground ground = SatisfyDimensionTests.Ground.at(generator, random, x, z);
                    int surface = generator.getBaseHeight(x, z, Heightmap.Types.WORLD_SURFACE_WG, SatisfyDimensionTests.HEIGHT, random);
                    minTop = Math.min(minTop, ground.top());
                    maxTop = Math.max(maxTop, ground.top());
                    minFloor = Math.min(minFloor, ground.height());
                    maxFloor = Math.max(maxFloor, ground.height());
                    minSurface = Math.min(minSurface, surface);
                    maxSurface = Math.max(maxSurface, surface);
                    shapeLand += ground.top() > sea ? 1 : 0;
                    dry += ground.dry() ? 1 : 0;
                }
                maxTopSpread = Math.max(maxTopSpread, maxTop - minTop);
                shapeChanges += shapeLand != 0 && shapeLand != randoms.length ? 1 : 0;
                dryChanges += dry != 0 && dry != randoms.length ? 1 : 0;
                surfaceSpreads.add(maxSurface - minSurface);
                floorSpreads.add(maxFloor - minFloor);
            }
        }
        surfaceSpreads.sort(null);
        floorSpreads.sort(null);
        int median = surfaceSpreads.get(points / 2);
        int p95 = surfaceSpreads.get(points * 95 / 100);
        Overgearedmod.LOGGER.info("Satisfy terrain over {} seeds, {} points: terrain level spread max {}, land or sea of the shape differs at {};"
                        + " surface spread median {}, p90 {}, p95 {}, p99 {}, max {}; ground (floor) spread median {}, p95 {}, max {};"
                        + " dry or wet differs at {}", SEEDS.length, points, maxTopSpread, shapeChanges, median,
                surfaceSpreads.get(points * 90 / 100), p95, surfaceSpreads.get(points * 99 / 100), surfaceSpreads.getLast(),
                floorSpreads.get(points / 2), floorSpreads.get(points * 95 / 100), floorSpreads.getLast(), dryChanges);
        helper.assertTrue(maxTopSpread == 0, "the terrain's own level is the same in every seed: it differs by up to " + maxTopSpread);
        helper.assertTrue(shapeChanges == 0, "land and sea of the terrain's shape are the same in every seed: " + shapeChanges + " points differ");
        helper.assertTrue(median <= MAX_SURFACE_SPREAD_MEDIAN && p95 <= MAX_SURFACE_SPREAD_P95,
                "the surface moves little with the seed: median " + median + ", p95 " + p95);
        helper.assertTrue(dryChanges <= MAX_DRY_CHANGES, "dry or wet changes with the seed at " + dryChanges + " points");
        // The coast cuts.
        List<String> cuts = new ArrayList<>();
        List<String> wrong = new ArrayList<>();
        for (Cut cut : CUTS) {
            int shore = mapShore(map, cut);
            List<List<Integer>> shapes = new ArrayList<>();
            int minDry = Integer.MAX_VALUE, maxDry = Integer.MIN_VALUE;
            for (RandomState random : randoms) {
                CutShape shape = cutShape(generator, random, cut, shore);
                shapes.add(shape.shapeShores());
                minDry = Math.min(minDry, shape.lastDry());
                maxDry = Math.max(maxDry, shape.lastDry());
            }
            List<Integer> first = shapes.getFirst();
            if (shapes.stream().distinct().count() != 1) {
                wrong.add(cut.name() + ": the terrain meets the sea at different blocks in different seeds: " + shapes);
            } else if (first.size() != 1 || Math.abs(first.getFirst() - shore) > SHAPE_SHORE_MARGIN) {
                wrong.add(cut.name() + ": the terrain meets the sea once, near the map's coast " + shore + ": " + first);
            }
            if (maxDry - minDry > MAX_LAST_DRY_SPREAD) {
                wrong.add(cut.name() + ": the last dry ground moves by " + (maxDry - minDry) + " blocks between seeds");
            }
            cuts.add(cut.name() + ": map shore " + shore + ", terrain shore " + first + ", last dry " + minDry + ".." + maxDry);
        }
        Overgearedmod.LOGGER.info("Satisfy coast cuts over {} seeds: {}", SEEDS.length, cuts);
        helper.assertTrue(wrong.isEmpty(), "the coast changes with the seed: " + wrong);
        helper.succeed();
    }

    /**
     * The four places are at the fixed chunks of {@code sites.json} in every seed, with no search: on dry land above the
     * sea (at the middle of the chunk, the terrain's own level), their structure fitting as the old search required (dry,
     * and the cave and manor on flat ground), and the landing on Bairan's square.
     */
    @GameTest(template = TestSupport.EMPTY, timeoutTicks = 2400)
    public static void theSitesAreTheSameInEverySeed(GameTestHelper helper) {
        var registries = helper.getLevel().registryAccess();
        ChunkGenerator generator = SatisfyDimensionTests.generator(helper);
        var templates = helper.getLevel().getStructureManager();
        var height = SatisfyDimensionTests.HEIGHT;
        int sea = generator.getSeaLevel();
        Map<String, ChunkPos> fixed = SatisfySites.west().all();
        helper.assertTrue(fixed.keySet().equals(Set.of(WorldSites.BAIRAN, WorldSites.NORTH_END_CAVE, WorldSites.ASHUR_MANOR, WorldSites.WINSTON)),
                "sites.json has the four places: " + fixed.keySet());
        List<String> failures = new ArrayList<>();
        for (long seed : SEEDS) {
            RandomState random = SatisfyDimensionTests.random(registries, seed);
            StartingVillage.Chosen chosen = StartingVillage.choose(registries, generator, random, templates, seed, height);
            helper.assertTrue(chosen.sites().equals(fixed), "seed " + seed + " has the fixed sites: " + chosen.sites() + " vs " + fixed);
            for (Map.Entry<String, ChunkPos> site : fixed.entrySet()) {
                ChunkPos chunk = site.getValue();
                SatisfyDimensionTests.Ground ground = SatisfyDimensionTests.Ground.at(generator, random, chunk.getMiddleBlockX(), chunk.getMiddleBlockZ());
                if (!ground.dry() || ground.top() <= sea) {
                    failures.add(site.getKey() + " is not on dry land above the sea, seed " + seed + ": " + ground);
                }
                boolean fits = switch (site.getKey()) {
                    case WorldSites.BAIRAN -> StartingVillage.villageAt(registries, generator, random, templates, seed, height, chunk).isPresent();
                    case WorldSites.NORTH_END_CAVE -> NorthEndCave.fitsAt(registries, generator, random, templates, seed, height, chunk);
                    case WorldSites.ASHUR_MANOR -> AshurManor.fitsAt(registries, generator, random, templates, seed, height, chunk);
                    default -> Winston.fitsAt(registries, generator, random, templates, seed, height, chunk);
                };
                if (!fits) {
                    failures.add(site.getKey() + " does not fit at " + chunk + ", seed " + seed);
                }
            }
            helper.assertTrue(chosen.landing().isPresent(), "a landing, seed " + seed);
            BlockPos landing = chosen.landing().get();
            ChunkPos bairan = fixed.get(WorldSites.BAIRAN);
            if (bairan.getChessboardDistance(new ChunkPos(landing)) > 2 || landing.getY() <= sea) {
                failures.add("the landing is on Bairan's square above the sea, seed " + seed + ": " + landing);
            }
        }
        helper.assertTrue(failures.isEmpty(), failures.size() + " checks of the fixed sites failed: " + failures);
        helper.succeed();
    }

    /**
     * The noise settings' climate (continents, erosion, ridges) is the {@code satisfy_terrain} function with the fixed
     * detail: the same value in every seed, the one {@link SatisfyNoise#climate} gives, within the function's
     * minValue/maxValue. And the function decodes from its JSON, which reads no noise of the world.
     */
    @GameTest(template = TestSupport.EMPTY)
    public static void theClimateOfTheTerrainIgnoresTheSeed(GameTestHelper helper) {
        var registries = helper.getLevel().registryAccess();
        RandomState[] randoms = Arrays.stream(SEEDS).mapToObj(seed -> SatisfyDimensionTests.random(registries, seed)).toArray(RandomState[]::new);
        SatisfyRelief west = SatisfyTerrains.get(SatisfyTerrains.WEST);
        for (SatisfyTerrain.Channel channel : SatisfyTerrain.Channel.values()) {
            for (int x = -3500; x <= 4600; x += 397) {
                for (int z = -880; z <= 5200; z += 401) {
                    double expected = SatisfyNoise.climate(channel, west, true, x, z);
                    for (RandomState random : randoms) {
                        DensityFunction function = switch (channel) {
                            case CONTINENTS -> random.router().continents();
                            case EROSION -> random.router().erosion();
                            case RIDGES -> random.router().ridges();
                        };
                        double value = function.compute(new DensityFunction.SinglePointContext(x, 70, z));
                        helper.assertTrue(value == expected, channel + " at " + x + "," + z + " is the fixed one: " + value + " vs " + expected);
                        helper.assertTrue(value >= function.minValue() && value <= function.maxValue(), channel + " within its bounds");
                    }
                }
            }
            var ops = RegistryOps.create(JsonOps.INSTANCE, registries);
            var json = JsonParser.parseString("{\"type\": \"overgearedmod:satisfy_terrain\", \"channel\": \"" + channel.getSerializedName()
                    + "\", \"map\": \"overgearedmod:west\", \"detail\": true}");
            DensityFunction decoded = DensityFunction.DIRECT_CODEC.parse(ops, json).getOrThrow(IllegalStateException::new);
            helper.assertTrue(decoded instanceof SatisfyTerrainFunction terrain && terrain.detail() && terrain.channel() == channel,
                    "the JSON decodes to the fixed terrain function: " + decoded);
            helper.assertTrue(decoded.mapAll(function -> function) == decoded, "the world's random state has nothing to seed in it");
            helper.assertTrue(decoded.minValue() == channel.noisyMin() && decoded.maxValue() == channel.noisyMax(), channel + " bounds");
        }
        helper.succeed();
    }
}
