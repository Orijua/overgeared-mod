package com.orijua.overgearedmod.feature.world;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.RandomState;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The one-per-world structures' sites of each running world, by seed and site name: {@link WorldSitePlacement} only knows
 * the seed. A new world gets the fixed sites of {@code sites.json} ({@link SatisfySites}, the same in every seed) when it
 * is created ({@link StartingVillage}) and saves them ({@link WorldSitesData}); an older world keeps the ones its search
 * gave it.
 * All of them are in the Satisfy dimension; the other dimensions are {@linkplain #exclude excluded}.
 */
public final class WorldSites {
    /** Bairan, the starting village. */
    public static final String BAIRAN = "bairan";
    /** The Northern End Cave, with Pagma's rare book. */
    public static final String NORTH_END_CAVE = "north_end_cave";
    /** Earl Ashur's manor, near Bairan. */
    public static final String ASHUR_MANOR = "ashur_manor";
    /** The district of Winston with Khan's smithy. */
    public static final String WINSTON = "winston";

    private static final Map<Long, Map<String, ChunkPos>> SITES = new ConcurrentHashMap<>();
    /** Where a player lands in Bairan (on its plaza), by seed. */
    private static final Map<Long, BlockPos> LANDINGS = new ConcurrentHashMap<>();
    /** The generators of the dimensions that are not Satisfy; they never place a site. Weak: a generator may go away. */
    private static final Set<RandomState> EXCLUDED = Collections.synchronizedSet(Collections.newSetFromMap(new WeakHashMap<>()));

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

    public static Optional<BlockPos> landingFor(long seed) {
        return Optional.ofNullable(LANDINGS.get(seed));
    }

    public static void rememberLanding(long seed, BlockPos landing) {
        LANDINGS.put(seed, landing);
    }

    public static void forgetLanding(long seed) {
        LANDINGS.remove(seed);
    }

    /** The overworld, Nether and End are the real world: their generators (by random state) place no site. */
    public static void exclude(RandomState generator) {
        EXCLUDED.add(generator);
    }

    static boolean isExcluded(RandomState generator) {
        return EXCLUDED.contains(generator);
    }

    static void clear() {
        SITES.clear();
        LANDINGS.clear();
        EXCLUDED.clear();
    }
}
