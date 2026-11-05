package com.orijua.overgearedmod.feature.world;

/**
 * A relief map over the blocks of the Satisfy dimension, from {@code 0} (deep sea) to {@code 9} (peak), smooth between
 * cells: what a {@link SatisfyTerrainFunction} turns into the vanilla terrain parameters. The canon map
 * ({@link SatisfyMap}) and the hand-drawn test island ({@link SatisfyTerrain}) are relief maps.
 */
@FunctionalInterface
public interface SatisfyRelief {
    /** The relief at block (x, z), from 0 to {@link SatisfyTerrain#MAX_RELIEF}. */
    double reliefAt(int blockX, int blockZ);
}
