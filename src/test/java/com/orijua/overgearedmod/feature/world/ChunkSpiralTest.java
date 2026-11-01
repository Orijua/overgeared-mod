package com.orijua.overgearedmod.feature.world;

import net.minecraft.world.level.ChunkPos;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ChunkSpiralTest {
    @Test
    void startsAtTheCenterAndGoesOutRingByRing() {
        ChunkPos center = new ChunkPos(5, -3);
        List<ChunkPos> chunks = ChunkSpiral.around(center, 3);
        assertEquals(center, chunks.getFirst());
        int lastRing = 0;
        for (ChunkPos chunk : chunks) {
            int ring = Math.max(Math.abs(chunk.x - center.x), Math.abs(chunk.z - center.z));
            assertTrue(ring >= lastRing, "never goes back to a nearer ring: " + chunk);
            lastRing = ring;
        }
    }

    @Test
    void coversTheWholeSquareOnce() {
        List<ChunkPos> chunks = ChunkSpiral.around(new ChunkPos(0, 0), 4);
        assertEquals(81, chunks.size(), "(2·4 + 1)²");
        assertEquals(81, new HashSet<>(chunks).size(), "no chunk twice");
        assertEquals(List.of(new ChunkPos(0, 0)), ChunkSpiral.around(new ChunkPos(0, 0), 0));
    }
}
