package com.orijua.overgearedmod.feature.capsule;

import net.minecraft.core.BlockPos;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Finds a free spot to appear in (own design, not canon: so nobody appears inside a block). A spot is valid when feet
 * and head are open (see {@link Probe#isOpen}) and the floor below has collision and is not magma. The search starts
 * in the origin's column, sweeping the nearest gap first (0, +1, -1, +2, -2... up to {@value #UP} up and {@value #DOWN}
 * down), then goes through rings of radius 1 to {@value #RADIUS} (Chebyshev distance, nearest column first) with the
 * same vertical sweep. Pure: the world is read through {@link Probe}.
 */
public final class SafeSpot {
    public static final int UP = 8;
    public static final int DOWN = 8;
    public static final int RADIUS = 6;

    private SafeSpot() {
    }

    /** What the search asks of the world. */
    public interface Probe {
        /** No collision, no fluid, and nothing that hurts or traps (fire, cobweb, berry bush, powder snow, portals). */
        boolean isOpen(BlockPos pos);

        /** Has collision and is not magma. */
        boolean isSafeFloor(BlockPos pos);
    }

    public static Optional<BlockPos> find(Probe probe, BlockPos origin) {
        for (int r = 0; r <= RADIUS; r++) {
            for (int[] column : ring(r)) {
                int x = origin.getX() + column[0];
                int z = origin.getZ() + column[1];
                // The nearest gap first: 0, +1, -1, +2, -2... (up before down at the same distance).
                for (int step = 0; step <= Math.max(UP, DOWN); step++) {
                    if (step <= UP && valid(probe, new BlockPos(x, origin.getY() + step, z))) {
                        return Optional.of(new BlockPos(x, origin.getY() + step, z));
                    }
                    if (step > 0 && step <= DOWN && valid(probe, new BlockPos(x, origin.getY() - step, z))) {
                        return Optional.of(new BlockPos(x, origin.getY() - step, z));
                    }
                }
            }
        }
        return Optional.empty();
    }

    private static boolean valid(Probe probe, BlockPos p) {
        return probe.isOpen(p) && probe.isOpen(p.above()) && probe.isSafeFloor(p.below());
    }

    /** The columns {dx, dz} at Chebyshev distance {@code r}, nearest first (by dx²+dz², then dx, then dz). */
    static List<int[]> ring(int r) {
        List<int[]> columns = new ArrayList<>();
        for (int dx = -r; dx <= r; dx++) {
            for (int dz = -r; dz <= r; dz++) {
                if (Math.max(Math.abs(dx), Math.abs(dz)) == r) {
                    columns.add(new int[]{dx, dz});
                }
            }
        }
        columns.sort(Comparator.<int[]>comparingInt(c -> c[0] * c[0] + c[1] * c[1])
                .thenComparingInt(c -> c[0]).thenComparingInt(c -> c[1]));
        return columns;
    }
}
