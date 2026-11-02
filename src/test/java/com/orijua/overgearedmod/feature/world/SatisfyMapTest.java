package com.orijua.overgearedmod.feature.world;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SatisfyMapTest {
    // Bairan's cell is (2, 1): the middle of the P block; snow to the north, the sea ('.') two cells west (col 0) and two east (col 4).
    private static final SatisfyMap MAP = new SatisfyMap(List.of(
            ".SSS.",
            ".PPP.",
            ".PPP."), 192, 2, 1);

    @Test
    void theOriginIsInItsCell() {
        assertEquals('P', MAP.at(0, 0), "the dimension's (0, 0) is Bairan's plains");
    }

    @Test
    void northIsUpAndEastIsRight() {
        assertEquals('S', MAP.at(0, -192), "one cell north: the snow");
        assertEquals('P', MAP.at(0, 192), "one cell south");
        assertEquals('.', MAP.at(192 * 2, 0), "two cells east: off the map");
    }

    @Test
    void beyondTheMapIsNone() {
        assertEquals('.', MAP.at(100_000, 0));
        assertEquals('.', MAP.at(0, -100_000));
        assertEquals('.', MAP.at(Integer.MIN_VALUE / 2, Integer.MAX_VALUE / 2));
    }

    @Test
    void theWarpKeepsRegionsRecognisable() {
        // Far from a border the warp (at most a third of a cell) can't change the region.
        for (int offset = -20; offset <= 20; offset += 5) {
            assertEquals('P', MAP.at(offset, offset), "around the middle of the cell");
        }
    }

    @Test
    void sameDataSameAnswerAcrossInstances() {
        SatisfyMap other = new SatisfyMap(MAP.rows(), MAP.cell(), MAP.originCol(), MAP.originRow());
        for (int x = -400; x <= 400; x += 37) {
            for (int z = -400; z <= 400; z += 41) {
                assertEquals(MAP.at(x, z), other.at(x, z));
            }
        }
    }

    @Test
    void everyCellCentreHasItsLetter() {
        // Within 31 blocks of a centre the warp (at most cell/3 = 64) can't reach the half-cell border (96).
        int cell = MAP.cell();
        for (int row = 0; row < MAP.rows().size(); row++) {
            for (int col = 0; col < MAP.rows().get(row).length(); col++) {
                char expected = MAP.rows().get(row).charAt(col);
                int cx = (col - MAP.originCol()) * cell;
                int cz = (row - MAP.originRow()) * cell;
                for (int dx = -31; dx <= 31; dx += 31) {
                    for (int dz = -31; dz <= 31; dz += 31) {
                        assertEquals(expected, MAP.at(cx + dx, cz + dz), "cell (" + col + ", " + row + ") offset " + dx + "," + dz);
                    }
                }
            }
        }
    }

    @Test
    void theWarpBendsBorders() {
        // The S/P border (z = -96) would be a straight line without the warp. Depends on the fixed noise salts and
        // WARP_STEP: if those change, pick another stretch of border.
        boolean snow = false;
        boolean plains = false;
        for (int x = -280; x <= 280; x++) {
            char letter = MAP.at(x, -96);
            snow |= letter == 'S';
            plains |= letter == 'P';
        }
        assertTrue(snow && plains, "the border must wobble: both letters along z = -96 (x from -288 to 288)");
    }

    @Test
    void theWarpStaysWithinAThirdOfACell() {
        int cell = MAP.cell();
        int reach = cell / 2 + cell / 3;
        for (int x = -3 * cell; x <= 3 * cell; x += 7) {
            for (int z = -3 * cell; z <= 3 * cell; z += 7) {
                char got = MAP.at(x, z);
                boolean explained = false;
                for (int col = -4; col <= 8 && !explained; col++) {
                    for (int row = -4; row <= 6 && !explained; row++) {
                        int cx = (col - MAP.originCol()) * cell;
                        int cz = (row - MAP.originRow()) * cell;
                        if (Math.abs(x - cx) <= reach && Math.abs(z - cz) <= reach) {
                            explained = letterOf(col, row) == got;
                        }
                    }
                }
                assertTrue(explained, "(" + x + ", " + z + ") gave " + got + " but no cell within reach has it");
            }
        }
    }

    private static char letterOf(int col, int row) {
        if (row < 0 || row >= MAP.rows().size()) {
            return SatisfyMap.NONE;
        }
        String line = MAP.rows().get(row);
        return col < 0 || col >= line.length() ? SatisfyMap.NONE : line.charAt(col);
    }

    @Test
    void rejectsZeroCell() {
        assertThrows(IllegalArgumentException.class, () -> new SatisfyMap(List.of("P"), 0, 0, 0));
    }

    @Test
    void rejectsRaggedRows() {
        assertThrows(IllegalArgumentException.class, () -> new SatisfyMap(List.of("PP", "P"), 192, 0, 0));
    }

    @Test
    void rejectsEmptyRows() {
        assertThrows(IllegalArgumentException.class, () -> new SatisfyMap(List.of(), 192, 0, 0));
    }

    @Test
    void rejectsOriginOffTheMap() {
        assertThrows(IllegalArgumentException.class, () -> new SatisfyMap(List.of("PP"), 192, 2, 0));
        assertThrows(IllegalArgumentException.class, () -> new SatisfyMap(List.of("PP"), 192, 0, 1));
    }
}
