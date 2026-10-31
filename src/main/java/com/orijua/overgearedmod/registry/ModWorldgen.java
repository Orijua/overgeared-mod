package com.orijua.overgearedmod.registry;

import com.orijua.overgearedmod.Overgearedmod;
import com.orijua.overgearedmod.structure.StartingVillagePlacement;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.levelgen.structure.placement.StructurePlacementType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** World generation types that structure JSON can name, such as a structure_set's "placement". */
public final class ModWorldgen {
    public static final DeferredRegister<StructurePlacementType<?>> STRUCTURE_PLACEMENTS =
            DeferredRegister.create(Registries.STRUCTURE_PLACEMENT, Overgearedmod.MODID);

    /** "type": "overgearedmod:starting_village": the one site chosen when the world was created (Bairan). */
    public static final DeferredHolder<StructurePlacementType<?>, StructurePlacementType<StartingVillagePlacement>> STARTING_VILLAGE =
            STRUCTURE_PLACEMENTS.register("starting_village", () -> () -> StartingVillagePlacement.CODEC);

    private ModWorldgen() {
    }
}
