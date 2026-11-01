package com.orijua.overgearedmod.feature.world;

import net.minecraft.world.level.ChunkPos;

import java.util.ArrayList;
import java.util.List;

/** Chunks around a center, ring by ring, nearest first. Pure, so the search order is unit tested. */
final class ChunkSpiral {
    private ChunkSpiral() {
    }

    /** The center, then every chunk at distance 1, 2... up to {@code radius} (square rings, (2r+1)² chunks in all). */
    static List<ChunkPos> around(ChunkPos center, int radius) {
        List<ChunkPos> chunks = new ArrayList<>((2 * radius + 1) * (2 * radius + 1));
        chunks.add(center);
        for (int ring = 1; ring <= radius; ring++) {
            for (int dx = -ring; dx <= ring; dx++) {
                for (int dz = -ring; dz <= ring; dz++) {
                    if (Math.max(Math.abs(dx), Math.abs(dz)) == ring) {
                        chunks.add(new ChunkPos(center.x + dx, center.z + dz));
                    }
                }
            }
        }
        return chunks;
    }
}
