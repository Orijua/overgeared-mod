package com.orijua.overgearedmod.registry;

import com.orijua.overgearedmod.Overgearedmod;
import com.orijua.overgearedmod.smelting.SmelterBlockEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Blocks that keep state of their own (items, heat) between ticks and saves. */
public final class ModBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, Overgearedmod.MODID);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<SmelterBlockEntity>> SMELTER =
            BLOCK_ENTITIES.register("smelter", () -> BlockEntityType.Builder.of(SmelterBlockEntity::new, ModBlocks.SMELTER.get()).build(null));

    private ModBlockEntities() {
    }
}
