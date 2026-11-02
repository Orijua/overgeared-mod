package com.orijua.overgearedmod.core.registry;

import com.mojang.serialization.MapCodec;
import com.orijua.overgearedmod.Overgearedmod;
import com.orijua.overgearedmod.feature.world.SatisfyBiomeSource;
import com.orijua.overgearedmod.feature.world.WorldSitePlacement;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.levelgen.structure.placement.StructurePlacementType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** World generation types that structure JSON can name, such as a structure_set's "placement". */
public final class ModWorldgen {
    public static final DeferredRegister<StructurePlacementType<?>> STRUCTURE_PLACEMENTS =
            DeferredRegister.create(Registries.STRUCTURE_PLACEMENT, Overgearedmod.MODID);

    /** "type": "overgearedmod:world_site", "site": "...": the one site chosen when the world was created (Bairan, the cave). */
    public static final DeferredHolder<StructurePlacementType<?>, StructurePlacementType<WorldSitePlacement>> WORLD_SITE =
            STRUCTURE_PLACEMENTS.register("world_site", () -> () -> WorldSitePlacement.CODEC);

    public static final DeferredRegister<MapCodec<? extends BiomeSource>> BIOME_SOURCES =
            DeferredRegister.create(Registries.BIOME_SOURCE, Overgearedmod.MODID);

    /** "type": "overgearedmod:satisfy_map": the lore regions of the Satisfy dimension over vanilla terrain. */
    public static final DeferredHolder<MapCodec<? extends BiomeSource>, MapCodec<SatisfyBiomeSource>> SATISFY_MAP =
            BIOME_SOURCES.register("satisfy_map", () -> SatisfyBiomeSource.CODEC);

    private ModWorldgen() {
    }
}
