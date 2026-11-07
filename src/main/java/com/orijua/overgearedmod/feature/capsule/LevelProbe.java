package com.orijua.overgearedmod.feature.capsule;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * {@link SafeSpot.Probe} over a real level. Reading a block loads its chunk ({@code Level.getBlockState} asks for the
 * chunk at full status).
 */
public record LevelProbe(ServerLevel level) implements SafeSpot.Probe {
    /**
     * Free to stand in: no collision, no fluid, and none of the blocks that have no collision but hurt, trap or move
     * you (fire, cobweb, sweet berry bush, powder snow, portals).
     */
    @Override
    public boolean isOpen(BlockPos pos) {
        if (level.isOutsideBuildHeight(pos)) {
            return false;
        }
        BlockState state = level.getBlockState(pos);
        return state.getCollisionShape(level, pos).isEmpty()
                && state.getFluidState().isEmpty()
                && !state.is(BlockTags.FIRE)
                && !state.is(Blocks.COBWEB)
                && !state.is(Blocks.SWEET_BERRY_BUSH)
                && !state.is(Blocks.POWDER_SNOW)
                && !state.is(Blocks.NETHER_PORTAL)
                && !state.is(Blocks.END_PORTAL);
    }

    /**
     * Has collision and is not magma. Lava and fire never qualify on their own: they have no collision, so the first
     * check already rejects them (there is no collidable block with lava in it to test a separate branch with).
     */
    @Override
    public boolean isSafeFloor(BlockPos pos) {
        if (level.isOutsideBuildHeight(pos)) {
            return false;
        }
        BlockState state = level.getBlockState(pos);
        return !state.getCollisionShape(level, pos).isEmpty() && !state.is(Blocks.MAGMA_BLOCK);
    }
}
