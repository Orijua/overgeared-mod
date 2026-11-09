package com.orijua.overgearedmod.feature.economy;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.BlockHitResult;

import java.util.Optional;

/**
 * The account terminal: right-click to see the account, convert Satisfy emeralds and do the daily work. It only works in
 * the real world (in Satisfy it shows "No signal"). Faces the player who places it.
 */
public class AccountTerminalBlock extends HorizontalDirectionalBlock {
    public static final MapCodec<AccountTerminalBlock> CODEC = simpleCodec(AccountTerminalBlock::new);

    public AccountTerminalBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer
                && TerminalService.open(serverPlayer, Optional.of(pos)) == TerminalService.Result.IN_SATISFY) {
            serverPlayer.displayClientMessage(Component.translatable("message.overgearedmod.terminal.no_signal"), true);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
