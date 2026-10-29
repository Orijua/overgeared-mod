package com.orijua.overgearedmod.equipment;

import com.orijua.overgearedmod.Overgearedmod;
import com.orijua.overgearedmod.registry.ModDataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.ItemAttributeModifierEvent;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

/** Turns the rating data component into gameplay (attack bonus) and UI (tooltip line). */
public final class RatingEvents {
    private static final ResourceLocation RATING_BONUS_ID = Overgearedmod.id("rating_bonus");

    private RatingEvents() {
    }

    public static void register(IEventBus gameBus) {
        gameBus.addListener(RatingEvents::onItemAttributes);
        gameBus.addListener(RatingEvents::onTooltip);
    }

    public static ItemRating getRating(ItemStack stack) {
        return stack.get(ModDataComponents.ITEM_RATING);
    }

    /** Only weapons (items that already deal attack damage) get the bonus. */
    private static void onItemAttributes(ItemAttributeModifierEvent event) {
        ItemRating rating = getRating(event.getItemStack());
        if (rating == null || rating.attackDamageBonus() <= 0) {
            return;
        }
        boolean isWeapon = event.getDefaultModifiers().modifiers().stream()
                .anyMatch(entry -> entry.attribute().equals(Attributes.ATTACK_DAMAGE));
        if (!isWeapon) {
            return;
        }
        event.addModifier(Attributes.ATTACK_DAMAGE,
                new AttributeModifier(RATING_BONUS_ID, rating.attackDamageBonus(), AttributeModifier.Operation.ADD_VALUE),
                EquipmentSlotGroup.MAINHAND);
    }

    /** Fired on the client only; the rating line goes right under the item name. */
    private static void onTooltip(ItemTooltipEvent event) {
        ItemRating rating = getRating(event.getItemStack());
        if (rating == null) {
            return;
        }
        int index = Math.min(1, event.getToolTip().size());
        event.getToolTip().add(index, Component.translatable("tooltip.overgearedmod.rating", rating.displayName()));
    }
}
