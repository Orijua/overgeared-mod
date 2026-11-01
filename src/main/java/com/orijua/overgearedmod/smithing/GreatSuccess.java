package com.orijua.overgearedmod.smithing;

import com.orijua.overgearedmod.Overgearedmod;
import com.orijua.overgearedmod.equipment.ItemRating;
import com.orijua.overgearedmod.goal.GoalTracker;
import com.orijua.overgearedmod.notification.NotificationEvents;
import com.orijua.overgearedmod.notification.SystemNotification;
import com.orijua.overgearedmod.registry.ModDataComponents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/**
 * A great success at the anvil (a "small surprise" of the discovery moments): the piece came out one rating higher than
 * rolled. The system says so in a window, the anvil sparks and a chime plays. Each one counts for the
 * {@link #MILESTONE}, so titles or secret classes can ask for them later.
 */
public final class GreatSuccess {
    public static final ResourceLocation MILESTONE = Overgearedmod.id("great_success");

    private GreatSuccess() {
    }

    public static void celebrate(ServerPlayer smith, ItemStack piece, BlockPos anvil) {
        ItemRating rating = piece.getOrDefault(ModDataComponents.ITEM_RATING, ItemRating.NORMAL);
        GoalTracker.milestone(smith, MILESTONE);
        NotificationEvents.send(smith, SystemNotification.Kind.GREAT_SUCCESS,
                List.of(Component.translatable("notification.overgearedmod.great_success.body", piece.getHoverName(), rating.displayName())));
        smith.level().playSound(null, anvil, SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.BLOCKS, 0.8f, 1.2f);
        smith.serverLevel().sendParticles(ParticleTypes.TOTEM_OF_UNDYING, anvil.getX() + 0.5, anvil.getY() + 1.2, anvil.getZ() + 0.5,
                30, 0.3, 0.3, 0.3, 0.25);
    }
}
