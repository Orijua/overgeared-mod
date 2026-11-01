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
        handheldItem(ModItems.FRACASO_SWORD.get()); // the user's 32x32 PNG

        // Placeholders that reuse a vanilla texture until there is an own PNG.
        vanillaTexture(ModItems.ENHANCEMENT_STONE.get(), "amethyst_shard");
        vanillaTexture(ModItems.RESPEC_SCROLL.get(), "paper");
        vanillaTexture(ModItems.SKILL_BOOK.get(), "writable_book");
        vanillaTexture(ModItems.FRACASO_BLUEPRINT.get(), "map");
        vanillaTexture(ModItems.UNAPPRAISED_BOOK.get(), "book");
        vanillaTexture(ModItems.EYE_OF_PALLALIAN.get(), "ender_eye");
        vanillaTexture(ModItems.PAGMA_RARE_BOOK.get(), "enchanted_book");
        vanillaTexture(ModItems.APPRENTICE_CONTRACT.get(), "paper");
        vanillaTexture(ModItems.WORKSHOP_LEDGER.get(), "writable_book");
        // Borrowed until the hammers get their own art.
        vanillaTexture(ModItems.IRON_SMITHING_HAMMER.get(), "mace");
        vanillaTexture(ModItems.JAFFA_SMITHING_HAMMER.get(), "mace");
        vanillaTexture(ModItems.MITHRIL_SMITHING_HAMMER.get(), "mace");
        vanillaTexture(ModItems.RAW_JAFFA.get(), "raw_copper");
        vanillaTexture(ModItems.JAFFA_INGOT.get(), "copper_ingot");
        vanillaTexture(ModItems.RAW_BLUE_ORICHALCUM.get(), "prismarine_crystals");
        vanillaTexture(ModItems.BLUE_ORICHALCUM_INGOT.get(), "prismarine_shard");

        withExistingParent(ModItems.BLACKSMITH_SPAWN_EGG.getId().getPath(), mcLoc("item/template_spawn_egg"));
        withExistingParent(ModItems.VILLAGE_CHIEF_SPAWN_EGG.getId().getPath(), mcLoc("item/template_spawn_egg"));
        withExistingParent(ModItems.BONE_LORD_SPAWN_EGG.getId().getPath(), mcLoc("item/template_spawn_egg"));
    }

    private void vanillaTexture(Item item, String vanillaTexture) {
        withExistingParent(BuiltInRegistries.ITEM.getKey(item).getPath(), mcLoc("item/generated"))
                .texture("layer0", mcLoc("item/" + vanillaTexture));
    }
}
