package com.orijua.overgearedmod.feature.world;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.orijua.overgearedmod.core.registry.ModWorldgen;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.ChunkGeneratorStructureState;
import net.minecraft.world.level.levelgen.structure.placement.RandomSpreadStructurePlacement;
import net.minecraft.world.level.levelgen.structure.placement.RandomSpreadType;
import net.minecraft.world.level.levelgen.structure.placement.StructurePlacementType;

/**
 * Where a one-per-world structure generates: only at the site chosen for this world when it was created
 * ({"type": "overgearedmod:world_site", "site": "bairan"} in a structure_set; see {@link WorldSites}). Without a site
 * (worlds created before it existed), nowhere.
 * <p>
 * It extends random spread only so that /locate finds it: /locate knows rings and random spread, and here every region
 * points at the same chunk. The spacing just sets how /locate steps through regions.
 */
public final class WorldSitePlacement extends RandomSpreadStructurePlacement {
    public static final MapCodec<WorldSitePlacement> CODEC = Codec.STRING.fieldOf("site").xmap(WorldSitePlacement::new, WorldSitePlacement::site);
    /** Far beyond the world border: what every region points at in a world without the site. */
    private static final ChunkPos NOWHERE = new ChunkPos(2_000_000, 2_000_000);

    private final String site;

    public WorldSitePlacement(String site) {
        super(32, 8, RandomSpreadType.LINEAR, 0);
        this.site = site;
    }

    public String site() {
        return site;
    }

    @Override
    public ChunkPos getPotentialStructureChunk(long seed, int regionX, int regionZ) {
        return WorldSites.siteFor(seed, site).orElse(NOWHERE);
    }

    /** Without a site, /locate and the structure checks stop here instead of trying to generate a chunk at NOWHERE. */
    @Override
    public boolean applyAdditionalChunkRestrictions(int regionX, int regionZ, long levelSeed) {
        return WorldSites.siteFor(levelSeed, site).isPresent();
    }

    /** Only the Satisfy dimension places sites: the real world (the overworld) has the same seed but none of them. */
    @Override
    public boolean isStructureChunk(ChunkGeneratorStructureState structureState, int x, int z) {
        return !WorldSites.isExcluded(structureState.randomState()) && super.isStructureChunk(structureState, x, z);
    }

    @Override
    public StructurePlacementType<?> type() {
        return ModWorldgen.WORLD_SITE.get();
    }
}
