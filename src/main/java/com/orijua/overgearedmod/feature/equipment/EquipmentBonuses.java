package com.orijua.overgearedmod.feature.equipment;

import com.orijua.overgearedmod.Overgearedmod;
import com.orijua.overgearedmod.core.registry.Definitions;
import com.orijua.overgearedmod.core.registry.ModDataComponents;
import com.orijua.overgearedmod.core.registry.ModDatapackRegistries;
import com.orijua.overgearedmod.feature.equipment.option.RolledOption;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemAttributeModifiers;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;

/**
 * Applies the mod's equipment bonuses (rating, enhancement, options) to the wearer as attribute
 * modifiers. Done per player rather than per item, so an item whose requirements the wearer
 * doesn't meet gives nothing. Recomputed on every equipment change and stat change.
 */
public final class EquipmentBonuses {
    private static final String PREFIX = "equip_";

    private EquipmentBonuses() {
    }

    public static void apply(Player player) {
        removeAll(player);
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            ItemStack stack = player.getItemBySlot(slot);
            if (!stack.isEmpty() && slot == bonusSlot(stack) && EquipmentEvents.meetsRequirements(player, stack)) {
                forEachBonus(stack, slot, (attribute, modifier) -> {
                    AttributeInstance instance = player.getAttribute(attribute);
                    if (instance != null) {
                        instance.addOrUpdateTransientModifier(modifier);
                    }
                });
            }
        }
        if (player.getHealth() > player.getMaxHealth()) {
            player.setHealth(player.getMaxHealth());
        }
    }

    /** The items the player wears where their bonuses count; their requirements aren't checked here. */
    public static List<ItemStack> wornEquipment(Player player) {
        List<ItemStack> worn = new ArrayList<>();
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            ItemStack stack = player.getItemBySlot(slot);
            if (!stack.isEmpty() && slot == bonusSlot(stack)) {
                worn.add(stack);
            }
        }
        return worn;
    }

    /** Armor counts in its own slot; anything else only in the main hand. */
    private static EquipmentSlot bonusSlot(ItemStack stack) {
        return stack.getItem() instanceof ArmorItem armor ? armor.getEquipmentSlot() : EquipmentSlot.MAINHAND;
    }

    private static boolean isWeapon(ItemStack stack) {
        return stack.getOrDefault(DataComponents.ATTRIBUTE_MODIFIERS, ItemAttributeModifiers.EMPTY).modifiers().stream()
                .anyMatch(entry -> entry.attribute().equals(Attributes.ATTACK_DAMAGE));
    }

    private static void forEachBonus(ItemStack stack, EquipmentSlot slot, BiConsumer<Holder<Attribute>, AttributeModifier> add) {
        String base = PREFIX + slot.getName() + "_";
        if (isWeapon(stack)) {
            ItemRating rating = stack.get(ModDataComponents.ITEM_RATING);
            if (rating != null && rating.attackDamageBonus() > 0) {
                add.accept(Attributes.ATTACK_DAMAGE, flat(base + "rating", rating.attackDamageBonus()));
            }
            int enhancement = stack.getOrDefault(ModDataComponents.ENHANCEMENT, 0);
            if (enhancement > 0) {
                add.accept(Attributes.ATTACK_DAMAGE, flat(base + "enhancement", enhancement * EquipmentRules.ATTACK_PER_ENHANCEMENT));
            }
        }
        List<RolledOption> options = stack.getOrDefault(ModDataComponents.ITEM_OPTIONS, List.of());
        for (int i = 0; i < options.size(); i++) {
            RolledOption rolled = options.get(i);
            ResourceLocation id = Overgearedmod.id(base + "option_" + i);
            Definitions.get(ModDatapackRegistries.ITEM_OPTION, rolled.option())
                    .ifPresent(def -> def.effect().modifiers(id, rolled.value(), add));
        }
    }

    private static AttributeModifier flat(String path, double amount) {
        return new AttributeModifier(Overgearedmod.id(path), amount, AttributeModifier.Operation.ADD_VALUE);
    }

    /** Stateless clean-up: drops every modifier this class may have added, on any attribute. */
    private static void removeAll(Player player) {
        BuiltInRegistries.ATTRIBUTE.holders().forEach(attribute -> {
            AttributeInstance instance = player.getAttribute(attribute);
            if (instance != null) {
                instance.getModifiers().stream()
                        .map(AttributeModifier::id)
                        .filter(id -> id.getNamespace().equals(Overgearedmod.MODID) && id.getPath().startsWith(PREFIX))
                        .toList()
                        .forEach(instance::removeModifier);
            }
        });
    }
}
