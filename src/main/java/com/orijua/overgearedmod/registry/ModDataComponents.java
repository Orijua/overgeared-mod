package com.orijua.overgearedmod.registry;

import com.orijua.overgearedmod.Overgearedmod;
import com.orijua.overgearedmod.equipment.ItemRating;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Data components = typed data stored on an ItemStack (the 1.20.5+ replacement for item NBT). */
public final class ModDataComponents {
    public static final DeferredRegister.DataComponents DATA_COMPONENTS =
            DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, Overgearedmod.MODID);

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<ItemRating>> ITEM_RATING =
            DATA_COMPONENTS.registerComponentType("item_rating", builder -> builder
                    .persistent(ItemRating.CODEC)                  // saved with the item
                    .networkSynchronized(ItemRating.STREAM_CODEC)); // sent to clients (tooltip)

    private ModDataComponents() {
    }
}
