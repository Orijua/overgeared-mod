package com.orijua.overgearedmod.feature.world;

import net.minecraft.world.level.ChunkPos;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SatisfyMapTest {
    private static final Map<Integer, String> LEGEND = Map.of(0, "sea", 1, "minecraft:plains", 5, "minecraft:snowy_plains",
            7, "minecraft:windswept_hills");

    // 5x4 cells of 16 px (2 blocks per pixel: 32 blocks per cell). Bairan is pixel (40, 40): the middle of cell (2, 2).
    //   sea  S  S  S  sea
    //   sea  S  P  P  sea
    //   sea  P  P  M  sea
    //   sea sea sea sea sea
    private static final byte[] REGION = {
            0, 5, 5, 5, 0,
            0, 5, 1, 1, 0,
            0, 1, 1, 7, 0,
            0, 0, 0, 0, 0};
    private static final SatisfyMap MAP = new SatisfyMap(new SatisfyMapData(5, 4, 2, 16, 40, 40, relief(REGION), REGION, LEGEND));
    private static final int CELL = 32;

    /** Land is relief 4, the sea 0, as the converter writes them. */
    private static byte[] relief(byte[] region) {
        byte[] relief = new byte[region.length];
        for (int i = 0; i < region.length; i++) {
            relief[i] = (byte) (region[i] == 0 ? 0 : 4);
        }
        return relief;
    }

    /** Block x of the middle of a column (block 0 is the middle of column 2). */
    private static int centreX(int col) {
        return (col - 2) * CELL;
    }

    private static int centreZ(int row) {
        return (row - 2) * CELL;
    }

    @Test
    void theOriginIsBairansPlains() {
        assertEquals(SatisfyMapData.PLAINS, MAP.regionAt(0, 0));
    }

    @Test
    void northIsUpAndEastIsRight() {
        assertEquals(5, MAP.regionAt(centreX(1), centreZ(0)), "north-west: the snow");
        assertEquals(7, MAP.regionAt(centreX(3), centreZ(2)), "east: the hills");
        assertEquals(SatisfyMapData.SEA, MAP.regionAt(centreX(2), centreZ(3)), "south: the sea");
    }

    @Test
    void beyondTheMapIsSea() {
        assertEquals(SatisfyMapData.SEA, MAP.regionAt(100_000, 0));
        assertEquals(SatisfyMapData.SEA, MAP.regionAt(0, -100_000));
        assertEquals(SatisfyMapData.SEA, MAP.regionAt(Integer.MIN_VALUE / 2, Integer.MAX_VALUE / 2));
        assertFalse(MAP.isLand(100_000, 100_000));
        assertTrue(MAP.isLand(0, 0));
    }

    @Test
    void theMiddleOfEveryCellKeepsItsRegion() {
        // The warp moves a point by less than half a cell, so a cell's middle never leaves it.
        int reach = (int) Math.floor(CELL * (0.5 - SatisfyMap.MAX_WARP));
        for (int row = 0; row < 4; row++) {
            for (int col = 0; col < 5; col++) {
                for (int d = -reach; d <= reach; d++) {
                    assertEquals(REGION[row * 5 + col], MAP.regionAt(centreX(col) + d, centreZ(row) - d), "cell (" + col + ", " + row + ")");
                }
            }
        }
    }

    @Test
    void theWarpStaysWithinHalfACell() {
        int reach = (int) Math.ceil(CELL * SatisfyMap.MAX_WARP) + 1;
        SatisfyMapData data = MAP.data();
        for (int x = -4 * CELL; x <= 4 * CELL; x += 3) {
            for (int z = -4 * CELL; z <= 4 * CELL; z += 3) {
                int got = MAP.regionAt(x, z);
                boolean explained = false;
                for (int dx = -reach; dx <= reach && !explained; dx += reach) {
                    for (int dz = -reach; dz <= reach && !explained; dz += reach) {
                        explained = data.regionAtBlock(x + dx, z + dz) == got;
                    }
                }
                assertTrue(explained, "(" + x + ", " + z + ") gave " + got + " but no cell within reach has it");
            }
        }
    }

    @Test
    void theWarpBendsBorders() {
        // The P/M border would be the straight line x = 16 (between columns 2 and 3) on row 2 without the warp. Walk it
        // along z and see where the region really changes. Depends on the fixed noise salts and steps: if those change,
        // pick another stretch of border (the bounds below say what to look for).
        java.util.Set<Integer> edges = new java.util.TreeSet<>();
        int deviation = 0;
        for (int z = centreZ(2) - 8; z <= centreZ(2) + 8; z += 2) {
            int edge = Integer.MIN_VALUE;
            for (int x = 0; x <= 32 && edge == Integer.MIN_VALUE; x++) {
                if (MAP.regionAt(x, z) == 7) {
                    edge = x;
                }
            }
            assertTrue(edge != Integer.MIN_VALUE, "the hills start somewhere east of Bairan at z=" + z);
            edges.add(edge);
            deviation = Math.max(deviation, Math.abs(edge - 16));
        }
        // Measured: the border changes at x = 16, 17 and 18 along the walk (deviation 2 blocks).
        assertTrue(deviation >= 2, "the warp bends the border by at least 2 blocks: " + deviation);
        assertTrue(deviation <= CELL * SatisfyMap.MAX_WARP + 1, "and by no more than the warp's reach: " + deviation);
        assertTrue(edges.size() >= 2, "it is not a rigid shift: " + edges);
    }

    @Test
    void sameDataSameAnswerAcrossInstances() {
        SatisfyMap other = new SatisfyMap(MAP.data());
        for (int x = -200; x <= 200; x += 7) {
            for (int z = -200; z <= 200; z += 11) {
                assertEquals(MAP.regionAt(x, z), other.regionAt(x, z));
            }
        }
    }

    @Test
    void theReliefIsReadAtTheWarpedPointAndIsSmooth() {
        SatisfyMapData data = MAP.data();
        int reach = (int) Math.ceil(CELL * SatisfyMap.MAX_WARP) + 1;
        for (int x = -4 * CELL; x <= 4 * CELL; x += 3) {
            for (int z = -4 * CELL; z <= 4 * CELL; z += 3) {
                double relief = MAP.reliefAt(x, z);
                assertTrue(relief >= 0 && relief <= SatisfyMapData.MAX_RELIEF, "relief " + relief + " at " + x + "," + z);
                // Bilinear at a point less than a warp away: never far from the unwarped value.
                double flat = data.relief(x, z);
                assertTrue(Math.abs(relief - flat) <= 8.0 * reach / CELL + 1e-9, "the warp is small at " + x + "," + z);
                // No steps: neighbours differ by little.
                assertTrue(Math.abs(MAP.reliefAt(x + 1, z) - relief) < 0.5, "smooth at " + x + "," + z);
            }
        }
        // Measured 4.0 exactly: the point and its warp reach lie in land cells that all have relief 4.
        assertEquals(4.0, MAP.reliefAt(centreX(2), centreZ(1) + CELL / 2), 1e-6, "inland between two land cells");
        assertEquals(0.0, MAP.reliefAt(100_000, 0), 1e-9, "the open sea beyond the map");
    }

    @Test
    void theCoastOfTheReliefFollowsTheCoastOfTheRegions() {
        // Relief halfway between sea (0) and land (4) is 2: it falls on the border of the regions, warp included. Rows 0-2
        // have the sea in column 0 and land in column 1; stay a warp away from row 3, all sea.
        for (int z = centreZ(0) + 16; z <= centreZ(2) - 16; z++) {
            int edge = Integer.MIN_VALUE;
            for (int x = centreX(0); x <= centreX(2); x++) {
                if (MAP.isLand(x, z)) {
                    edge = x;
                    break;
                }
            }
            assertTrue(edge != Integer.MIN_VALUE, "land on row z=" + z);
            assertTrue(MAP.reliefAt(edge - 2, z) < 2.0 + 0.5 && MAP.reliefAt(edge + 2, z) > 2.0 - 0.5,
                    "the relief crosses the coast where the region does, z=" + z + ": " + MAP.reliefAt(edge - 2, z) + " / " + MAP.reliefAt(edge + 2, z));
        }
    }

    // --- The real map and the places chosen from it ------------------------------------------------------------------

    /** The biomes of a worldgen tag file shipped in the mod (data/overgearedmod/tags/worldgen/biome/&lt;path&gt;.json). */
    private static Set<String> biomeTag(String path) {
        try (java.io.InputStream in = SatisfyMapTest.class.getResourceAsStream("/data/overgearedmod/tags/worldgen/biome/" + path + ".json")) {
            assertTrue(in != null, "the tag " + path + " exists");
            Set<String> values = new java.util.HashSet<>();
            com.google.gson.JsonParser.parseReader(new java.io.InputStreamReader(in, java.nio.charset.StandardCharsets.UTF_8))
                    .getAsJsonObject().getAsJsonArray("values").forEach(value -> values.add(value.getAsString()));
            return values;
        } catch (java.io.IOException e) {
            throw new AssertionError(e);
        }
    }

    @Test
    void bairanIsInThePlainsOfTheWestMap() {
        SatisfyMap west = new SatisfyMap(SatisfyMapData.west());
        int reach = (int) Math.floor(west.data().cellBlocks() * (0.5 - SatisfyMap.MAX_WARP));
        for (int d = -reach; d <= reach; d++) {
            assertEquals(SatisfyMapData.PLAINS, west.regionAt(d, d), "Bairan's plains");
        }
    }

    @Test
    void winstonIsBairansNeighbourOnTheMap() {
        SatisfyMapData west = SatisfyMapData.west();
        ChunkPos offset = Winston.mapOffset(west);
        assertEquals(SatisfyPlaces.chunkOffset(west, SatisfyPlaces.west().get("winston").orElseThrow()), offset, "from places.json");
        assertTrue(offset.x < 0 && offset.z < 0, "north-west of Bairan: " + offset);
        assertTrue(Math.max(Math.abs(offset.x), Math.abs(offset.z)) >= Winston.KEEP_AWAY, "not on top of Bairan");
        String biome = west.biomeAtBlock(offset.getMiddleBlockX(), offset.getMiddleBlockZ());
        assertTrue(biomeTag("has_structure/winston").contains(biome), "Winston's place is in one of its biomes: " + biome);
    }

    @Test
    void theCaveIsLookedForOnTheSnowyNorthCoast() {
        SatisfyMapData west = SatisfyMapData.west();
        Set<String> cold = biomeTag("has_structure/north_end_cave");
        ChunkPos target = NorthEndCave.mapOffset(west);
        assertTrue(target.z <= -NorthEndCave.MIN_DISTANCE, "the Arctic Glacier is far north of Bairan: " + target);
        assertEquals("sea", west.biomeAtBlock(target.getMiddleBlockX(), target.getMiddleBlockZ()), "the glacier itself is at sea");
        // Cold land of the map close to the glacier, and far enough north of Bairan.
        int found = 0;
        for (int dx = -NorthEndCave.SEARCH_RADIUS; dx <= NorthEndCave.SEARCH_RADIUS; dx += 2) {
            for (int dz = -NorthEndCave.SEARCH_RADIUS; dz <= NorthEndCave.SEARCH_RADIUS; dz += 2) {
                ChunkPos chunk = new ChunkPos(target.x + dx, target.z + dz);
                if (-chunk.z >= NorthEndCave.MIN_DISTANCE && cold.contains(west.biomeAtBlock(chunk.getMiddleBlockX(), chunk.getMiddleBlockZ()))) {
                    found++;
                }
            }
        }
        assertTrue(found >= 1, "at least one cold candidate near the glacier: " + found);
        assertTrue(found >= MIN_COLD_CANDIDATES, "plenty of cold land to choose from near the glacier: " + found);
    }

    /** Cold candidates (every other chunk within SEARCH_RADIUS of the glacier label, 16+ chunks north of Bairan): measured 115, half of it. */
    private static final int MIN_COLD_CANDIDATES = 57;

    @Test
    void theMountainPrefilterRejectsPeaksAndKeepsTheCoast() {
        // A uniform 21x21 map of cells with block 0 at its middle (cell 10): the warp never takes it off the map.
        byte[] region = new byte[21 * 21];
        java.util.Arrays.fill(region, (byte) 5);
        byte[] high = new byte[region.length];
        byte[] low = new byte[region.length];
        java.util.Arrays.fill(high, (byte) 9);
        java.util.Arrays.fill(low, (byte) 3);
        SatisfyMap peaks = new SatisfyMap(new SatisfyMapData(21, 21, 2, 16, 168, 168, high, region, LEGEND));
        SatisfyMap coast = new SatisfyMap(new SatisfyMapData(21, 21, 2, 16, 168, 168, low, region, LEGEND));
        assertTrue(peaks.reliefAt(0, 0) >= NorthEndCave.MAX_MAP_RELIEF, "peaks are over the cut: " + peaks.reliefAt(0, 0));
        assertTrue(coast.reliefAt(0, 0) < NorthEndCave.MAX_MAP_RELIEF, "the coast is under it: " + coast.reliefAt(0, 0));
    }

    @Test
    void landOnTheEdgeOfTheGridIsSeaBeyondIt() {
        // 3x3 cells of 32 blocks; the whole top row is land, the rest sea. Block 0 is the middle of cell (1, 1).
        byte[] region = {5, 5, 5, 0, 0, 0, 0, 0, 0};
        byte[] relief = {4, 4, 4, 0, 0, 0, 0, 0, 0};
        SatisfyMap map = new SatisfyMap(new SatisfyMapData(3, 3, 2, 16, 24, 24, relief, region, LEGEND));
        assertTrue(map.isLand(0, -CELL), "the top row, in the middle of its cell, is land");
        assertFalse(map.isLand(0, -3 * CELL), "beyond the north edge it is sea");
        assertEquals(0.0, map.reliefAt(0, -100_000), 1e-9);
        assertFalse(map.isLand(0, 0), "the second row is sea");
        // The coast forms at the edge: the relief falls to the sea beyond it.
        assertTrue(map.reliefAt(0, -CELL) > map.reliefAt(0, -2 * CELL), "the relief falls towards the sea off the edge");
    }
}
