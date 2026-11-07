package com.orijua.overgearedmod.feature.capsule;

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

/**
 * The VR capsule (canon: how a player enters Satisfy). Right-click to lie down: after a short "Connecting…" the player
 * appears in Satisfy. Faces the player who places it.
 */
public class CapsuleBlock extends HorizontalDirectionalBlock {
    public static final MapCodec<CapsuleBlock> CODEC = simpleCodec(CapsuleBlock::new);

    public CapsuleBlock(Properties properties) {
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
        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
            switch (CapsuleService.begin(serverPlayer, pos)) {
                case IN_SATISFY, ALREADY_CONNECTED -> serverPlayer.displayClientMessage(Component.translatable("message.overgearedmod.capsule.in_satisfy"), true);
                case NOT_OVERWORLD -> serverPlayer.displayClientMessage(Component.translatable("message.overgearedmod.capsule.only_overworld"), true);
                case NO_SATISFY -> serverPlayer.displayClientMessage(Component.translatable("message.overgearedmod.capsule.no_satisfy"), true);
                case ALREADY_CONNECTING, STARTED -> {
                }
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
