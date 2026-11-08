package com.orijua.overgearedmod.feature.economy;

import com.orijua.overgearedmod.core.config.TradeBalance;
import com.orijua.overgearedmod.core.registry.ModDataComponents;
import com.orijua.overgearedmod.feature.equipment.EquipmentBonuses;
import com.orijua.overgearedmod.feature.equipment.ItemRating;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TieredItem;
import net.minecraft.world.item.Tiers;

import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

/**
 * The bridge between a stack and {@link TradeRules}: what a piece is worth and why it may not be traded. It touches no
 * client class, so the server and the Trade tab read the same numbers. {@code today} is a game day ({@link TradeRules#day}).
 */
public final class TradeGoods {
    private TradeGoods() {
    }

    /** Why this stack cannot be traded, if it cannot. Only a loaned-for-a-quest mark counts as a quest item. */
    public static Optional<TradeRules.Refusal> refusal(ItemStack stack) {
        return TradeRules.refusal(stack.getCount(), stack.isDamageableItem(),
                stack.get(ModDataComponents.ITEM_RATING) != null, stack.get(ModDataComponents.LOANED_FOR) != null);
    }

    /** The saturation key of a rated stack: call it only when {@link #refusal} is empty. */
    public static TradeKey key(ItemStack stack) {
        ItemRating rating = Objects.requireNonNull(stack.get(ModDataComponents.ITEM_RATING), "the stack has no rating");
        return new TradeKey(TradeCategory.classify(stack::is), rating);
    }

    /** The material multiplier: the vanilla tier of a tool or the material of an armor, else the config's other material. */
    public static double material(ItemStack stack, TradeBalance balance) {
        String name = null;
        if (stack.getItem() instanceof TieredItem tiered && tiered.getTier() instanceof Tiers tier) {
            name = tier.name().toLowerCase(Locale.ROOT);
        } else if (stack.getItem() instanceof ArmorItem armor) {
            name = armor.getMaterial().unwrapKey().map(key -> key.location().getPath()).orElse(null);
        }
        return name == null ? balance.otherMaterial() : balance.materialMultiplier().getOrDefault(name, balance.otherMaterial());
    }

    public static long reference(ItemStack stack, TradeBalance balance) {
        ItemRating rating = Objects.requireNonNull(stack.get(ModDataComponents.ITEM_RATING), "the stack has no rating");
        return TradeRules.reference(rating, material(stack, balance), stack.getOrDefault(ModDataComponents.ENHANCEMENT, 0),
                EquipmentBonuses.isWeapon(stack), balance);
    }

    /** What the stack pays in {@code mode} today, with the pressure the saturation has on its key. */
    public static long price(ItemStack stack, TradeMode mode, TradeSaturation saturation, long today, TradeBalance balance) {
        int pressure = saturation.pressure(key(stack), today, balance);
        return TradeRules.price(mode, reference(stack, balance), pressure, balance);
    }
}
