package com.orijua.overgearedmod.feature.npc.client;

import com.orijua.overgearedmod.core.client.ScrollList;
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
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * The workshop: every apprentice of the player as a card with his stage, what he is doing (or why he stopped), a bar
 * with the piece on the anvil, his order and his wage, as last reported by him. A tooltip gives what is cut short, and a
 * click opens his window when he is at hand. His level shows with enough Insight, as in his own window. Opened from the
 * Satisfy window or the workshop ledger.
 */
public class WorkshopTab extends SatisfyTab {
    private static final int ROW = 34;
    private static final int CARD = 32;
    private static final int LIST_TOP = 26;
    private static final int BAR_SPACE = 6;
    private static final int STRIPE = 2;
    private static final int CARD_FILL = 0x30FFFFFF;
    private static final int CARD_HOVER = 0x50FFFFFF;

    private final ScrollList scroll = new ScrollList(4);

    public WorkshopTab() {
        super(Component.translatable("tab.overgearedmod.workshop"));
    }

    private List<WorkshopEntry> entries() {
        return Workshop.entries(player()).stream()
                .filter(entry -> player().getData(ModAttachments.APPRENTICES).contains(entry.apprentice()))
                .toList();
    }

    private int visibleRows() {
        return Math.max(1, (height - LIST_TOP) / ROW);
    }

    private int cardWidth() {
        return width - BAR_SPACE;
    }

    private int cardTop(int visibleIndex) {
        return top + LIST_TOP + visibleIndex * ROW;
    }

    @Override
    public boolean mouseScrolled(double wheel) {
        scroll.setVisibleRows(visibleRows());
        return scroll.scroll(wheel, entries().size());
    }

    /** A click on a card opens that apprentice's window if he is near; otherwise the system says he is far. */
    @Override
    public boolean mouseClicked(double mouseX, double mouseY) {
        List<WorkshopEntry> entries = entries();
        scroll.setVisibleRows(visibleRows());
        scroll.clamp(entries.size());
        for (int i = scroll.first(); i < scroll.end(entries.size()); i++) {
            if (hovered(mouseX, mouseY, cardTop(i - scroll.first()))) {
                open(entries.get(i));
                return false;
            }
        }
        return false;
    }

    private void open(WorkshopEntry entry) {
        Minecraft minecraft = Minecraft.getInstance();
        for (Entity entity : minecraft.level.entitiesForRendering()) {
            if (entity instanceof ApprenticeSmith apprentice && apprentice.getUUID().equals(entry.apprentice()) && apprentice.isAlive()
                    && apprentice.isOwnedBy(player())
                    && apprentice.distanceToSqr(player()) <= ApprenticeSmith.WINDOW_REACH * ApprenticeSmith.WINDOW_REACH) {
                ApprenticeScreen.open(apprentice);
                return;
            }
        }
        player().displayClientMessage(Component.translatable("message.overgearedmod.workshop_far", entry.name()), false);
    }

    private boolean hovered(double mouseX, double mouseY, int y) {
        return mouseX >= left && mouseX < left + cardWidth() && mouseY >= y && mouseY < y + CARD;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY) {
        List<WorkshopEntry> entries = entries();
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
        int stopped = Workshop.stoppedCount(entries);
        graphics.drawString(font(), Component.translatable("screen.overgearedmod.workshop.totals", Workshop.working(entries), stopped),
                left, top + UiStyle.LINE, stopped > 0 ? UiStyle.BAD : UiStyle.MUTED);

        scroll.setVisibleRows(visibleRows());
        scroll.clamp(entries.size());
        boolean showLevel = InsightRules.detail(Observation.effectiveInsight(player())) != InsightRules.Detail.LOW;
        WorkshopEntry tooltipEntry = null;
        for (int i = scroll.first(); i < scroll.end(entries.size()); i++) {
            int y = cardTop(i - scroll.first());
            boolean hover = hovered(mouseX, mouseY, y);
            renderCard(graphics, entries.get(i), y, hover, showLevel);
            if (hover) {
                tooltipEntry = entries.get(i);
            }
        }
        scroll.renderBar(graphics, left + width - 3, top + LIST_TOP, visibleRows() * ROW - 2, entries.size());
        if (tooltipEntry != null) {
            graphics.renderComponentTooltip(font(), tooltip(tooltipEntry), mouseX, mouseY);
        }
    }

