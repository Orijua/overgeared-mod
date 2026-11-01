package com.orijua.overgearedmod.feature.smithing;

import com.orijua.overgearedmod.core.event.SatisfyPlayerEvent;
import com.orijua.overgearedmod.feature.equipment.ItemRating;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

/**
 * A player took a rated item out of the smithing anvil. {@code precision} is the manual forge's (0-1), or negative for
 * an automatic one (or a simulated one).
 */
public final class ItemForgedEvent extends SatisfyPlayerEvent {
    private final ItemStack forged;
    private final ItemRating rating;
    private final double precision;

    public ItemForgedEvent(ServerPlayer player, ItemStack forged, ItemRating rating) {
        this(player, forged, rating, -1);
    }

    public ItemForgedEvent(ServerPlayer player, ItemStack forged, ItemRating rating, double precision) {
        super(player);
        this.forged = forged;
        this.rating = rating;
        this.precision = precision;
    }

    public ItemStack forged() {
        return forged;
    }

    public ItemRating rating() {
        return rating;
    }

    /** Whether it was forged by hand, with the minigame. */
    public boolean manual() {
        return precision >= 0;
    }

    public double precision() {
        return precision;
    }
}
