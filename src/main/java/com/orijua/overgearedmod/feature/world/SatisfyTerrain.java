package com.orijua.overgearedmod.feature.world;

import java.util.List;

/**
 * A hand-drawn relief map (the technical test of the terrain, {@link SatisfyTerrains#TEST_ISLAND}): a grid of digits
 * {@code 0-9}, one per {@code cell} blocks, north up ({@code 0} deep sea, {@code 3} coast, {@code 5} lowland, {@code 9} mountain peak). The relief at a
 * block is interpolated bilinearly between cell centres, so the land has no steps; beyond the grid it is {@code 0}
 * (sea). The middle of cell ({@code originCol}, {@code originRow}) is block (0, 0). The real terrain reads the canon map
 * instead ({@link SatisfyMap}), with the same meaning of the digits.
 * <p>
 * {@link Channel} turns any relief into the vanilla climate parameters (continentalness, erosion, ridges) that the
 * vanilla offset/factor splines turn into a height. Pure, so it is unit-tested.
 */
public record SatisfyTerrain(List<String> rows, int cell, int originCol, int originRow) implements SatisfyRelief {
    public static final double MAX_RELIEF = 9.0;

    public SatisfyTerrain {
        rows = List.copyOf(rows);
        if (cell <= 0) {
            throw new IllegalArgumentException("cell must be positive: " + cell);
        }
        if (rows.isEmpty()) {
            throw new IllegalArgumentException("the terrain has no rows");
        }
        int width = rows.get(0).length();
        for (String row : rows) {
            if (row.length() != width) {
                throw new IllegalArgumentException("ragged terrain rows: expected " + width + " columns but found " + row.length());
            }
            for (char c : row.toCharArray()) {
                if (c < '0' || c > '9') {
                    throw new IllegalArgumentException("relief digits are 0-9: " + c);
                }
            }
        }
        if (originRow < 0 || originRow >= rows.size() || originCol < 0 || originCol >= width) {
            throw new IllegalArgumentException("the origin (" + originCol + ", " + originRow + ") is off the terrain");
        }
    }

    /** The relief at a block, from 0 (deep sea) to 9 (peak). */
    public double relief(double blockX, double blockZ) {
        double u = blockX / cell + originCol;
        double v = blockZ / cell + originRow;
        int u0 = (int) Math.floor(u);
        int v0 = (int) Math.floor(v);
        double fu = u - u0;
        double fv = v - v0;
        double top = lerp(cellRelief(u0, v0), cellRelief(u0 + 1, v0), fu);
        double bottom = lerp(cellRelief(u0, v0 + 1), cellRelief(u0 + 1, v0 + 1), fu);
        return lerp(top, bottom, fv);
    }

    @Override
    public double reliefAt(int blockX, int blockZ) {
        return relief(blockX, (double) blockZ);
    }

    private int cellRelief(int col, int row) {
        if (row < 0 || row >= rows.size()) {
            return 0;
        }
        String line = rows.get(row);
        return col < 0 || col >= line.length() ? 0 : line.charAt(col) - '0';
    }

    private static double lerp(double a, double b, double t) {
        return a + (b - a) * t;
    }

    /**
     * The climate parameter a density function feeds the vanilla terrain splines, as a function of the relief, plus a
     * small share of vanilla's own noise for that parameter so coasts and ranges are not smooth. Tuned on the vanilla
     * 1.21.1 splines (sea level 63): relief {@code 0} is a deep sea (floor ~35), {@code 1-2} a shallower sea (~37-48),
     * the shore falls just past {@code 2.5} (the border of a coast cell), {@code 3} is a low coast (~64-67), {@code 4-5}
     * lowland (~67-80), {@code 6-7} hills (~90-130) and {@code 8-9} mountains (~150-230). The ridges stay well away from
     * 0, so the vanilla terrain doesn't carve random rivers.
     */
    public enum Channel implements net.minecraft.util.StringRepresentable {
        CONTINENTS("continents",
                new double[][]{{0, -1.0}, {1, -0.5}, {2, -0.2}, {3, -0.11}, {4, -0.02}, {5, 0.08}, {6, 0.2}, {7, 0.35}, {8, 0.55}, {9, 0.8}},
                new double[][]{{0, 0.03}, {9, 0.03}}),
        EROSION("erosion",
                new double[][]{{0, 0.3}, {3, 0.3}, {4, 0.27}, {5, 0.22}, {6, 0.0}, {7, -0.3}, {8, -0.6}, {9, -0.85}},
                new double[][]{{0, 0.03}, {5, 0.03}, {7, 0.08}, {9, 0.1}}),
        RIDGES("ridges",
                new double[][]{{0, 0.42}, {5, 0.42}, {6, 0.5}, {7, 0.58}, {9, 0.62}},
                new double[][]{{0, 0.04}, {5, 0.04}, {6, 0.1}, {7, 0.2}, {9, 0.28}});

        private final String serializedName;
        private final double[][] base;
        private final double[][] noise;
        private final double min;
        private final double max;
        private final double noisyMin;
        private final double noisyMax;

        Channel(String serializedName, double[][] base, double[][] noise) {
            this.serializedName = serializedName;
            this.base = base;
            this.noise = noise;
            // Both curves are piecewise linear, so their extremes are at the breakpoints of either.
            double min = Double.POSITIVE_INFINITY;
            double max = Double.NEGATIVE_INFINITY;
            double noisyMin = Double.POSITIVE_INFINITY;
            double noisyMax = Double.NEGATIVE_INFINITY;
            for (double[][] curve : new double[][][]{base, noise}) {
                for (double[] point : curve) {
                    double value = piecewise(point[0], base);
                    double weight = piecewise(point[0], noise);
                    min = Math.min(min, value);
                    max = Math.max(max, value);
                    noisyMin = Math.min(noisyMin, value - weight);
                    noisyMax = Math.max(noisyMax, value + weight);
                }
            }
            this.min = min;
            this.max = max;
            this.noisyMin = noisyMin;
            this.noisyMax = noisyMax;
        }

        /** The channel's value for a relief, without noise. */
        public double of(double relief) {
            return piecewise(relief, base);
        }

        /** How much of the vanilla noise is added at a relief (the noise is clamped to [-1, 1]). */
        public double noiseWeight(double relief) {
            return piecewise(relief, noise);
        }

        /** The channel's value for a relief, with a sample of the vanilla noise for it. */
        public double of(double relief, double noiseSample) {
            return of(relief) + noiseWeight(relief) * Math.max(-1.0, Math.min(1.0, noiseSample));
        }

        /** The lowest value without noise. */
        public double min() {
            return min;
        }

        /** The highest value without noise. */
        public double max() {
            return max;
        }

        /** The lowest value with noise. */
        public double noisyMin() {
            return noisyMin;
        }

        /** The highest value with noise. */
        public double noisyMax() {
            return noisyMax;
        }

        @Override
        public String getSerializedName() {
            return serializedName;
        }

        static double piecewise(double x, double[][] points) {
            if (x <= points[0][0]) {
                return points[0][1];
            }
            for (int i = 1; i < points.length; i++) {
                if (x <= points[i][0]) {
                    double t = (x - points[i - 1][0]) / (points[i][0] - points[i - 1][0]);
                    return lerp(points[i - 1][1], points[i][1], t);
                }
            }
            return points[points.length - 1][1];
        }
    }
}
