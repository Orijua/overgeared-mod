package com.orijua.overgearedmod.registry;

import com.orijua.overgearedmod.Overgearedmod;
import com.orijua.overgearedmod.progression.RespecScrollItem;
import com.orijua.overgearedmod.skill.SkillBookItem;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tiers;
import net.neoforged.neoforge.common.DeferredSpawnEggItem;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Overgearedmod.MODID);

    // Jaffa: ore -> raw jaffa -> (smelt) ingot -> the Jaffa sword. Textures borrowed from vanilla copper for now.
    public static final DeferredItem<BlockItem> JAFFA_ORE = ITEMS.registerSimpleBlockItem(ModBlocks.JAFFA_ORE);
    public static final DeferredItem<BlockItem> DEEPSLATE_JAFFA_ORE = ITEMS.registerSimpleBlockItem(ModBlocks.DEEPSLATE_JAFFA_ORE);
    public static final DeferredItem<Item> RAW_JAFFA = ITEMS.registerSimpleItem("raw_jaffa");
    public static final DeferredItem<Item> JAFFA_INGOT = ITEMS.registerSimpleItem("jaffa_ingot");

    // Mithril: ore -> raw mithril -> (smelt) ingot -> higher-tier material (enhancement stones, respec scrolls, Grid's sword).
    public static final DeferredItem<BlockItem> MITHRIL_ORE = ITEMS.registerSimpleBlockItem(ModBlocks.MITHRIL_ORE);
    public static final DeferredItem<BlockItem> DEEPSLATE_MITHRIL_ORE = ITEMS.registerSimpleBlockItem(ModBlocks.DEEPSLATE_MITHRIL_ORE);
    public static final DeferredItem<Item> RAW_MITHRIL = ITEMS.registerSimpleItem("raw_mithril");
    public static final DeferredItem<Item> MITHRIL_INGOT = ITEMS.registerSimpleItem("mithril_ingot");

    // Forged at the smithing anvil from 2 jaffa ingots and a stick, so it usually carries a rating.
    public static final DeferredItem<SwordItem> JAFFA_SWORD = ITEMS.register("jaffa_sword",
            () -> new SwordItem(Tiers.IRON, new Item.Properties()
                    .attributes(SwordItem.createAttributes(Tiers.IRON, 3, -2.4f))));

    // Grid's legendary sword (Extras). Forged from diamonds; stronger than a diamond sword but needs level 10 and 8 STR.
    public static final DeferredItem<SwordItem> GRID_LEGENDARY_SWORD = ITEMS.register("grid_legendary_sword",
            () -> new SwordItem(Tiers.DIAMOND, new Item.Properties()
                    .attributes(SwordItem.createAttributes(Tiers.DIAMOND, 5, -2.4f))));

    public static final DeferredItem<BlockItem> SMITHING_ANVIL = ITEMS.registerSimpleBlockItem(ModBlocks.SMITHING_ANVIL);

    public static final DeferredItem<Item> ENHANCEMENT_STONE = ITEMS.registerSimpleItem("enhancement_stone");

    public static final DeferredItem<SkillBookItem> SKILL_BOOK = ITEMS.registerItem("skill_book",
            SkillBookItem::new, new Item.Properties().stacksTo(1));

    public static final DeferredItem<DeferredSpawnEggItem> BLACKSMITH_SPAWN_EGG = ITEMS.register("blacksmith_spawn_egg",
            () -> new DeferredSpawnEggItem(ModEntities.BLACKSMITH, 0x4A3222, 0x9EE6F5, new Item.Properties()));
    public static final DeferredItem<DeferredSpawnEggItem> VILLAGE_CHIEF_SPAWN_EGG = ITEMS.register("village_chief_spawn_egg",
            () -> new DeferredSpawnEggItem(ModEntities.VILLAGE_CHIEF, 0x6B4A2B, 0xC9A64B, new Item.Properties()));
    public static final DeferredItem<DeferredSpawnEggItem> BONE_LORD_SPAWN_EGG = ITEMS.register("bone_lord_spawn_egg",
            () -> new DeferredSpawnEggItem(ModEntities.BONE_LORD, 0x1E1E1E, 0xD9D3C1, new Item.Properties()));

    public static final DeferredItem<RespecScrollItem> RESPEC_SCROLL = ITEMS.registerItem("respec_scroll",
            RespecScrollItem::new, new Item.Properties().stacksTo(16));

    private ModItems() {
    }
}
