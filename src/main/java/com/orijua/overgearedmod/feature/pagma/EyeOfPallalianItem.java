package com.orijua.overgearedmod.feature.pagma;

import com.orijua.overgearedmod.core.registry.ModItems;
import com.orijua.overgearedmod.feature.notification.NotificationEvents;
import com.orijua.overgearedmod.feature.notification.SystemNotification;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * The Eye of Pallalian (canon, cap. 1: the most expensive appraisal item, which Grid had bought six months before). Used
 * with an unappraised item in the inventory, it is spent to reveal what that item is. For Pagma's old book, that is
 * the moment Earl Ashur comes for it ({@link AshurBetrayal#betray}).
 */
public class EyeOfPallalianItem extends Item {
    public EyeOfPallalianItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack eye = player.getItemInHand(hand);
        if (level.isClientSide || !(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResultHolder.success(eye);
        }
        if (!appraise(serverPlayer)) {
            player.displayClientMessage(Component.translatable("message.overgearedmod.nothing_to_appraise"), true);
            return InteractionResultHolder.fail(eye);
        }
        eye.consume(1, player);
        return InteractionResultHolder.consume(eye);
    }

    /** Appraises the first unappraised book in the player's inventory; false if there is none. Doesn't spend the eye. */
    public static boolean appraise(ServerPlayer player) {
        Inventory inventory = player.getInventory();
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            if (inventory.getItem(slot).is(ModItems.UNAPPRAISED_BOOK.get())) {
                inventory.getItem(slot).shrink(1);
                ItemStack book = new ItemStack(ModItems.PAGMA_RARE_BOOK.get());
                if (!inventory.add(slot, book) && !inventory.add(book)) {
                    player.drop(book, false);
                }
                player.level().playSound(null, player.blockPosition(), SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.PLAYERS, 1.0f, 0.7f);
                NotificationEvents.send(player, SystemNotification.Kind.ITEM, List.of(
                        Component.translatable("notification.overgearedmod.appraised", book.getHoverName()),
                        Component.translatable("item.overgearedmod.pagma_rare_book.effect")));
                AshurBetrayal.betray(player);
                return true;
            }
        }
        return false;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable(getDescriptionId() + ".desc").withStyle(ChatFormatting.GRAY));
    }
}
