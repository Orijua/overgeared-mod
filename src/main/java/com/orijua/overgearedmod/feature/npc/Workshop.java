package com.orijua.overgearedmod.feature.npc;

import com.orijua.overgearedmod.core.registry.ModAttachments;
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
    /** How many "out of reach" lines exist in the language files (message.overgearedmod.workshop_idle.N). */
    public static final int IDLE_LINES = 3;

    private Workshop() {
    }

    public static List<WorkshopEntry> entries(Player master) {
        return master.getData(ModAttachments.WORKSHOP);
    }

    /** Refreshes {@code apprentice}'s entry (only written when something changed) and warns if he just stopped. */
    public static void report(ServerPlayer master, ApprenticeSmith apprentice) {
        long now = apprentice.level().getGameTime();
        WorkshopEntry entry = new WorkshopEntry(apprentice.getUUID(), apprentice.baseName(), apprentice.grade() == NpcGrade.NAMED,
                apprentice.isCraftsman(), apprentice.smithingLevel(), apprentice.status().name(),
                apprentice.order().map(BuiltInRegistries.ITEM::getKey), apprentice.daysPaid(),
                apprentice.shownProgress(),
                apprentice.shownMissing().isEmpty() ? Optional.empty() : Optional.of(BuiltInRegistries.ITEM.getKey(apprentice.shownMissing().getItem())),
                apprentice.shownMissing().getCount(), now);
        List<WorkshopEntry> entries = new ArrayList<>(entries(master));
        Optional<WorkshopEntry> previous = entries.stream().filter(e -> e.apprentice().equals(entry.apprentice())).findFirst();
        if (previous.isPresent() && previous.get().sameNewsAs(entry) && now - previous.get().reportedAt() < WorkshopEntry.REFRESH_TICKS) {
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

    /** His status in words, as his window shows it: what he lacks, or how far the piece is while he works. */
    public static Component statusText(WorkshopEntry entry) {
        return statusText(entry.statusValue(), entry.progress(), entry.missingCount(), missingItem(entry).map(Item::getDescription).orElse(null));
    }

    /**
     * The one place that words an apprentice's status: his window, the Workshop tab and the chat warning all use it, so
     * they always agree. {@code missingName} is null when he lacks nothing.
     */
    public static Component statusText(ApprenticeSmith.Status status, int progress, int missingCount, Component missingName) {
        if (status == ApprenticeSmith.Status.NO_MATERIALS && missingName != null) {
            return Component.translatable("message.overgearedmod.apprentice_status.no_materials_item", missingCount, missingName);
        }
        if (showsProgress(status, progress)) {
            return Component.translatable("message.overgearedmod.apprentice_status.working_progress", progress);
        }
        return Component.translatable("message.overgearedmod.apprentice_status." + status.name().toLowerCase());
    }

    /** Whether the piece's progress is worth showing (as a bar or in the text): he is forging and it has started. */
    public static boolean showsProgress(ApprenticeSmith.Status status, int progress) {
        return status == ApprenticeSmith.Status.WORKING && progress > 0;
    }

    /** How many of {@code entries} are forging right now (those out of reach, whose news is old, are not counted). */
    public static int working(List<WorkshopEntry> entries, long now) {
        return (int) entries.stream().filter(e -> !e.isStale(now) && e.statusValue() == ApprenticeSmith.Status.WORKING).count();
    }

    /** How many of {@code entries} stopped on their own with an order (the ones that need their master). */
    public static int stoppedCount(List<WorkshopEntry> entries, long now) {
        return (int) entries.stream().filter(e -> !e.isStale(now) && e.statusValue().stopped()).count();
    }

    /** What the Workshop says of an apprentice out of reach: a line of colour instead of news that may be old. */
    public static Component staleText(WorkshopEntry entry) {
        return Component.translatable("message.overgearedmod.workshop_idle." + Math.floorMod(entry.apprentice().hashCode(), IDLE_LINES));
    }

    /** His status as the Workshop shows it at game time {@code now}. */
    public static Component displayStatus(WorkshopEntry entry, long now) {
        return entry.isStale(now) ? staleText(entry) : statusText(entry);
    }

    /** The ordered item's name, for the tab. */
    public static Optional<Item> orderItem(WorkshopEntry entry) {
        return entry.order().flatMap(BuiltInRegistries.ITEM::getOptional);
    }

    /** The item he lacks for the next piece, for the tab's tooltip. */
    public static Optional<Item> missingItem(WorkshopEntry entry) {
        return entry.missing().flatMap(BuiltInRegistries.ITEM::getOptional);
    }
}
