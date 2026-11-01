package com.orijua.overgearedmod.smithing;

import com.orijua.overgearedmod.Overgearedmod;
import com.orijua.overgearedmod.equipment.ItemRating;
import com.orijua.overgearedmod.equipment.option.OptionEffect;
import com.orijua.overgearedmod.equipment.option.RolledOption;
import com.orijua.overgearedmod.registry.Definitions;
import com.orijua.overgearedmod.registry.ModDataComponents;
import com.orijua.overgearedmod.registry.ModDatapackRegistries;
import net.minecraft.core.HolderLookup;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.function.ToDoubleBiFunction;

/** Reading a smithing hammer: what it adds to a forge, finding the best one, and wearing it down. */
public final class SmithingHammers {
    /** The hammers; their own options ("precision", "learning") roll only on these, and generic ones never do. */
    public static final TagKey<Item> TAG = ItemTags.create(Overgearedmod.id("smithing_hammers"));

    private SmithingHammers() {
    }

    public static boolean isHammer(ItemStack stack) {
        return stack.getItem() instanceof SmithingHammerItem;
    }

    /** Skill points the hammer adds to the rating roll (0 if it isn't one). */
    public static double skillBonus(HolderLookup.Provider registries, ItemStack stack) {
        if (!(stack.getItem() instanceof SmithingHammerItem hammer)) {
            return 0;
        }
        ItemRating rating = stack.getOrDefault(ModDataComponents.ITEM_RATING, ItemRating.NORMAL);
        return HammerRules.skillBonus(hammer.materialBonus(), rating, stack.getOrDefault(ModDataComponents.ENHANCEMENT, 0),
                sumOptions(registries, stack, OptionEffect::forgingSkill));
    }

    /** Extra smithing experience from the hammer's options (0.1 = +10 %). */
    public static double xpBonus(HolderLookup.Provider registries, ItemStack stack) {
        return isHammer(stack) ? sumOptions(registries, stack, OptionEffect::smithingXp) : 0;
    }

    private static double sumOptions(HolderLookup.Provider registries, ItemStack stack, ToDoubleBiFunction<OptionEffect, Float> part) {
        double total = 0;
        for (RolledOption rolled : stack.getOrDefault(ModDataComponents.ITEM_OPTIONS, List.<RolledOption>of())) {
            total += Definitions.get(registries, ModDatapackRegistries.ITEM_OPTION, rolled.option())
                    .map(def -> part.applyAsDouble(def.effect(), rolled.value())).orElse(0.0);
        }
        return total;
    }

    /** The inventory slot holding the best hammer (highest bonus), or -1. */
    public static int bestSlot(HolderLookup.Provider registries, Inventory inventory) {
        int best = -1;
        double bestBonus = -1;
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            double bonus = isHammer(inventory.getItem(slot)) ? skillBonus(registries, inventory.getItem(slot)) : -1;
            if (bonus > bestBonus) {
                best = slot;
                bestBonus = bonus;
            }
        }
        return best;
    }

    /** One piece forged with the hammer in {@code slot} of {@code smith}: it wears a little (and may break). */
    public static void wear(LivingEntity smith, EquipmentSlot slot) {
        ItemStack hammer = smith.getItemBySlot(slot);
        if (isHammer(hammer)) {
            hammer.hurtAndBreak(1, smith, slot);
        }
    }

    /** "+12" or "+12.5". */
    public static String format(double bonus) {
        return bonus == Math.rint(bonus) ? "+" + (int) bonus : String.format("+%.1f", bonus);
    }
}
