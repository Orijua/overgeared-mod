package com.orijua.overgearedmod.core.registry;

import com.orijua.overgearedmod.Overgearedmod;
import com.orijua.overgearedmod.feature.insight.InsightSwordItem;
import com.orijua.overgearedmod.feature.npc.ApprenticeContractItem;
import com.orijua.overgearedmod.feature.npc.WorkshopLedgerItem;
import com.orijua.overgearedmod.feature.pagma.EyeOfPallalianItem;
import com.orijua.overgearedmod.feature.pagma.PagmaRareBookItem;
import com.orijua.overgearedmod.feature.pagma.UnappraisedItem;
import com.orijua.overgearedmod.feature.progression.RespecScrollItem;
import com.orijua.overgearedmod.feature.skill.SkillBookItem;
import com.orijua.overgearedmod.feature.smithing.BlueprintItem;
import com.orijua.overgearedmod.feature.smithing.SmithingHammerItem;
import com.orijua.overgearedmod.feature.winston.TransferContractItem;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
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

    // Blue orichalcum: ore -> raw -> (smelter only) ingot -> Fracaso. Textures borrowed from vanilla for now.
    public static final DeferredItem<BlockItem> BLUE_ORICHALCUM_ORE = ITEMS.registerSimpleBlockItem(ModBlocks.BLUE_ORICHALCUM_ORE);
    public static final DeferredItem<BlockItem> DEEPSLATE_BLUE_ORICHALCUM_ORE = ITEMS.registerSimpleBlockItem(ModBlocks.DEEPSLATE_BLUE_ORICHALCUM_ORE);
    public static final DeferredItem<Item> RAW_BLUE_ORICHALCUM = ITEMS.registerSimpleItem("raw_blue_orichalcum");
    public static final DeferredItem<Item> BLUE_ORICHALCUM_INGOT = ITEMS.registerSimpleItem("blue_orichalcum_ingot");

    // Mithril: ore -> raw mithril -> (smelter only) ingot -> higher-tier material (enhancement stones, respec scrolls, Grid's sword).
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

    // Fracaso (canon cap. 11): Grid's first legendary work, a blue orichalcum greatsword whose requirements nobody meets.
    // Forged only from its blueprint, which the Legendary Smith Successor quest gives and the forge uses up.
    public static final DeferredItem<SwordItem> FRACASO_SWORD = ITEMS.register("fracaso_sword",
            () -> new SwordItem(Tiers.NETHERITE, new Item.Properties()
                    .attributes(SwordItem.createAttributes(Tiers.NETHERITE, 7, -2.8f))));
    public static final DeferredItem<BlueprintItem> FRACASO_BLUEPRINT = ITEMS.registerItem("fracaso_blueprint",
            BlueprintItem::new, new Item.Properties().stacksTo(1).rarity(Rarity.EPIC));

    // The book on the golden table of the Northern End Cave (canon, cap. 1): unknown until it is appraised.
    public static final DeferredItem<UnappraisedItem> UNAPPRAISED_BOOK = ITEMS.registerItem("unappraised_book",
            UnappraisedItem::new, new Item.Properties().stacksTo(1).fireResistant());
    // The Eye of Pallalian appraises it (sold by Smith, expensive): it becomes Pagma's rare book, and Earl Ashur comes for it.
    public static final DeferredItem<EyeOfPallalianItem> EYE_OF_PALLALIAN = ITEMS.registerItem("eye_of_pallalian",
            EyeOfPallalianItem::new, new Item.Properties().stacksTo(16).rarity(Rarity.RARE));
    public static final DeferredItem<PagmaRareBookItem> PAGMA_RARE_BOOK = ITEMS.registerItem("pagma_rare_book",
            PagmaRareBookItem::new, new Item.Properties().stacksTo(1).rarity(Rarity.EPIC).fireResistant());
    public static final DeferredItem<BlockItem> GOLDEN_TABLE = ITEMS.registerSimpleBlockItem(ModBlocks.GOLDEN_TABLE);

    // An apprentice blacksmith's contract (mass production, phase 2): Smith gives the first at smithing 50 and sells the rest.
    public static final DeferredItem<ApprenticeContractItem> APPRENTICE_CONTRACT = ITEMS.registerItem("apprentice_contract",
            ApprenticeContractItem::new, new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON));
    // Smithing hammers (forged at the anvil, rated): in the off hand they improve forging; an apprentice can carry one.
    public static final DeferredItem<SmithingHammerItem> IRON_SMITHING_HAMMER = ITEMS.registerItem("iron_smithing_hammer",
            properties -> new SmithingHammerItem(5, properties), new Item.Properties().durability(250));
    public static final DeferredItem<SmithingHammerItem> JAFFA_SMITHING_HAMMER = ITEMS.registerItem("jaffa_smithing_hammer",
            properties -> new SmithingHammerItem(10, properties), new Item.Properties().durability(500));
    public static final DeferredItem<SmithingHammerItem> MITHRIL_SMITHING_HAMMER = ITEMS.registerItem("mithril_smithing_hammer",
            properties -> new SmithingHammerItem(20, properties), new Item.Properties().durability(1000).rarity(Rarity.UNCOMMON));
    // Swords of observation (canon, cap. 157): the Commander's (the chief's watch quest) and the Ruler's (lent by the chief).
    public static final DeferredItem<InsightSwordItem> COMMANDERS_SWORD = ITEMS.registerItem("commanders_sword",
            properties -> new InsightSwordItem(Tiers.IRON, 140, 10, false, properties.attributes(SwordItem.createAttributes(Tiers.IRON, 3, -2.4f))),
            new Item.Properties().rarity(Rarity.UNCOMMON));
    public static final DeferredItem<InsightSwordItem> RULERS_SWORD = ITEMS.registerItem("rulers_sword",
            properties -> new InsightSwordItem(Tiers.DIAMOND, 100, 25, true, properties.attributes(SwordItem.createAttributes(Tiers.DIAMOND, 3, -2.4f))),
            new Item.Properties().rarity(Rarity.RARE));
    // Opens the Workshop tab: Smith gives it with the first contract; also crafted (book + iron ingot).
    public static final DeferredItem<WorkshopLedgerItem> WORKSHOP_LEDGER = ITEMS.registerItem("workshop_ledger",
            WorkshopLedgerItem::new, new Item.Properties().stacksTo(1));

    public static final DeferredItem<BlockItem> SMITHING_ANVIL = ITEMS.registerSimpleBlockItem(ModBlocks.SMITHING_ANVIL);
    public static final DeferredItem<BlockItem> SMELTER = ITEMS.registerSimpleBlockItem(ModBlocks.SMELTER);

    public static final DeferredItem<Item> ENHANCEMENT_STONE = ITEMS.registerSimpleItem("enhancement_stone");

    public static final DeferredItem<SkillBookItem> SKILL_BOOK = ITEMS.registerItem("skill_book",
            SkillBookItem::new, new Item.Properties().stacksTo(1));

    public static final DeferredItem<DeferredSpawnEggItem> BLACKSMITH_SPAWN_EGG = ITEMS.register("blacksmith_spawn_egg",
            () -> new DeferredSpawnEggItem(ModEntities.BLACKSMITH, 0x4A3222, 0x9EE6F5, new Item.Properties()));
    public static final DeferredItem<DeferredSpawnEggItem> VILLAGE_CHIEF_SPAWN_EGG = ITEMS.register("village_chief_spawn_egg",
            () -> new DeferredSpawnEggItem(ModEntities.VILLAGE_CHIEF, 0x6B4A2B, 0xC9A64B, new Item.Properties()));
    public static final DeferredItem<DeferredSpawnEggItem> BONE_LORD_SPAWN_EGG = ITEMS.register("bone_lord_spawn_egg",
            () -> new DeferredSpawnEggItem(ModEntities.BONE_LORD, 0x1E1E1E, 0xD9D3C1, new Item.Properties()));
    public static final DeferredItem<DeferredSpawnEggItem> KHAN_SPAWN_EGG = ITEMS.register("khan_spawn_egg",
            () -> new DeferredSpawnEggItem(ModEntities.KHAN, 0x4A3A2A, 0x8C8C8C, new Item.Properties()));

    public static final DeferredItem<TransferContractItem> TRANSFER_CONTRACT = ITEMS.registerItem("transfer_contract",
            TransferContractItem::new, new Item.Properties().stacksTo(1));

    public static final DeferredItem<RespecScrollItem> RESPEC_SCROLL = ITEMS.registerItem("respec_scroll",
            RespecScrollItem::new, new Item.Properties().stacksTo(16));

    private ModItems() {
    }
}
