package com.orijua.overgearedmod.feature.world;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SatisfyTerrainTest {
    private static final SatisfyTerrain ISLAND = SatisfyTerrains.get(SatisfyTerrains.TEST_ISLAND);

    @Test
    void reliefIsInterpolatedBetweenCellCentres() {
        SatisfyTerrain terrain = new SatisfyTerrain(List.of("09", "09"), 100, 0, 0);
        assertEquals(0.0, terrain.relief(0, 0), 1e-9);
        assertEquals(9.0, terrain.relief(100, 0), 1e-9);
        assertEquals(4.5, terrain.relief(50, 50), 1e-9);
    }

    @Test
    void beyondTheGridIsSea() {
        assertEquals(0.0, ISLAND.relief(100_000, 0), 1e-9);
        assertEquals(0.0, ISLAND.relief(0, -100_000), 1e-9);
    }

    @Test
    void theIslandRisesFromTheSeaToAPeak() {
        assertEquals(9.0, ISLAND.relief(0, 0), 1e-9);
        double previous = ISLAND.relief(6 * 96 + 200, 0);
        for (int x = 6 * 96; x >= 0; x -= 96) {
            double now = ISLAND.relief(x, 0);
            assertTrue(now >= previous, "the relief never drops going inland at x=" + x);
            previous = now;
        }
    }

    @Test
    void channelsStayInsideTheirDeclaredRange() {
        for (SatisfyTerrain.Channel channel : SatisfyTerrain.Channel.values()) {
            for (double relief = 0; relief <= SatisfyTerrain.MAX_RELIEF; relief += 0.1) {
                double value = channel.of(relief);
                assertTrue(value >= channel.min() - 1e-9 && value <= channel.max() + 1e-9, channel + " at " + relief + ": " + value);
            }
        }
    }

    @Test
    void seaIsBelowTheCoastAndMountainsAreErodedLess() {
        assertTrue(SatisfyTerrain.Channel.CONTINENTS.of(0) < -0.455, "deep sea is below the ocean threshold");
        assertTrue(SatisfyTerrain.Channel.CONTINENTS.of(3.5) > -0.19, "the coast is above the shore band");
        assertTrue(SatisfyTerrain.Channel.EROSION.of(9) < SatisfyTerrain.Channel.EROSION.of(4), "peaks are less eroded");
    }

    @Test
    void badGridsAreRejected() {
        assertThrows(IllegalArgumentException.class, () -> new SatisfyTerrain(List.of("01", "0"), 10, 0, 0));
        assertThrows(IllegalArgumentException.class, () -> new SatisfyTerrain(List.of("0x"), 10, 0, 0));
        assertThrows(IllegalArgumentException.class, () -> new SatisfyTerrain(List.of("01"), 10, 5, 0));
        assertThrows(IllegalArgumentException.class, () -> new SatisfyTerrain(List.of("01"), 0, 0, 0));
    }
}
