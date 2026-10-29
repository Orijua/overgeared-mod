package com.orijua.overgearedmod.registry;

import com.orijua.overgearedmod.Overgearedmod;
import com.orijua.overgearedmod.equipment.ItemRating;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Typed data stored on an ItemStack: saved with the item ("persistent") and sent to clients ("networkSynchronized"). */
public final class ModDataComponents {
    public static final DeferredRegister.DataComponents DATA_COMPONENTS =
            DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, Overgearedmod.MODID);

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<ItemRating>> ITEM_RATING =
            DATA_COMPONENTS.registerComponentType("item_rating", builder -> builder
                    .persistent(ItemRating.CODEC)
                    .networkSynchronized(ItemRating.STREAM_CODEC));

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<ResourceLocation>> SKILL_BOOK =
            DATA_COMPONENTS.registerComponentType("skill_book", builder -> builder
                    .persistent(ResourceLocation.CODEC)
                    .networkSynchronized(ResourceLocation.STREAM_CODEC));

    private ModDataComponents() {
    }
}
