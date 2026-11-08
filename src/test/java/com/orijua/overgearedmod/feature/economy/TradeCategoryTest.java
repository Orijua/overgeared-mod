package com.orijua.overgearedmod.feature.economy;

import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.common.Tags;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TradeCategoryTest {
    @SafeVarargs
    private static TradeCategory of(TagKey<Item>... tags) {
        return TradeCategory.classify(List.of(tags)::contains);
    }

    @Test
    void eachTagGivesItsCategory() {
        assertEquals(TradeCategory.SWORD, of(ItemTags.SWORDS));
        assertEquals(TradeCategory.AXE, of(ItemTags.AXES));
        assertEquals(TradeCategory.PICKAXE, of(ItemTags.PICKAXES));
        assertEquals(TradeCategory.SHOVEL, of(ItemTags.SHOVELS));
        assertEquals(TradeCategory.BOW, of(Tags.Items.TOOLS_BOW));
        assertEquals(TradeCategory.SHIELD, of(Tags.Items.TOOLS_SHIELD));
        assertEquals(TradeCategory.HELMET, of(ItemTags.HEAD_ARMOR));
        assertEquals(TradeCategory.CHESTPLATE, of(ItemTags.CHEST_ARMOR));
        assertEquals(TradeCategory.LEGGINGS, of(ItemTags.LEG_ARMOR));
        assertEquals(TradeCategory.BOOTS, of(ItemTags.FOOT_ARMOR));
    }

    @Test
    void theFirstInOrderWins() {
        assertEquals(TradeCategory.SWORD, of(ItemTags.AXES, ItemTags.SWORDS));
    }

    @Test
    void noTagIsOther() {
        assertEquals(TradeCategory.OTHER, of());
        assertEquals(TradeCategory.OTHER, of(ItemTags.HOES));
    }
}
