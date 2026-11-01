package com.orijua.overgearedmod.pagma;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/**
 * An item whose nature is unknown until it is appraised (canon, cap. 1: Pagma's rare book needs appraisal before Grid
 * knows what it is). The tooltip only says that; appraising it comes with Earl Ashur's quest.
 */
public class UnappraisedItem extends Item {
    public UnappraisedItem(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.overgearedmod.unappraised.tooltip").withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
    }
}
