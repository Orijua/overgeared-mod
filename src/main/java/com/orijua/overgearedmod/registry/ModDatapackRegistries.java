package com.orijua.overgearedmod.registry;

import com.orijua.overgearedmod.Overgearedmod;
import com.orijua.overgearedmod.equipment.option.ItemOptionDefinition;
import com.orijua.overgearedmod.skill.SkillDefinition;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.neoforged.neoforge.registries.DataPackRegistryEvent;

/**
 * Content defined in JSON. Each registry reads data/<namespace>/overgearedmod/<name>/<id>.json,
 * e.g. data/overgearedmod/overgearedmod/skill/pagma_link.json, and is synced to clients for the UI.
 */
public final class ModDatapackRegistries {
    public static final ResourceKey<Registry<SkillDefinition>> SKILL = key("skill");
    public static final ResourceKey<Registry<ItemOptionDefinition>> ITEM_OPTION = key("item_option");

    private ModDatapackRegistries() {
    }

    public static void register(DataPackRegistryEvent.NewRegistry event) {
        event.dataPackRegistry(SKILL, SkillDefinition.CODEC, SkillDefinition.CODEC);
        event.dataPackRegistry(ITEM_OPTION, ItemOptionDefinition.CODEC, ItemOptionDefinition.CODEC);
    }

    private static <T> ResourceKey<Registry<T>> key(String name) {
        return ResourceKey.createRegistryKey(Overgearedmod.id(name));
    }
}
