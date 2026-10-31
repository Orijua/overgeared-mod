package com.orijua.overgearedmod.structure;

import com.mojang.serialization.MapCodec;
import com.orijua.overgearedmod.registry.ModWorldgen;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.structure.placement.RandomSpreadStructurePlacement;
import net.minecraft.world.level.levelgen.structure.placement.RandomSpreadType;
import net.minecraft.world.level.levelgen.structure.placement.StructurePlacementType;

/**
 * Where the starting village generates: only at the site {@link StartingVillage} chose for this world when it was created
 * ("type": "overgearedmod:starting_village" in a structure_set). Without a site (worlds created before it existed), nowhere.
 * <p>
 * It extends random spread only so that /locate finds it: /locate knows rings and random spread, and here every region
 * points at the same chunk. The spacing just sets how /locate steps through regions.
 */
public final class StartingVillagePlacement extends RandomSpreadStructurePlacement {
    public static final MapCodec<StartingVillagePlacement> CODEC = MapCodec.unit(StartingVillagePlacement::new);
    /** Far beyond the world border: what every region points at in a world without a site. */
    private static final ChunkPos NOWHERE = new ChunkPos(2_000_000, 2_000_000);

    public StartingVillagePlacement() {
        super(32, 8, RandomSpreadType.LINEAR, 0);
    }

    @Override
    public ChunkPos getPotentialStructureChunk(long seed, int regionX, int regionZ) {
        return StartingVillage.siteFor(seed).orElse(NOWHERE);
    }

    /** Without a site, /locate and the structure checks stop here instead of trying to generate a chunk at NOWHERE. */
    @Override
    public boolean applyAdditionalChunkRestrictions(int regionX, int regionZ, long levelSeed) {
        return StartingVillage.siteFor(levelSeed).isPresent();
    }

    @Override
    public StructurePlacementType<?> type() {
        return ModWorldgen.STARTING_VILLAGE.get();
    }
}
