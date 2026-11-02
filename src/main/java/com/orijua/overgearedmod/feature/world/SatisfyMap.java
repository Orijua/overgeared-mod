package com.orijua.overgearedmod.feature.world;

import java.util.List;

/**
 * The lore map of the Satisfy dimension: a grid of letters (one per {@code cell} blocks, north up) painted from the
 * canon map of the West Continent. {@code '.'} means "no lore here": the land beyond the map is ordinary Minecraft.
 * <p>
 * The block (0, 0) of the dimension is the middle of the cell ({@code originCol}, {@code originRow}), which is where
 * Bairan is. Borders are warped with smooth noise so the regions don't come out as squares. Pure, so it is unit-tested.
 */
public record SatisfyMap(List<String> rows, int cell, int originCol, int originRow) {
    public static final char NONE = '.';
    /** The warp's lattice spacing, in blocks. */
    private static final int WARP_STEP = 64;

    public SatisfyMap {
        rows = List.copyOf(rows);
        if (cell <= 0) {
            throw new IllegalArgumentException("cell must be positive: " + cell);
        }
        if (rows.isEmpty()) {
            throw new IllegalArgumentException("the map has no rows");
        }
        int width = rows.get(0).length();
        for (String row : rows) {
            if (row.length() != width) {
                throw new IllegalArgumentException("ragged map rows: expected " + width + " columns but found " + row.length());
            }
        }
        if (originRow < 0 || originRow >= rows.size() || originCol < 0 || originCol >= width) {
            throw new IllegalArgumentException("the origin (" + originCol + ", " + originRow + ") is off the map");
        }
    }

    /** The letter of the region that block (x, z) belongs to, or {@link #NONE} off the map. */
    public char at(int blockX, int blockZ) {
        double warpX = warp(blockX, blockZ, 0x9E3779B9L) * cell / 3.0;
        double warpZ = warp(blockX, blockZ, 0x85EBCA6BL) * cell / 3.0;
        int col = Math.floorDiv((int) Math.floor(blockX + warpX) + cell / 2, cell) + originCol;
        int row = Math.floorDiv((int) Math.floor(blockZ + warpZ) + cell / 2, cell) + originRow;
        if (row < 0 || row >= rows.size()) {
            return NONE;
        }
        String line = rows.get(row);
        return col < 0 || col >= line.length() ? NONE : line.charAt(col);
    }

    /** Smooth value noise in [-1, 1]. */
    private static double warp(int x, int z, long salt) {
        int x0 = Math.floorDiv(x, WARP_STEP);
        int z0 = Math.floorDiv(z, WARP_STEP);
        double fx = smooth((x - x0 * WARP_STEP) / (double) WARP_STEP);
        double fz = smooth((z - z0 * WARP_STEP) / (double) WARP_STEP);
        double top = lerp(corner(x0, z0, salt), corner(x0 + 1, z0, salt), fx);
        double bottom = lerp(corner(x0, z0 + 1, salt), corner(x0 + 1, z0 + 1, salt), fx);
        return lerp(top, bottom, fz);
    }

    private static double corner(int x, int z, long salt) {
        long h = x * 0x632BE59BD9B4E019L ^ z * 0x8CB92BA72F3D8DD7L ^ salt;
        h ^= h >>> 33;
        h *= 0xFF51AFD7ED558CCDL;
        h ^= h >>> 33;
        return ((h & 0xFFFF) / 32767.5) - 1.0;
    }

    private static double smooth(double t) {
        return t * t * (3 - 2 * t);
    }

    private static double lerp(double a, double b, double t) {
        return a + (b - a) * t;
    }
}
