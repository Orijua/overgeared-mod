package com.orijua.overgearedmod.feature.winston;

import com.orijua.overgearedmod.feature.quest.goal.GoalTracker;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * The contract by which Khan's smithy would pass to the Mero Company (canon, cap. 24: "destroy the smithy transfer
 * contract"). The last of the thugs drops it; right-click tears it up, which counts the
 * {@link WinstonStory#CONTRACT_DESTROYED} milestone.
 */
public class TransferContractItem extends Item {
    public TransferContractItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player instanceof ServerPlayer serverPlayer) {
            GoalTracker.milestone(serverPlayer, WinstonStory.CONTRACT_DESTROYED);
            serverPlayer.displayClientMessage(Component.translatable("message.overgearedmod.contract_torn"), false);
            serverPlayer.serverLevel().sendParticles(new ItemParticleOption(ParticleTypes.ITEM, new ItemStack(Items.PAPER)),
                    player.getX(), player.getEyeY() - 0.3, player.getZ(), 20, 0.3, 0.2, 0.3, 0.05);
        }
        level.playSound(null, player.blockPosition(), SoundEvents.BOOK_PAGE_TURN, SoundSource.PLAYERS, 1.0f, 0.6f);
        stack.consume(1, player);
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.overgearedmod.transfer_contract.desc").withStyle(ChatFormatting.GRAY));
    }
}
