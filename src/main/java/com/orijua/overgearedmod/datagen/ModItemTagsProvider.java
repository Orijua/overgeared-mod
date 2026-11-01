package com.orijua.overgearedmod.datagen;

import com.orijua.overgearedmod.Overgearedmod;
import com.orijua.overgearedmod.core.registry.ModItems;
import com.orijua.overgearedmod.feature.equipment.LootEvents;
import com.orijua.overgearedmod.feature.npc.ApprenticeMaterials;
import com.orijua.overgearedmod.feature.smithing.SmithingHammers;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.tags.ItemTagsProvider;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.common.data.ExistingFileHelper;

import java.util.concurrent.CompletableFuture;

final class ModItemTagsProvider extends ItemTagsProvider {
    /** Items that can roll weapon options (item_option "applies_to"). */
    static final TagKey<Item> WEAPONS = ItemTags.create(Overgearedmod.id("weapons"));
    /** Items that can roll armor options. */
    static final TagKey<Item> ARMOR = ItemTags.create(Overgearedmod.id("armor"));
    /** What Smith takes for his iron quest (deliver goal "#overgearedmod:smith_iron"): raw iron or the ores. */
    static final TagKey<Item> SMITH_IRON = ItemTags.create(Overgearedmod.id("smith_iron"));

    ModItemTagsProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> lookup,
                        CompletableFuture<TagLookup<Block>> blockTags, ExistingFileHelper files) {
        super(output, lookup, blockTags, Overgearedmod.MODID, files);
    }

    @Override
    protected void addTags(HolderLookup.Provider provider) {
        tag(WEAPONS).addTag(ItemTags.SWORDS).addTag(ItemTags.AXES);
        tag(ARMOR).addTag(ItemTags.HEAD_ARMOR).addTag(ItemTags.CHEST_ARMOR).addTag(ItemTags.LEG_ARMOR).addTag(ItemTags.FOOT_ARMOR);

        // What elites can drop (rated Rare or better): mid-tier gear, never the mod's unique weapons.
        tag(LootEvents.ELITE_DROPS).add(Items.IRON_SWORD, Items.IRON_AXE,
                Items.IRON_HELMET, Items.IRON_CHESTPLATE, Items.IRON_LEGGINGS, Items.IRON_BOOTS,
                Items.CHAINMAIL_HELMET, Items.CHAINMAIL_CHESTPLATE, Items.CHAINMAIL_LEGGINGS, Items.CHAINMAIL_BOOTS);

        // Being in minecraft:swords also makes them enchantable as swords.
        tag(ItemTags.SWORDS).add(ModItems.JAFFA_SWORD.get(), ModItems.GRID_LEGENDARY_SWORD.get(), ModItems.FRACASO_SWORD.get());

        copy(Tags.Blocks.ORES, Tags.Items.ORES);
        tag(Tags.Items.INGOTS).add(ModItems.MITHRIL_INGOT.get(), ModItems.JAFFA_INGOT.get(), ModItems.BLUE_ORICHALCUM_INGOT.get());
        tag(Tags.Items.RAW_MATERIALS).add(ModItems.RAW_MITHRIL.get(), ModItems.RAW_JAFFA.get(), ModItems.RAW_BLUE_ORICHALCUM.get());
        tag(SMITH_IRON).add(Items.RAW_IRON, Items.IRON_ORE, Items.DEEPSLATE_IRON_ORE);

        tag(SmithingHammers.TAG).add(ModItems.IRON_SMITHING_HAMMER.get(), ModItems.JAFFA_SMITHING_HAMMER.get(), ModItems.MITHRIL_SMITHING_HAMMER.get());

        // What an apprentice needs to have mastered to work each material; anything else is a beginner's.
        tag(ApprenticeMaterials.INTERMEDIATE).add(Items.GOLD_INGOT);
        tag(ApprenticeMaterials.ADVANCED).add(Items.DIAMOND);
        tag(ApprenticeMaterials.MASTER).add(Items.NETHERITE_INGOT, ModItems.MITHRIL_INGOT.get(), ModItems.BLUE_ORICHALCUM_INGOT.get());
    }
}
