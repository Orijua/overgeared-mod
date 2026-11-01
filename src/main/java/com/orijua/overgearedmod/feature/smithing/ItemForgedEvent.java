package com.orijua.overgearedmod.feature.smithing;

import com.orijua.overgearedmod.core.event.SatisfyPlayerEvent;
import com.orijua.overgearedmod.feature.equipment.ItemRating;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

/** A player took a rated item out of the smithing anvil. */
public final class ItemForgedEvent extends SatisfyPlayerEvent {
    private final ItemStack forged;
    private final ItemRating rating;

    public ItemForgedEvent(ServerPlayer player, ItemStack forged, ItemRating rating) {
        super(player);
        this.forged = forged;
        this.rating = rating;
    }

    public ItemStack forged() {
        return forged;
    }

    public ItemRating rating() {
        return rating;
    }
}
