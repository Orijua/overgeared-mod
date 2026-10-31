package com.orijua.overgearedmod.datagen;

import com.orijua.overgearedmod.Overgearedmod;
import com.orijua.overgearedmod.registry.ModItems;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.PackOutput;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.client.model.generators.ItemModelProvider;
import net.neoforged.neoforge.common.data.ExistingFileHelper;

/** Item models for plain items (block items come from ModBlockStateProvider). */
final class ModItemModelProvider extends ItemModelProvider {
    ModItemModelProvider(PackOutput output, ExistingFileHelper files) {
        super(output, Overgearedmod.MODID, files);
    }

    @Override
    protected void registerModels() {
        // Own textures: textures/item/<id>.png.
        basicItem(ModItems.RAW_MITHRIL.get());
        basicItem(ModItems.MITHRIL_INGOT.get());
        handheldItem(ModItems.JAFFA_SWORD.get());
        handheldItem(ModItems.GRID_LEGENDARY_SWORD.get());

        // Placeholders that reuse a vanilla texture until there is an own PNG.
        vanillaTexture(ModItems.ENHANCEMENT_STONE.get(), "amethyst_shard");
        vanillaTexture(ModItems.RESPEC_SCROLL.get(), "paper");
        vanillaTexture(ModItems.SKILL_BOOK.get(), "writable_book");

        withExistingParent(ModItems.BLACKSMITH_SPAWN_EGG.getId().getPath(), mcLoc("item/template_spawn_egg"));
        withExistingParent(ModItems.VILLAGE_CHIEF_SPAWN_EGG.getId().getPath(), mcLoc("item/template_spawn_egg"));
        withExistingParent(ModItems.BONE_LORD_SPAWN_EGG.getId().getPath(), mcLoc("item/template_spawn_egg"));
    }

    private void vanillaTexture(Item item, String vanillaTexture) {
        withExistingParent(BuiltInRegistries.ITEM.getKey(item).getPath(), mcLoc("item/generated"))
                .texture("layer0", mcLoc("item/" + vanillaTexture));
    }
}
