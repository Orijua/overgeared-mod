package com.orijua.overgearedmod.feature.pagma;

import com.mojang.serialization.MapCodec;
import com.orijua.overgearedmod.core.registry.ModItems;
import com.orijua.overgearedmod.feature.notification.NotificationEvents;
import com.orijua.overgearedmod.feature.notification.SystemNotification;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.List;

/**
 * The golden table at the back of the Northern End Cave (canon, cap. 1: Pagma's rare book lies on a golden table that
 * can't be moved or taken). It can't be broken; using it while the book is on it gives the book, which still has to be
 * appraised to know what it is. One book per table, so one per world.
 */
public class GoldenTableBlock extends Block {
    public static final MapCodec<GoldenTableBlock> CODEC = simpleCodec(GoldenTableBlock::new);
    public static final BooleanProperty BOOK = BooleanProperty.create("book");
    private static final VoxelShape SHAPE = Shapes.or(Block.box(0, 12, 0, 16, 14, 16),
            Block.box(1, 0, 1, 3, 12, 3), Block.box(13, 0, 1, 15, 12, 3), Block.box(1, 0, 13, 3, 12, 15), Block.box(13, 0, 13, 15, 12, 15));

    public GoldenTableBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(BOOK, false));
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(BOOK);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (!state.getValue(BOOK)) {
            if (!level.isClientSide) {
                player.displayClientMessage(Component.translatable("message.overgearedmod.golden_table_empty"), true);
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        level.setBlockAndUpdate(pos, state.setValue(BOOK, false));
        ItemStack book = new ItemStack(ModItems.UNAPPRAISED_BOOK.get());
        Component name = book.getHoverName();
        if (!player.getInventory().add(book)) {
            player.drop(book, false);
        }
        level.playSound(null, pos, SoundEvents.BOOK_PAGE_TURN, SoundSource.BLOCKS, 1.0f, 0.8f);
        if (player instanceof ServerPlayer serverPlayer) {
            NotificationEvents.send(serverPlayer, SystemNotification.Kind.ITEM, List.of(
                    Component.translatable("notification.overgearedmod.item.body", name),
                    Component.translatable("notification.overgearedmod.item.unappraised")));
        }
        return InteractionResult.CONSUME;
    }
}
