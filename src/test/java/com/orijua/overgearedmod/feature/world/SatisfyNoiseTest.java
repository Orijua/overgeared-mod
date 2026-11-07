package com.orijua.overgearedmod.feature.world;

import org.junit.jupiter.api.Test;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The fixed-seed detail of the Satisfy terrain ({@link SatisfyNoise}) and what the {@code satisfy_terrain} density function
 * computes with it. The function itself (its codec, {@code mapAll}) needs a bootstrapped game: the gametests
 * ({@code SatisfyFixedWorldTests}) check it.
 */
class SatisfyNoiseTest {
    private static final SatisfyTerrain.Channel[] CHANNELS = SatisfyTerrain.Channel.values();

    /**
     * Samples taken once (2026-11-07, Minecraft 1.21.1) and kept: the noise must give exactly these in every run, every
     * world and every machine. If they change, every new world's coast changes too.
     */
    private static final int[][] POINTS = {{0, 0}, {137, -2045}, {-3500, 4100}, {4600, -880}, {-1864, -4}};
    private static final double[][] EXPECTED = {
            {-0.18948785606391177, 0.4507007603080412, -0.7718130697773563, -0.5600361735968787, 0.2736001774436192},
            {-0.08650954854038867, -0.4978079215559065, 0.3885519514305833, 0.027574799990995786, 0.5654774176813289},
            {0.025949825580232466, 0.15808862843440197, -0.8159651756284156, -0.32583009592121537, -0.1455572002282078}};

    @Test
    void theSamplesNeverChange() {
        StringBuilder actual = new StringBuilder();
        boolean same = true;
        for (int c = 0; c < CHANNELS.length; c++) {
            actual.append('{');
            for (int p = 0; p < POINTS.length; p++) {
                double value = SatisfyNoise.sample(CHANNELS[c], POINTS[p][0], POINTS[p][1]);
                actual.append(value).append(p + 1 < POINTS.length ? ", " : "");
                same &= value == EXPECTED[c][p];
            }
            actual.append("},\n");
        }
        assertTrue(same, "the fixed-seed noise changed; it is now:\n" + actual);
    }

    @Test
    void theSameBlockAlwaysGivesTheSameValueOnAnyThread() throws Exception {
        double[] first = grid();
        ExecutorService pool = Executors.newFixedThreadPool(4);
        try {
            Future<?>[] runs = new Future<?>[4];
            for (int i = 0; i < runs.length; i++) {
                runs[i] = pool.submit(() -> {
                    double[] again = grid();
                    for (int j = 0; j < first.length; j++) {
                        assertEquals(first[j], again[j], 0.0, "sample " + j);
                    }
                });
            }
            for (Future<?> run : runs) {
                run.get(30, TimeUnit.SECONDS);
            }
        } finally {
            pool.shutdownNow();
        }
    }

    private static double[] grid() {
        double[] values = new double[3 * 41 * 41];
        int i = 0;
        for (SatisfyTerrain.Channel channel : CHANNELS) {
            for (int x = -4000; x <= 4000; x += 200) {
                for (int z = -1000; z <= 7000; z += 200) {
                    values[i++] = SatisfyNoise.sample(channel, x, z);
                }
            }
        }
        return values;
    }

    /**
     * In range, and like vanilla's: mostly in [-1, 1], with a spread (standard deviation) of a few tenths over the map,
     * neither flat nor saturated. Measured over 6561 points of the map (2026-11-07): continents mean -0.11, sd 0.39, 152
     * outside [-1, 1]; erosion 0.12, 0.27, none; ridges -0.01, 0.35, 18 (bounds 7.3, 4.8 and 5.7: never reached).
     */
    @Test
    void theSamplesAreInRangeAndVaried() {
        for (SatisfyTerrain.Channel channel : CHANNELS) {
            double max = SatisfyNoise.sampleBound(channel);
            assertTrue(max > 1 && max < 10, channel + " bound " + max);
            double sum = 0;
            double squares = 0;
            int outside = 0;
            int n = 0;
            for (int x = -3500; x <= 4600; x += 101) {
                for (int z = -880; z <= 5200; z += 76) {
                    double value = SatisfyNoise.sample(channel, x, z);
                    assertTrue(Math.abs(value) <= max, channel + " at " + x + "," + z + ": " + value + " over " + max);
                    sum += value;
                    squares += value * value;
                    outside += Math.abs(value) > 1 ? 1 : 0;
                    n++;
                }
            }
            double mean = sum / n;
            double sd = Math.sqrt(squares / n - mean * mean);
            System.out.printf("%s: mean %.3f, sd %.3f, |v|>1 %d of %d, bound %.3f%n", channel, mean, sd, outside, n, max);
            assertTrue(Math.abs(mean) < 0.3, channel + " mean " + mean);
            assertTrue(sd > 0.1 && sd < 0.6, channel + " sd " + sd);
            assertTrue(outside < n / 20, channel + ": " + outside + " of " + n + " samples outside [-1, 1]");
        }
    }

    /** What the density function computes stays inside its declared bounds, reaches near them and is the same at every y. */
    @Test
    void theClimateStaysWithinItsMinAndMaxValue() {
        SatisfyRelief west = new SatisfyMap(SatisfyMapData.west()); // what SatisfyTerrains.get(WEST) gives, without the game
        for (SatisfyTerrain.Channel channel : CHANNELS) {
            assertEquals(channel.noisyMin(), SatisfyNoise.minValue(channel, true), 0.0);
            assertEquals(channel.noisyMax(), SatisfyNoise.maxValue(channel, true), 0.0);
            assertEquals(channel.min(), SatisfyNoise.minValue(channel, false), 0.0);
            assertEquals(channel.max(), SatisfyNoise.maxValue(channel, false), 0.0);
            double lowest = Double.POSITIVE_INFINITY;
            double highest = Double.NEGATIVE_INFINITY;
            for (int x = -3600; x <= 4700; x += 37) {
                for (int z = -1000; z <= 5300; z += 41) {
                    double value = SatisfyNoise.climate(channel, west, true, x, z);
                    assertTrue(value >= SatisfyNoise.minValue(channel, true) && value <= SatisfyNoise.maxValue(channel, true),
                            channel + " at " + x + "," + z + ": " + value);
                    assertEquals(value, SatisfyNoise.climate(channel, west, true, x, z), 0.0, "the same every time");
                    double without = SatisfyNoise.climate(channel, west, false, x, z);
                    double weight = channel.noiseWeight(west.reliefAt(x, z));
                    assertTrue(Math.abs(value - without) <= weight + 1e-12, "the detail is a small share at " + x + "," + z);
                    lowest = Math.min(lowest, value);
                    highest = Math.max(highest, value);
                }
            }
            assertTrue(lowest < channel.min() + 0.05 && highest > channel.max() - 0.15,
                    channel + " spans its range: " + lowest + ".." + highest + " of " + channel.min() + ".." + channel.max());
        }
    }
}
