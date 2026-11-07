package com.orijua.overgearedmod.feature.capsule;

import com.orijua.overgearedmod.core.registry.ModAttachments;
import com.orijua.overgearedmod.core.registry.ModItems;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

/**
 * The first time a player joins a world they get one basic capsule, so the start takes minutes and not a vanilla
 * mining trip. In the canon Grid bought his capsule in debt (ch. 1); here the debt is only flavor text. The flag is
 * saved with the player and survives death, so it is given once. The recipe stays for extra capsules.
 */
public final class StarterCapsule {
    private StarterCapsule() {
    }

    public static void register(IEventBus gameBus) {
        gameBus.addListener(StarterCapsule::onLoggedIn);
    }

    private static void onLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            giveIfFirstTime(player);
        }
    }

    /** Gives the capsule and tells the player the first time only; returns whether it was given now. */
    public static boolean giveIfFirstTime(ServerPlayer player) {
        if (player.getData(ModAttachments.STARTER_CAPSULE_GIVEN.get())) {
            return false;
        }
        player.setData(ModAttachments.STARTER_CAPSULE_GIVEN.get(), true);
        ItemStack capsule = new ItemStack(ModItems.CAPSULE.get());
        if (!player.getInventory().add(capsule)) {
            player.drop(capsule, false);
        }
        player.sendSystemMessage(Component.translatable("message.overgearedmod.capsule.starter"));
        return true;
    }
}
