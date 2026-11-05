package com.orijua.overgearedmod.feature.world;

import java.util.List;

/**
 * A relief map that drives the terrain of the Satisfy dimension: a grid of digits {@code 0-9}, one per {@code cell}
 * blocks, north up ({@code 0} deep sea, {@code 3} coast, {@code 5} lowland, {@code 9} mountain peak). The relief at a
 * block is interpolated bilinearly between cell centres, so the land has no steps; beyond the grid it is {@code 0}
 * (sea). The middle of cell ({@code originCol}, {@code originRow}) is block (0, 0).
 * <p>
 * {@link Channel} turns the relief into the vanilla climate parameters (continentalness, erosion, ridges) that the
 * vanilla offset/factor splines turn into a height. Pure, so it is unit-tested.
 */
public record SatisfyTerrain(List<String> rows, int cell, int originCol, int originRow) {
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

    /** The climate parameter a density function feeds the vanilla terrain splines. */
    public enum Channel implements net.minecraft.util.StringRepresentable {
        CONTINENTS("continents", -1.0, 0.7) {
            @Override
            public double of(double relief) {
                return piecewise(relief, new double[][]{{0, -1.0}, {2, -0.6}, {3.5, -0.15}, {5, 0.35}, {9, 0.7}});
            }
        },
        EROSION("erosion", -0.8, 0.5) {
            @Override
            public double of(double relief) {
                return piecewise(relief, new double[][]{{0, 0.5}, {5, 0.5}, {8, -0.8}, {9, -0.8}});
            }
        },
        /** Constant, away from 0 so the vanilla terrain doesn't carve random rivers. */
        RIDGES("ridges", 0.45, 0.45) {
            @Override
            public double of(double relief) {
                return 0.45;
            }
        };

        private final String serializedName;
        private final double min;
        private final double max;

        Channel(String serializedName, double min, double max) {
            this.serializedName = serializedName;
            this.min = min;
            this.max = max;
        }

        /** The channel's value for a relief. */
        public abstract double of(double relief);

        public double min() {
            return min;
        }

        public double max() {
            return max;
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
