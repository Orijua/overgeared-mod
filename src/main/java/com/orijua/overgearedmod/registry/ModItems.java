package com.orijua.overgearedmod.registry;

import com.orijua.overgearedmod.Overgearedmod;
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

    private ModItems() {
    }
}
