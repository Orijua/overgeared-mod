package com.orijua.overgearedmod.smithing;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/**
 * A smithing hammer, forged at the smithing anvil like any equipment (so it has a rating, options and can be
 * enhanced). In the off hand it improves its owner's forging; an apprentice can carry one too. Each piece forged with it
 * wears it a little. {@code materialBonus} is the base its material gives (see {@link HammerRules}).
 */
public class SmithingHammerItem extends Item {
    private final int materialBonus;

    public SmithingHammerItem(int materialBonus, Properties properties) {
        super(properties);
        this.materialBonus = materialBonus;
    }

    public int materialBonus() {
        return materialBonus;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        if (context.registries() != null) {
            tooltip.add(Component.translatable("item.overgearedmod.smithing_hammer.bonus",
                    SmithingHammers.format(SmithingHammers.skillBonus(context.registries(), stack))).withStyle(ChatFormatting.GOLD));
        }
        tooltip.add(Component.translatable("item.overgearedmod.smithing_hammer.desc").withStyle(ChatFormatting.GRAY));
    }
}
