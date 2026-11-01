package com.orijua.overgearedmod.feature.progression;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** Right-click to get every allocated stat point back as free points. Consumed on use. */
public class RespecScrollItem extends Item {
    public RespecScrollItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player instanceof ServerPlayer serverPlayer) {
            PlayerStats before = ProgressionService.get(serverPlayer);
            PlayerStats after = ProgressionRules.refundAll(before);
            if (after.freePoints() == before.freePoints()) {
                serverPlayer.displayClientMessage(Component.translatable("message.overgearedmod.respec_nothing"), true);
                return InteractionResultHolder.fail(stack);
            }
            ProgressionService.set(serverPlayer, after);
            stack.consume(1, player);
            serverPlayer.displayClientMessage(Component.translatable("message.overgearedmod.respec_done", after.freePoints()), true);
            level.playSound(null, player.blockPosition(), SoundEvents.BOOK_PAGE_TURN, SoundSource.PLAYERS, 1.0f, 1.0f);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }
}
