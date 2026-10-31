package com.orijua.overgearedmod.structure;

import net.minecraft.world.level.ChunkPos;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The one-per-world structures' sites of each running world, by seed and site name: {@link WorldSitePlacement} only knows
 * the seed. They are chosen when the world is created ({@link StartingVillage}) and saved with it ({@link WorldSitesData}).
 */
public final class WorldSites {
    /** Bairan, the starting village. */
    public static final String BAIRAN = "bairan";
    /** The Northern End Cave, with Pagma's rare book. */
    public static final String NORTH_END_CAVE = "north_end_cave";

    private static final Map<Long, Map<String, ChunkPos>> SITES = new ConcurrentHashMap<>();

    private WorldSites() {
    }

    public static Optional<ChunkPos> siteFor(long seed, String site) {
        return Optional.ofNullable(SITES.getOrDefault(seed, Map.of()).get(site));
    }

    /** Also for tests: what a world with this seed would have saved. */
    public static void remember(long seed, String site, ChunkPos chunk) {
        SITES.computeIfAbsent(seed, key -> new ConcurrentHashMap<>()).put(site, chunk);
    }

    public static void forget(long seed, String site) {
        Map<String, ChunkPos> sites = SITES.get(seed);
        if (sites != null) {
            sites.remove(site);
        }
    }

    static void clear() {
        SITES.clear();
    }
}
