package com.orijua.overgearedmod.datagen;

import com.orijua.overgearedmod.Overgearedmod;
import com.orijua.overgearedmod.core.registry.ModBlocks;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.tags.BlockTags;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.common.data.BlockTagsProvider;
import net.neoforged.neoforge.common.data.ExistingFileHelper;

import java.util.concurrent.CompletableFuture;

final class ModBlockTagsProvider extends BlockTagsProvider {
    ModBlockTagsProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> lookup, ExistingFileHelper files) {
        super(output, lookup, Overgearedmod.MODID, files);
    }

    @Override
    protected void addTags(HolderLookup.Provider provider) {
        tag(BlockTags.MINEABLE_WITH_PICKAXE).add(ModBlocks.SMITHING_ANVIL.get(), ModBlocks.SMELTER.get(), ModBlocks.CAPSULE.get(), ModBlocks.ADVANCED_CAPSULE.get(),
                ModBlocks.DIAMOND_CAPSULE.get(), ModBlocks.ACCOUNT_TERMINAL.get(),
                ModBlocks.MITHRIL_ORE.get(), ModBlocks.DEEPSLATE_MITHRIL_ORE.get(),
                ModBlocks.JAFFA_ORE.get(), ModBlocks.DEEPSLATE_JAFFA_ORE.get(), ModBlocks.BLUE_ORICHALCUM_ORE.get(),
                ModBlocks.DEEPSLATE_BLUE_ORICHALCUM_ORE.get());
        tag(BlockTags.NEEDS_DIAMOND_TOOL).add(ModBlocks.BLUE_ORICHALCUM_ORE.get(), ModBlocks.DEEPSLATE_BLUE_ORICHALCUM_ORE.get());
        tag(BlockTags.NEEDS_STONE_TOOL).add(ModBlocks.JAFFA_ORE.get(), ModBlocks.DEEPSLATE_JAFFA_ORE.get());
        tag(BlockTags.NEEDS_IRON_TOOL).add(ModBlocks.MITHRIL_ORE.get(), ModBlocks.DEEPSLATE_MITHRIL_ORE.get());
        // c:ores, shared with other mods.
        tag(Tags.Blocks.ORES).add(ModBlocks.MITHRIL_ORE.get(), ModBlocks.DEEPSLATE_MITHRIL_ORE.get(), ModBlocks.JAFFA_ORE.get(),
                ModBlocks.DEEPSLATE_JAFFA_ORE.get(), ModBlocks.BLUE_ORICHALCUM_ORE.get(),
                ModBlocks.DEEPSLATE_BLUE_ORICHALCUM_ORE.get());
    }
}
