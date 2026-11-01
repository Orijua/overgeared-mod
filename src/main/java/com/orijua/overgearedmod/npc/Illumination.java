package com.orijua.overgearedmod.npc;

import com.orijua.overgearedmod.equipment.ItemRating;
import com.orijua.overgearedmod.event.ItemForgedEvent;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.IEventBus;

/**
 * Illumination (canon: Khan reaches craftsman after watching Grid work, cap. 768): when a smith forges a Legendary
 * piece, his Master apprentices within {@link ApprenticeRules#ILLUMINATION_REACH} blocks become Craftsmen.
 */
public final class Illumination {
    private Illumination() {
    }

    public static void register(IEventBus gameBus) {
        gameBus.addListener(ItemForgedEvent.class, event -> enlighten(event.player(), event.rating()));
    }

    /** Lets every apprentice of {@code master} nearby judge the piece he just forged. Returns how many were enlightened. */
    public static int enlighten(ServerPlayer master, ItemRating rating) {
        int enlightened = 0;
        for (ApprenticeSmith apprentice : master.serverLevel().getEntitiesOfClass(ApprenticeSmith.class,
                master.getBoundingBox().inflate(ApprenticeRules.ILLUMINATION_REACH), apprentice -> apprentice.isOwnedBy(master))) {
            if (apprentice.judgePiece(rating, false)) {
                enlightened++;
            }
        }
        return enlightened;
    }
}
