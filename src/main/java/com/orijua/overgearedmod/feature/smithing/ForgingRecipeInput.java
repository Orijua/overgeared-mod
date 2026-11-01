package com.orijua.overgearedmod.feature.smithing;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeInput;

/** What the recipe lookup sees: the two input slots of the smithing anvil. */
public record ForgingRecipeInput(ItemStack material, ItemStack handle) implements RecipeInput {
    @Override
    public ItemStack getItem(int index) {
        return switch (index) {
            case 0 -> material;
            case 1 -> handle;
            default -> throw new IllegalArgumentException("No slot " + index);
        };
    }

    @Override
    public int size() {
        return 2;
    }
}
