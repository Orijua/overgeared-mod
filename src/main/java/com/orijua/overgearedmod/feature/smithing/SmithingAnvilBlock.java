package com.orijua.overgearedmod.feature.smithing;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/**
 * Opens the forging menu, like a crafting table: no block entity, the items live in the menu
 * and are given back when it closes. Project design (canon: Grid forges at smithies/anvils).
 */
public class SmithingAnvilBlock extends Block {
    private static final Component TITLE = Component.translatable("container.overgearedmod.smithing_anvil");

    public SmithingAnvilBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (!level.isClientSide) {
            player.openMenu(state.getMenuProvider(level, pos));
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    protected MenuProvider getMenuProvider(BlockState state, Level level, BlockPos pos) {
        return new SimpleMenuProvider(
                (containerId, inventory, player) -> new SmithingAnvilMenu(containerId, inventory, ContainerLevelAccess.create(level, pos)),
                TITLE);
    }
}
