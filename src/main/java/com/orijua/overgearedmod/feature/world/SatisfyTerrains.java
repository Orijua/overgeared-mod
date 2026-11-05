package com.orijua.overgearedmod.feature.world;

import com.orijua.overgearedmod.Overgearedmod;
import net.minecraft.resources.ResourceLocation;

import java.util.List;
import java.util.Map;

/** The named relief maps a {@code overgearedmod:satisfy_terrain} density function can read. */
public final class SatisfyTerrains {
    /** A hand-drawn island with a mountain in the middle: the technical test of the terrain. */
    public static final ResourceLocation TEST_ISLAND = Overgearedmod.id("test_island");

    private static final Map<ResourceLocation, SatisfyTerrain> BY_ID = Map.of(
            TEST_ISLAND, new SatisfyTerrain(List.of("0000011100000",
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
                    "0000011100000"), 96, 6, 6));

    private SatisfyTerrains() {
    }

    public static SatisfyTerrain get(ResourceLocation id) {
        SatisfyTerrain terrain = BY_ID.get(id);
        if (terrain == null) {
            throw new IllegalArgumentException("unknown Satisfy terrain " + id + ", known: " + BY_ID.keySet());
        }
        return terrain;
    }
}
