package com.orijua.overgearedmod.registry;

import com.orijua.overgearedmod.Overgearedmod;
import com.orijua.overgearedmod.progression.RespecScrollItem;
import com.orijua.overgearedmod.skill.SkillBookItem;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tiers;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Overgearedmod.MODID);

    // Original project content (placeholder material).
    public static final DeferredItem<Item> TEST_INGOT = ITEMS.registerSimpleItem("test_ingot");

    // Original project content. Forged at the smithing anvil, so it usually carries a rating.
    public static final DeferredItem<SwordItem> JAFFA_SWORD = ITEMS.register("jaffa_sword",
            () -> new SwordItem(Tiers.IRON, new Item.Properties()
                    .attributes(SwordItem.createAttributes(Tiers.IRON, 3, -2.4f))));

    public static final DeferredItem<BlockItem> SMITHING_ANVIL = ITEMS.registerSimpleBlockItem(ModBlocks.SMITHING_ANVIL);

    public static final DeferredItem<Item> ENHANCEMENT_STONE = ITEMS.registerSimpleItem("enhancement_stone");

    public static final DeferredItem<SkillBookItem> SKILL_BOOK = ITEMS.registerItem("skill_book",
            SkillBookItem::new, new Item.Properties().stacksTo(1));

    public static final DeferredItem<RespecScrollItem> RESPEC_SCROLL = ITEMS.registerItem("respec_scroll",
            RespecScrollItem::new, new Item.Properties().stacksTo(16));

    private ModItems() {
    }
}
