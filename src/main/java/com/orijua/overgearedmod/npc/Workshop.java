package com.orijua.overgearedmod.npc;

import com.orijua.overgearedmod.registry.ModAttachments;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * A smith's workshop: the snapshot of each of his apprentices that the Workshop tab shows, kept on the master (synced
 * to him) and refreshed by every apprentice while both are around. When one who was working stops (unpaid, out of
 * materials, no room, no anvil), his master is told in the chat.
 */
public final class Workshop {
    private Workshop() {
    }

    public static List<WorkshopEntry> entries(Player master) {
        return master.getData(ModAttachments.WORKSHOP);
    }

    /** Refreshes {@code apprentice}'s entry (only written when something changed) and warns if he just stopped. */
    public static void report(ServerPlayer master, ApprenticeSmith apprentice) {
        WorkshopEntry entry = new WorkshopEntry(apprentice.getUUID(), apprentice.baseName(), apprentice.grade() == NpcGrade.NAMED,
                apprentice.isCraftsman(), apprentice.smithingLevel(), apprentice.status().name(),
                apprentice.order().map(BuiltInRegistries.ITEM::getKey), apprentice.daysPaid());
        List<WorkshopEntry> entries = new ArrayList<>(entries(master));
        Optional<WorkshopEntry> previous = entries.stream().filter(e -> e.apprentice().equals(entry.apprentice())).findFirst();
        if (previous.isPresent() && previous.get().equals(entry)) {
            return;
        }
        previous.ifPresentOrElse(old -> entries.set(entries.indexOf(old), entry), () -> entries.add(entry));
        master.setData(ModAttachments.WORKSHOP, List.copyOf(entries));
        if (previous.isPresent() && stopped(previous.get().statusValue(), entry.statusValue())) {
            master.sendSystemMessage(Component.translatable("message.overgearedmod.workshop_stopped", apprentice.getDisplayName(),
                    apprentice.statusText()).withStyle(ChatFormatting.GOLD));
        }
    }

    /** Whether going from {@code before} to {@code after} is an apprentice stopping on his own (not his order cancelled). */
    static boolean stopped(ApprenticeSmith.Status before, ApprenticeSmith.Status after) {
        return before == ApprenticeSmith.Status.WORKING && after != ApprenticeSmith.Status.WORKING && after != ApprenticeSmith.Status.NO_ORDER;
    }

    public static void forget(ServerPlayer master, UUID apprentice) {
        List<WorkshopEntry> entries = new ArrayList<>(entries(master));
        if (entries.removeIf(entry -> entry.apprentice().equals(apprentice))) {
            master.setData(ModAttachments.WORKSHOP, List.copyOf(entries));
        }
    }

    /** The ordered item's name, for the tab. */
    public static Optional<Item> orderItem(WorkshopEntry entry) {
        return entry.order().flatMap(BuiltInRegistries.ITEM::getOptional);
    }
}
