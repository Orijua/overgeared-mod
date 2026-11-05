package com.orijua.overgearedmod.feature.world;

import net.minecraft.data.worldgen.TerrainProvider;
import net.minecraft.util.CubicSpline;
import net.minecraft.util.ToFloatFunction;
import org.junit.jupiter.api.Test;

import java.util.function.ToDoubleFunction;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The height each relief gives, read from vanilla's own offset spline ({@link TerrainProvider}, the one the noise settings
 * {@code overgearedmod:satisfy} use): the surface is about where the depth is 0, {@code 63.5 + 128 * offset}. It leaves
 * out the 3D noise and the jaggedness (a few blocks, more on peaks), so the gametests check the real heights; this one
 * checks every relief and every corner of the noise, fast.
 */
class SatisfyTerrainHeightTest {
    private static final int SEA_LEVEL = 63;

    /** A point of the climate: continentalness, erosion, ridges (weirdness). */
    private record Climate(float c, float e, float r) {
    }

    private static ToFloatFunction<Climate> coordinate(ToDoubleFunction<Climate> value) {
        return new ToFloatFunction<>() {
            @Override
            public float apply(Climate climate) {
                return (float) value.applyAsDouble(climate);
            }

            @Override
            public float minValue() {
                return -2;
            }

            @Override
            public float maxValue() {
                return 2;
            }
        };
    }

    /** Vanilla's peaks and valleys (NoiseRouterData.peaksAndValleys, which needs a bootstrapped game). */
    private static double peaksAndValleys(double weirdness) {
        return -(Math.abs(Math.abs(weirdness) - 2.0 / 3.0) - 1.0 / 3.0) * 3.0;
    }

    private static final CubicSpline<Climate, ToFloatFunction<Climate>> OFFSET = TerrainProvider.overworldOffset(
            coordinate(Climate::c), coordinate(Climate::e), coordinate(climate -> peaksAndValleys(climate.r())), false);

    private static double height(double relief, double cNoise, double eNoise, double rNoise) {
        Climate climate = new Climate((float) SatisfyTerrain.Channel.CONTINENTS.of(relief, cNoise),
                (float) SatisfyTerrain.Channel.EROSION.of(relief, eNoise), (float) SatisfyTerrain.Channel.RIDGES.of(relief, rNoise));
        return 63.5 + 128 * OFFSET.apply(climate);
    }

    /** The lowest and highest height a relief gives over the corners of the noise. */
    private static double[] range(double relief) {
        double low = Double.POSITIVE_INFINITY;
        double high = Double.NEGATIVE_INFINITY;
        for (int c = -1; c <= 1; c++) {
            for (int e = -1; e <= 1; e++) {
                for (int r = -1; r <= 1; r++) {
                    double h = height(relief, c, e, r);
                    low = Math.min(low, h);
                    high = Math.max(high, h);
                }
            }
        }
        return new double[]{low, high};
    }

    private static void assertBetween(double relief, double low, double high) {
        double[] range = range(relief);
        assertTrue(range[0] >= low && range[1] <= high, "relief " + relief + " gives " + Math.round(range[0]) + "-" + Math.round(range[1])
                + ", expected " + low + "-" + high);
    }

    @Test
    void eachReliefHasItsHeight() {
        assertBetween(0, 30, 40);                       // deep sea
        assertBetween(2, 40, SEA_LEVEL - 5);             // shallow sea and lakes
        assertBetween(3, SEA_LEVEL, 70);                 // a low coast, always dry
        assertBetween(4, SEA_LEVEL + 2, 76);             // lowland
        assertBetween(5, SEA_LEVEL + 2, 80);             // high lowland (Bairan)
        assertTrue(height(4, 0, 0, 0) >= SEA_LEVEL + 5, "the lowland is a few blocks above the sea: " + height(4, 0, 0, 0));
        assertBetween(6.5, 65, 110);                     // hills
        assertBetween(8, 100, 180);                      // mountains
        assertBetween(9, 140, 250);                      // peaks
        assertTrue(height(9, 0, 0, 0) > 200, "the peaks are high: " + height(9, 0, 0, 0));
    }

    @Test
    void theShoreIsAtTheBorderOfACoastCell() {
        // The border between a coast cell (3) and the sea (2) is relief 2.5: the shore is there, give or take the noise.
        assertTrue(height(2.3, 0, 0, 0) < SEA_LEVEL && height(2.75, 0, 0, 0) > SEA_LEVEL,
                height(2.3, 0, 0, 0) + " / " + height(2.75, 0, 0, 0));
        assertTrue(Math.abs(height(2.5, 0, 0, 0) - SEA_LEVEL) <= 3, "at the border: " + height(2.5, 0, 0, 0));
    }

    @Test
    void noCliffsBelowTheMountains() {
        // A cell is 32 blocks: where the relief changes by one per cell, 1/16 of relief is 2 blocks. Up to relief 8 the
        // ground climbs at most 3 blocks per block there (vanilla's own mountain-foot cliff is crossed slowly).
        for (double relief = 2.75; relief < 8; relief += 1.0 / 16) {
            double step = Math.abs(height(relief + 1.0 / 16, 0, 0, 0) - height(relief, 0, 0, 0));
            assertTrue(step <= 6, "relief " + relief + ": " + step + " blocks in 2");
        }
    }

    @Test
    void theGroundRisesWithTheRelief() {
        double previous = height(0, 0, 0, 0);
        for (double relief = 0.1; relief <= SatisfyTerrain.MAX_RELIEF + 1e-9; relief += 0.1) {
            double now = height(relief, 0, 0, 0);
            assertTrue(now >= previous - 3, "relief " + relief + ": " + now + " after " + previous);
            previous = Math.max(previous, now);
        }
    }
}
