package com.orijua.overgearedmod.feature.world;

import com.orijua.overgearedmod.Overgearedmod;
import net.minecraft.resources.ResourceLocation;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The relief maps a {@code overgearedmod:satisfy_terrain} density function can read, by id: the hand-drawn
 * {@link #TEST_ISLAND}, or a canon map converted by {@code tools/satisfy_map} ({@code overgearedmod:west} is
 * {@code data/overgearedmod/satisfy_map/west/}, read with {@link SatisfyMapData} and warped like the biomes by
 * {@link SatisfyMap}).
 */
public final class SatisfyTerrains {
    /** A hand-drawn island with a mountain in the middle: the technical test of the terrain. */
    public static final ResourceLocation TEST_ISLAND = Overgearedmod.id("test_island");
    /** The West Continent of the canon map, the terrain of the Satisfy dimension. */
    public static final ResourceLocation WEST = Overgearedmod.id("west");

    private static final SatisfyTerrain ISLAND = new SatisfyTerrain(List.of("0000011100000",
            "0001233321000",
            "0023445443200",
            "0134556554310",
            "0245668665420",
            "1345689865431",
            "1356899986531",
            "1345689865431",
            "0245668665420",
            "0134556554310",
            "0023445443200",
            "0001233321000",
            "0000011100000"), 96, 6, 6);

    /** Canon maps already read, by id: the data never changes while the game runs. */
    private static final Map<ResourceLocation, SatisfyMap> MAPS = new ConcurrentHashMap<>();

    private SatisfyTerrains() {
    }

    /** The hand-drawn test island. */
    public static SatisfyTerrain testIsland() {
        return ISLAND;
    }

    /**
     * The relief map with this id: the test island, or the canon map in {@code satisfy_map/<path>/}. Fails with an
     * {@link IllegalArgumentException} if there is no such map.
     */
    public static SatisfyRelief get(ResourceLocation id) {
        if (id.equals(TEST_ISLAND)) {
            return ISLAND;
        }
        return MAPS.computeIfAbsent(id, key -> {
            try {
                return new SatisfyMap(SatisfyMapData.cached(SatisfyBiomeSource.folder(key)));
            } catch (RuntimeException e) {
                throw new IllegalArgumentException("unknown Satisfy terrain " + key + " (" + e.getMessage() + ")", e);
            }
        });
    }
}
