package com.orijua.overgearedmod.feature.world;

/**
 * The lore regions and the relief of the Satisfy dimension, block by block: the cells of a canon map
 * ({@link SatisfyMapData}, north up, block (0, 0) on Bairan's pixel) with their borders warped by smooth noise, so the
 * regions and the coast don't come out as squares and staircases. Beyond the map, and in its sea and lakes, the region is
 * {@link SatisfyMapData#SEA}.
 * <p>
 * The warp moves the point that is looked up by less than half a cell, so a block always gets the region of its own cell
 * or of a neighbour, and the middle of every cell keeps its own. The relief ({@link #reliefAt}) is read at the same
 * warped point, so the coast of the terrain follows the coast of the regions. Pure, so it is unit-tested.
 */
public final class SatisfyMap implements SatisfyRelief {
    /** The broad wobble: lattice spacing and reach, in cells. */
    private static final double BROAD_STEP = 2.0;
    private static final double BROAD_REACH = 0.32;
    /** The fine wobble, which breaks the staircases of diagonal borders. */
    private static final double FINE_STEP = 0.5;
    private static final double FINE_REACH = 0.13;
    /** The farthest the warp moves a point, in cells (each axis): under half a cell. */
    static final double MAX_WARP = BROAD_REACH + FINE_REACH;
    private static final long SALT_X = 0x9E3779B9L;
    private static final long SALT_Z = 0x85EBCA6BL;

    private final SatisfyMapData data;
    private final int broadStep;
    private final int fineStep;
    private final double broadReach;
    private final double fineReach;

    public SatisfyMap(SatisfyMapData data) {
        this.data = data;
        int cell = data.cellBlocks();
        this.broadStep = Math.max(1, (int) Math.round(cell * BROAD_STEP));
        this.fineStep = Math.max(1, (int) Math.round(cell * FINE_STEP));
        this.broadReach = cell * BROAD_REACH;
        this.fineReach = cell * FINE_REACH;
    }

    public SatisfyMapData data() {
        return data;
    }

    /** The region index of block (x, z) after the warp; {@link SatisfyMapData#SEA} off the map. */
    public int regionAt(int blockX, int blockZ) {
        double x = blockX + warp(blockX, blockZ, SALT_X);
        double z = blockZ + warp(blockX, blockZ, SALT_Z);
        return data.regionAtCell(data.colAtBlock((int) Math.floor(x)), data.rowAtBlock((int) Math.floor(z)));
    }

    /**
     * The relief of block (x, z), 0-9, at the same warped point as {@link #regionAt}: bilinear between the cell centres
     * ({@link SatisfyMapData#relief}), so the ground has no steps; {@code 0} (deep sea) beyond the map.
     */
    @Override
    public double reliefAt(int blockX, int blockZ) {
        return data.relief(blockX + warp(blockX, blockZ, SALT_X), blockZ + warp(blockX, blockZ, SALT_Z));
    }

    /** Whether block (x, z) is on the map's land (not its sea or a lake), after the warp. */
    public boolean isLand(int blockX, int blockZ) {
        return regionAt(blockX, blockZ) != SatisfyMapData.SEA;
    }

    /** The warp along one axis, in blocks: two octaves of smooth value noise. */
    private double warp(int x, int z, long salt) {
        return noise(x, z, salt, broadStep) * broadReach + noise(x, z, salt * 31 + 7, fineStep) * fineReach;
    }

    /** Smooth value noise in [-1, 1] on a lattice of {@code step} blocks. */
    private static double noise(int x, int z, long salt, int step) {
        int x0 = Math.floorDiv(x, step);
        int z0 = Math.floorDiv(z, step);
        double fx = smooth((x - (double) x0 * step) / step);
        double fz = smooth((z - (double) z0 * step) / step);
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
