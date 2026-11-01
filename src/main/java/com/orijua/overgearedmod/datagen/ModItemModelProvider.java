package com.orijua.overgearedmod.datagen;

import com.orijua.overgearedmod.Overgearedmod;
import com.orijua.overgearedmod.core.registry.ModItems;
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
        // From the user's art pack (docs/Recursos_Overgeared_MasterPack), 32x32.
        basicItem(ModItems.RESPEC_SCROLL.get());
        basicItem(ModItems.PAGMA_RARE_BOOK.get());
        basicItem(ModItems.JAFFA_INGOT.get());
        basicItem(ModItems.RAW_BLUE_ORICHALCUM.get());
        handheldItem(ModItems.IRON_SMITHING_HAMMER.get());
        handheldItem(ModItems.MITHRIL_SMITHING_HAMMER.get());

        // Placeholders that reuse a vanilla texture until there is an own PNG.
        vanillaTexture(ModItems.ENHANCEMENT_STONE.get(), "amethyst_shard");
        vanillaTexture(ModItems.SKILL_BOOK.get(), "writable_book");
        vanillaTexture(ModItems.FRACASO_BLUEPRINT.get(), "map");
        vanillaTexture(ModItems.UNAPPRAISED_BOOK.get(), "book");
        vanillaTexture(ModItems.EYE_OF_PALLALIAN.get(), "ender_eye");
        vanillaTexture(ModItems.APPRENTICE_CONTRACT.get(), "paper");
        vanillaTexture(ModItems.WORKSHOP_LEDGER.get(), "writable_book");
        vanillaTexture(ModItems.JAFFA_SMITHING_HAMMER.get(), "mace");
        handheld(ModItems.COMMANDERS_SWORD.get(), "iron_sword");
        handheld(ModItems.RULERS_SWORD.get(), "golden_sword");
        vanillaTexture(ModItems.RAW_JAFFA.get(), "raw_copper");
        vanillaTexture(ModItems.BLUE_ORICHALCUM_INGOT.get(), "prismarine_shard");

        withExistingParent(ModItems.BLACKSMITH_SPAWN_EGG.getId().getPath(), mcLoc("item/template_spawn_egg"));
        withExistingParent(ModItems.VILLAGE_CHIEF_SPAWN_EGG.getId().getPath(), mcLoc("item/template_spawn_egg"));
        withExistingParent(ModItems.BONE_LORD_SPAWN_EGG.getId().getPath(), mcLoc("item/template_spawn_egg"));
    }

    /** A tool or weapon (held like a sword) that borrows a vanilla texture. */
    private void handheld(Item item, String vanillaTexture) {
        withExistingParent(BuiltInRegistries.ITEM.getKey(item).getPath(), mcLoc("item/handheld"))
                .texture("layer0", mcLoc("item/" + vanillaTexture));
    }

    private void vanillaTexture(Item item, String vanillaTexture) {
        withExistingParent(BuiltInRegistries.ITEM.getKey(item).getPath(), mcLoc("item/generated"))
                .texture("layer0", mcLoc("item/" + vanillaTexture));
    }
}
