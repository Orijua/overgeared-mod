package com.orijua.overgearedmod.feature.world;

import net.minecraft.world.level.ChunkPos;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
        assertTrue(!MAP.isLand(100_000, 100_000));
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
        // The P/M border (x = 16, between columns 2 and 3 on row 2) would be a straight line without the warp. Depends
        // on the fixed noise salts and steps: if those change, pick another stretch of border.
        boolean shifted = false;
        for (int z = centreZ(2) - 12; z <= centreZ(2) + 12; z++) {
            shifted |= MAP.regionAt(14, z) == 7 || MAP.regionAt(18, z) == SatisfyMapData.PLAINS;
        }
        assertTrue(shifted, "the border must wobble near x = 16");
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

    // --- The real map and the places chosen from it ------------------------------------------------------------------

    private static final Set<String> COLD = Set.of("minecraft:snowy_plains", "minecraft:snowy_taiga", "minecraft:frozen_peaks");

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
        assertEquals(new ChunkPos(-13, -6), offset, "about 200 blocks west and 100 north");
        assertTrue(Math.max(Math.abs(offset.x), Math.abs(offset.z)) >= Winston.KEEP_AWAY, "not on top of Bairan");
        String biome = west.biomeAtBlock(offset.getMiddleBlockX(), offset.getMiddleBlockZ());
        assertTrue(Set.of("minecraft:plains", "minecraft:taiga", "minecraft:snowy_plains", "minecraft:savanna").contains(biome),
                "Winston's place is in one of its biomes: " + biome);
    }

    @Test
    void theCaveIsLookedForOnTheSnowyNorthCoast() {
        SatisfyMapData west = SatisfyMapData.west();
        ChunkPos target = NorthEndCave.mapOffset(west);
        assertTrue(target.z <= -NorthEndCave.MIN_DISTANCE, "the Arctic Glacier is far north of Bairan: " + target);
        assertEquals("sea", west.biomeAtBlock(target.getMiddleBlockX(), target.getMiddleBlockZ()), "the glacier itself is at sea");
        // Cold land of the map close to the glacier, and far enough north of Bairan.
        int found = 0;
        for (int dx = -NorthEndCave.SEARCH_RADIUS; dx <= NorthEndCave.SEARCH_RADIUS; dx += 2) {
            for (int dz = -NorthEndCave.SEARCH_RADIUS; dz <= NorthEndCave.SEARCH_RADIUS; dz += 2) {
                ChunkPos chunk = new ChunkPos(target.x + dx, target.z + dz);
                if (-chunk.z >= NorthEndCave.MIN_DISTANCE && COLD.contains(west.biomeAtBlock(chunk.getMiddleBlockX(), chunk.getMiddleBlockZ()))) {
                    found++;
                }
            }
        }
        assertTrue(found >= 50, "plenty of cold land to choose from near the glacier: " + found);
    }
}
