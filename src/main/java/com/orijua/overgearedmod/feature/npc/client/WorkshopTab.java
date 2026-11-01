package com.orijua.overgearedmod.feature.npc.client;

import com.orijua.overgearedmod.core.client.SatisfyTab;
import com.orijua.overgearedmod.core.client.UiStyle;
import com.orijua.overgearedmod.core.registry.ModAttachments;
import com.orijua.overgearedmod.feature.insight.InsightRules;
import com.orijua.overgearedmod.feature.insight.Observation;
import com.orijua.overgearedmod.feature.npc.ApprenticeRules;
import com.orijua.overgearedmod.feature.npc.ApprenticeSmith;
import com.orijua.overgearedmod.feature.npc.Workshop;
import com.orijua.overgearedmod.feature.npc.WorkshopEntry;
import com.orijua.overgearedmod.feature.progression.ProgressionService;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.Optional;

/**
 * The workshop: every apprentice of the player with his stage, what he is doing (or why he stopped), his order and his
 * wage, as last reported by him. His level shows with enough Insight, as in his own window. Opened from the Satisfy
 * window or the workshop ledger.
 */
public class WorkshopTab extends SatisfyTab {
    private static final int ROW = 30;
    private static final int LIST_TOP = 18;

    public WorkshopTab() {
        super(Component.translatable("tab.overgearedmod.workshop"));
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY) {
        List<WorkshopEntry> entries = Workshop.entries(player()).stream()
                .filter(entry -> player().getData(ModAttachments.APPRENTICES).contains(entry.apprentice()))
                .toList();
        int smithing = ProgressionService.effectiveSmithingLevel(player());
        int allowed = ApprenticeRules.allowedApprentices(smithing);
        int hired = player().getData(ModAttachments.APPRENTICES).size();
        int next = ApprenticeRules.levelForApprentice(hired);
        Component header = next < 0 || hired < allowed
                ? Component.translatable("screen.overgearedmod.workshop.count", hired, allowed)
                : Component.translatable("screen.overgearedmod.workshop.count_next", hired, allowed, next);
        UiStyle.heading(graphics, font(), header, left, top);

        if (entries.isEmpty()) {
            graphics.drawWordWrap(font(), Component.translatable("screen.overgearedmod.workshop.empty", ApprenticeRules.FIRST_APPRENTICE_LEVEL),
                    left, top + LIST_TOP + 4, width, UiStyle.MUTED);
            return;
        }
        boolean showLevel = InsightRules.detail(Observation.effectiveInsight(player())) != InsightRules.Detail.LOW;
        for (int i = 0; i < entries.size(); i++) {
            renderEntry(graphics, entries.get(i), top + LIST_TOP + i * ROW, showLevel);
        }
    }

    private void renderEntry(GuiGraphics graphics, WorkshopEntry entry, int y, boolean showLevel) {
        MutableComponent title = Component.literal(entry.name()).withColor(entry.named() ? UiStyle.GOLD : UiStyle.TEXT)
                .append(Component.literal(" · ").withColor(UiStyle.MUTED))
                .append(Component.translatable("npc.overgearedmod.stage." + entry.stage().key()).withColor(UiStyle.MUTED));
        if (entry.craftsman()) {
            title.append(Component.translatable("entity.overgearedmod.apprentice_smith.craftsman_suffix").withColor(UiStyle.GOLD));
        }
        if (showLevel) {
            title.append(Component.translatable("screen.overgearedmod.workshop.level", entry.level()).withColor(UiStyle.MUTED));
        }
        graphics.drawString(font(), title, left + 20, y, UiStyle.TEXT);

        ApprenticeSmith.Status status = entry.statusValue();
        Component statusText = Component.translatable("message.overgearedmod.apprentice_status." + entry.status().toLowerCase());
        int statusColor = switch (status) {
            case WORKING -> UiStyle.GOOD;
            case NO_ORDER -> UiStyle.MUTED;
            default -> UiStyle.BAD;
        };
        graphics.drawString(font(), statusText, left + width - font().width(statusText), y, statusColor);

        Optional<Item> order = Workshop.orderItem(entry);
        order.ifPresent(item -> graphics.renderItem(new ItemStack(item), left, y + 2));
        Component detail = Component.translatable("screen.overgearedmod.workshop.detail",
                order.map(Item::getDescription).orElse(Component.translatable("message.overgearedmod.apprentice_no_order_name")), entry.daysPaid());
        graphics.drawString(font(), font().substrByWidth(detail, width - 20).getString(), left + 20, y + 11,
                entry.daysPaid() > 0 ? UiStyle.MUTED : UiStyle.BAD);
        graphics.fill(left, y + ROW - 6, left + width, y + ROW - 5, UiStyle.LINE_SOFT & 0x40FFFFFF);
    }
}
