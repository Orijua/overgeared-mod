package com.orijua.overgearedmod.npc;

import com.orijua.overgearedmod.Overgearedmod;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;

/**
 * Which materials an apprentice may work at each mastery stage, as item tags (data, so new materials only need a tag).
 * A material in none of them is a beginner's (iron, jaffa...). Gold needs an intermediate smith, diamond an advanced
 * one, and netherite, mithril or orichalcum a master.
 */
public final class ApprenticeMaterials {
    public static final TagKey<Item> INTERMEDIATE = ItemTags.create(Overgearedmod.id("apprentice_materials/intermediate"));
    public static final TagKey<Item> ADVANCED = ItemTags.create(Overgearedmod.id("apprentice_materials/advanced"));
    public static final TagKey<Item> MASTER = ItemTags.create(Overgearedmod.id("apprentice_materials/master"));

    private ApprenticeMaterials() {
    }

    /** The stage needed to work {@code material}: the highest any of its items asks for. */
    public static MasteryStage requiredStage(Ingredient material) {
        MasteryStage needed = MasteryStage.BEGINNER;
        for (ItemStack stack : material.getItems()) {
            MasteryStage stage = stack.is(MASTER) ? MasteryStage.MASTER
                    : stack.is(ADVANCED) ? MasteryStage.ADVANCED
                    : stack.is(INTERMEDIATE) ? MasteryStage.INTERMEDIATE : MasteryStage.BEGINNER;
            if (stage.atLeast(needed)) {
                needed = stage;
            }
        }
        return needed;
    }
}
