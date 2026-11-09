package com.orijua.overgearedmod.feature.economy;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.Optional;

/** The phone: opens the account and the work from anywhere in the real world; in Satisfy it shows "No signal". */
public class PhoneItem extends Item {
    public PhoneItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer
                && TerminalService.open(serverPlayer, Optional.empty()) == TerminalService.Result.IN_SATISFY) {
            serverPlayer.displayClientMessage(Component.translatable("message.overgearedmod.terminal.no_signal"), true);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }
}
