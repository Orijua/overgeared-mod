package com.orijua.overgearedmod.datagen;

import com.orijua.overgearedmod.Overgearedmod;
import com.orijua.overgearedmod.registry.ModItems;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.tags.ItemTagsProvider;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.common.data.ExistingFileHelper;

import java.util.concurrent.CompletableFuture;

final class ModItemTagsProvider extends ItemTagsProvider {
    /** Items that can roll weapon options (item_option "applies_to"). */
    static final TagKey<Item> WEAPONS = ItemTags.create(Overgearedmod.id("weapons"));
    /** Items that can roll armor options. */
    static final TagKey<Item> ARMOR = ItemTags.create(Overgearedmod.id("armor"));

    ModItemTagsProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> lookup,
                        CompletableFuture<TagLookup<Block>> blockTags, ExistingFileHelper files) {
        super(output, lookup, blockTags, Overgearedmod.MODID, files);
    }

    @Override
    protected void addTags(HolderLookup.Provider provider) {
        tag(WEAPONS).addTag(ItemTags.SWORDS).addTag(ItemTags.AXES);
        tag(ARMOR).addTag(ItemTags.HEAD_ARMOR).addTag(ItemTags.CHEST_ARMOR).addTag(ItemTags.LEG_ARMOR).addTag(ItemTags.FOOT_ARMOR);

        // Being in minecraft:swords also makes them enchantable as swords.
        tag(ItemTags.SWORDS).add(ModItems.JAFFA_SWORD.get(), ModItems.GRID_LEGENDARY_SWORD.get());

        copy(Tags.Blocks.ORES, Tags.Items.ORES);
        tag(Tags.Items.INGOTS).add(ModItems.MITHRIL_INGOT.get());
        tag(Tags.Items.RAW_MATERIALS).add(ModItems.RAW_MITHRIL.get());
    }
}