    private void renderCard(GuiGraphics graphics, WorkshopEntry entry, int y, boolean hover, boolean showLevel) {
        int cardWidth = cardWidth();
        ApprenticeSmith.Status status = entry.statusValue();
        int statusColor = switch (status) {
            case WORKING -> UiStyle.GOOD;
            case NO_ORDER -> UiStyle.MUTED;
            default -> UiStyle.BAD;
        };
        graphics.fill(left, y, left + cardWidth, y + CARD, hover ? CARD_HOVER : CARD_FILL);
        graphics.fill(left, y, left + STRIPE, y + CARD, 0xFF000000 | statusColor);

        Optional<Item> order = Workshop.orderItem(entry);
        order.ifPresent(item -> graphics.renderItem(new ItemStack(item), left + 6, y + 8));

        int textX = left + 26;
        Component statusText = Workshop.statusText(entry);
        int statusWidth = font().width(statusText) + (status.stopped() ? font().width("! ") : 0);
        if (status.stopped()) {
            graphics.drawString(font(), "!", left + cardWidth - statusWidth - 3, y + 3, UiStyle.BAD);
        }
        graphics.drawString(font(), statusText, left + cardWidth - font().width(statusText) - 3, y + 3, statusColor);

        MutableComponent title = Component.literal(entry.name()).withColor(entry.named() ? UiStyle.GOLD : UiStyle.TEXT)
                .append(Component.literal(" · ").withColor(UiStyle.MUTED))
                .append(Component.translatable("npc.overgearedmod.stage." + entry.stage().key()).withColor(UiStyle.MUTED));
        if (entry.craftsman()) {
            title.append(Component.translatable("entity.overgearedmod.apprentice_smith.craftsman_suffix").withColor(UiStyle.GOLD));
        }
        if (showLevel) {
            title.append(Component.translatable("screen.overgearedmod.workshop.level", entry.level()).withColor(UiStyle.MUTED));
        }
        graphics.drawString(font(), UiStyle.ellipsize(font(), title, cardWidth - 26 - statusWidth - 10), textX, y + 3, UiStyle.TEXT);

        Component detail = Component.translatable("screen.overgearedmod.workshop.detail",
                order.map(Item::getDescription).orElse(Component.translatable("message.overgearedmod.apprentice_no_order_name")), entry.daysPaid());
        graphics.drawString(font(), UiStyle.ellipsize(font(), detail, cardWidth - 26 - 4), textX, y + 14,
                entry.daysPaid() > 0 ? UiStyle.MUTED : UiStyle.BAD);

        if (status == ApprenticeSmith.Status.WORKING) {
            // The piece on the anvil, filling up along the bottom of the card.
            UiStyle.bar(graphics, textX, y + 25, cardWidth - 26 - 6, 3, entry.progress() / 100f, UiStyle.GOOD);
        }
    }

    /** What the cards cut short: the full order, the wage and what he lacks. */
    private List<Component> tooltip(WorkshopEntry entry) {
        List<Component> lines = new ArrayList<>();
        lines.add(Component.literal(entry.name()).withColor(entry.named() ? UiStyle.GOLD : UiStyle.TEXT));
        lines.add(Workshop.statusText(entry));
        Component order = Workshop.orderItem(entry).map(Item::getDescription)
                .orElse(Component.translatable("message.overgearedmod.apprentice_no_order_name"));
        lines.add(Component.translatable("screen.overgearedmod.workshop.tip.order", order).withColor(UiStyle.MUTED));
        lines.add(Component.translatable("screen.overgearedmod.workshop.tip.wage", entry.daysPaid())
                .withColor(entry.daysPaid() > 0 ? UiStyle.MUTED : UiStyle.BAD));
        Workshop.missingItem(entry).ifPresent(item -> lines.add(Component.translatable("screen.overgearedmod.workshop.tip.missing",
                entry.missingCount(), item.getDescription()).withColor(UiStyle.BAD)));
        lines.add(Component.translatable("screen.overgearedmod.workshop.tip.open").withColor(UiStyle.MUTED));
        return lines;
    }
}
