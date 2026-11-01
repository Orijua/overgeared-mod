package com.orijua.overgearedmod.pagma;

import com.orijua.overgearedmod.client.PagmaBookScreen;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * Pagma's rare book, appraised (canon, cap. 1): Legendary, no conditions; whoever reads it becomes Pagma's successor,
 * and their level and stats change. Using it asks for confirmation on the client, then the server reads it
 * ({@link PagmaBook#read}).
 */
public class PagmaRareBookItem extends Item {
    public PagmaRareBookItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        if (level.isClientSide) {
            // Only runs on the client, so the client-only class is never loaded on a server.
            PagmaBookScreen.confirm();
        }
        return InteractionResultHolder.sidedSuccess(player.getItemInHand(hand), level.isClientSide);
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.overgearedmod.pagma_rare_book.rating").withStyle(ChatFormatting.GOLD));
        tooltip.add(Component.translatable("item.overgearedmod.pagma_rare_book.effect").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("item.overgearedmod.pagma_rare_book.conditions").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("item.overgearedmod.pagma_rare_book.warning").withStyle(ChatFormatting.RED));
    }
}
