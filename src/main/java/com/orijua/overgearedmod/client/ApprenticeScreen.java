package com.orijua.overgearedmod.client;

import com.orijua.overgearedmod.client.ui.SatisfyButton;
import com.orijua.overgearedmod.client.ui.SatisfyConfirmScreen;
import com.orijua.overgearedmod.client.ui.UiStyle;
import com.orijua.overgearedmod.insight.InsightRules;
import com.orijua.overgearedmod.insight.Observation;
import com.orijua.overgearedmod.network.ApprenticeActionPayload;
import com.orijua.overgearedmod.npc.ApprenticeRules;
import com.orijua.overgearedmod.npc.ApprenticeSmith;
import com.orijua.overgearedmod.npc.NpcGrade;
import com.orijua.overgearedmod.smithing.SmithingHammers;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.List;
import java.util.Optional;

/**
 * An apprentice's window (his master right-clicks him empty-handed): what he is doing and why not, his order, his wage
 * and his containers, with a button for each thing that can be done from here. Orders and stacks of emeralds still go
 * by right-clicking him with the item, as the hint at the bottom says. Everything shown is synced entity data, so the
 * window follows him live.
 */
public final class ApprenticeScreen extends Screen {
    private static final int WIDTH = 300;
    private static final int PADDING = 10;
    private static final int ROW = 22;
    private static final int LABEL_WIDTH = 70;
    private static final int BUTTON_WIDTH = 70;
    private static final int ROWS = 8;

    private final ApprenticeSmith apprentice;
    private List<FormattedCharSequence> hint = List.of();
    private Button cancel;
    private Button pay;
    private Button clear;
    private Button hammer;
    private int left;
    private int top;
    private int panelHeight;

    private ApprenticeScreen(ApprenticeSmith apprentice) {
        super(apprentice.getDisplayName());
        this.apprentice = apprentice;
    }

    /** Client only: called from the apprentice's interaction on the client side. */
    public static void open(ApprenticeSmith apprentice) {
        Minecraft.getInstance().setScreen(new ApprenticeScreen(apprentice));
    }

    private int rowY(int row) {
        return top + PADDING + 2 * UiStyle.LINE + 8 + row * ROW;
    }

    private int buttonsY() {
        return rowY(ROWS) + 2;
    }

    @Override
    protected void init() {
        hint = font.split(Component.translatable("screen.overgearedmod.apprentice.hint", ApprenticeRules.DAILY_WAGE), WIDTH - 2 * PADDING);
        top = 0;
        panelHeight = buttonsY() + 18 + 8 + hint.size() * 10 + PADDING;
        left = (width - WIDTH) / 2;
        top = (height - panelHeight) / 2;
        int buttonX = left + WIDTH - PADDING - BUTTON_WIDTH;
        cancel = rowButton(buttonX, 1, "screen.overgearedmod.apprentice.cancel", ApprenticeActionPayload.Action.CANCEL_ORDER);
        pay = rowButton(buttonX, 2, "screen.overgearedmod.apprentice.pay", ApprenticeActionPayload.Action.PAY_DAY);
        pay.setTooltip(Tooltip.create(Component.translatable("screen.overgearedmod.apprentice.pay_tooltip", ApprenticeRules.DAILY_WAGE,
                ApprenticeRules.MAX_PAID_DAYS)));
        // Assigning closes the window so the next right-click can pick the container.
        addRenderableWidget(Button.builder(Component.translatable("screen.overgearedmod.apprentice.assign"), b -> assign(ApprenticeActionPayload.Action.ASSIGN_MATERIALS))
                .bounds(buttonX, rowY(3) - 4, BUTTON_WIDTH, 16).build(SatisfyButton::new));
        addRenderableWidget(Button.builder(Component.translatable("screen.overgearedmod.apprentice.assign"), b -> assign(ApprenticeActionPayload.Action.ASSIGN_OUTPUT))
                .bounds(buttonX, rowY(4) - 4, BUTTON_WIDTH, 16).build(SatisfyButton::new));
        // One button: give him your best hammer, or take his back.
        hammer = addRenderableWidget(Button.builder(Component.empty(), b -> send(apprentice.getMainHandItem().isEmpty()
                        ? ApprenticeActionPayload.Action.GIVE_HAMMER : ApprenticeActionPayload.Action.TAKE_HAMMER))
                .bounds(buttonX, rowY(5) - 4, BUTTON_WIDTH, 16).build(SatisfyButton::new));
        hammer.setTooltip(Tooltip.create(Component.translatable("screen.overgearedmod.apprentice.hammer_tooltip")));
        int bottomWidth = 120;
        clear = addRenderableWidget(Button.builder(Component.translatable("screen.overgearedmod.apprentice.clear"),
                        b -> send(ApprenticeActionPayload.Action.CLEAR_CONTAINERS))
                .bounds(left + WIDTH / 2 - bottomWidth - 4, buttonsY(), bottomWidth, 18).build(SatisfyButton::new));
        addRenderableWidget(Button.builder(Component.translatable("screen.overgearedmod.apprentice.dismiss"), b -> confirmDismiss())
                .bounds(left + WIDTH / 2 + 4, buttonsY(), bottomWidth, 18).build(SatisfyButton::new));
        updateButtons();
    }

    private Button rowButton(int x, int row, String key, ApprenticeActionPayload.Action action) {
        return addRenderableWidget(Button.builder(Component.translatable(key), b -> send(action))
                .bounds(x, rowY(row) - 4, BUTTON_WIDTH, 16).build(SatisfyButton::new));
    }

    private void send(ApprenticeActionPayload.Action action) {
        PacketDistributor.sendToServer(new ApprenticeActionPayload(apprentice.getId(), action));
    }

    private void assign(ApprenticeActionPayload.Action action) {
        send(action);
        onClose();
    }

    private void confirmDismiss() {
        minecraft.setScreen(new SatisfyConfirmScreen(this, Component.translatable("screen.overgearedmod.apprentice.dismiss_title", apprentice.getDisplayName()),
                Component.translatable("screen.overgearedmod.apprentice.dismiss_message"), () -> send(ApprenticeActionPayload.Action.DISMISS)));
    }

    private void updateButtons() {
        cancel.active = !apprentice.shownOrder().isEmpty();
        pay.active = apprentice.shownDaysPaid() < ApprenticeRules.MAX_PAID_DAYS
                && minecraft.player.getInventory().countItem(Items.EMERALD) >= ApprenticeRules.DAILY_WAGE;
        clear.active = apprentice.assigned(ApprenticeSmith.Slot.MATERIALS).isPresent() || apprentice.assigned(ApprenticeSmith.Slot.OUTPUT).isPresent();
        boolean hasHammer = !apprentice.getMainHandItem().isEmpty();
        hammer.setMessage(Component.translatable(hasHammer ? "screen.overgearedmod.apprentice.hammer_take" : "screen.overgearedmod.apprentice.hammer_give"));
        hammer.active = hasHammer || SmithingHammers.bestSlot(minecraft.level.registryAccess(), minecraft.player.getInventory()) >= 0;
    }

    /** Closes when he is gone or left behind; the buttons follow his synced state. */
    @Override
    public void tick() {
        if (!apprentice.isAlive() || apprentice.distanceToSqr(minecraft.player) > ApprenticeSmith.WINDOW_REACH * ApprenticeSmith.WINDOW_REACH) {
            onClose();
            return;
        }
        updateButtons();
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.renderBackground(graphics, mouseX, mouseY, partialTick);
        UiStyle.panel(graphics, left, top, WIDTH, panelHeight);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        int centerX = left + WIDTH / 2;
        graphics.drawCenteredString(font, title, centerX, top + PADDING, UiStyle.HEADING);
        Component role = Component.translatable(apprentice.isCraftsman() ? "screen.overgearedmod.apprentice.craftsman" : "entity.overgearedmod.apprentice_smith");
        Component stage = Component.translatable("npc.overgearedmod.stage." + apprentice.shownStage().key());
        int insight = Observation.effectiveInsight(minecraft.player);
        InsightRules.Detail detail = InsightRules.detail(insight);
        String level = detail == InsightRules.Detail.LOW ? "??" : String.valueOf(apprentice.shownLevel());
        graphics.drawCenteredString(font, Component.translatable("screen.overgearedmod.apprentice.subtitle", role, stage, level),
                centerX, top + PADDING + UiStyle.LINE, apprentice.isCraftsman() ? UiStyle.GOLD : UiStyle.MUTED);
        int lineY = top + PADDING + 2 * UiStyle.LINE + 2;
        graphics.fill(left + PADDING, lineY, left + WIDTH - PADDING, lineY + 1, UiStyle.LINE_SOFT);

        ApprenticeSmith.Status status = apprentice.status();
        int statusColor = switch (status) {
            case WORKING -> UiStyle.GOOD;
            case NO_ORDER -> UiStyle.MUTED;
            default -> UiStyle.BAD;
        };
        row(graphics, 0, "status", Component.translatable("message.overgearedmod.apprentice_status." + status.name().toLowerCase()), statusColor);

        ItemStack order = apprentice.shownOrder();
        if (order.isEmpty()) {
            row(graphics, 1, "order", Component.translatable("message.overgearedmod.apprentice_no_order_name"), UiStyle.MUTED);
        } else {
            int x = left + PADDING + LABEL_WIDTH;
            graphics.renderItem(order, x, rowY(1) - 4);
            label(graphics, 1, "order");
            graphics.drawString(font, clip(order.getHoverName(), valueWidth() - 18), x + 18, rowY(1), UiStyle.TEXT);
        }
        row(graphics, 2, "wage", Component.translatable("screen.overgearedmod.apprentice.wage_days", apprentice.shownDaysPaid()),
                apprentice.shownDaysPaid() > 0 ? UiStyle.TEXT : UiStyle.BAD);
        row(graphics, 3, "materials", container(ApprenticeSmith.Slot.MATERIALS, "screen.overgearedmod.apprentice.materials_default"), UiStyle.TEXT);
        row(graphics, 4, "output", container(ApprenticeSmith.Slot.OUTPUT, "screen.overgearedmod.apprentice.output_default"), UiStyle.TEXT);

        ItemStack held = apprentice.getMainHandItem();
        if (held.isEmpty()) {
            row(graphics, 5, "hammer", Component.translatable("screen.overgearedmod.apprentice.no_hammer"), UiStyle.MUTED);
        } else {
            int x = left + PADDING + LABEL_WIDTH;
            graphics.renderItem(held, x, rowY(5) - 4);
            label(graphics, 5, "hammer");
            Component text = held.getHoverName().copy().append(" " + SmithingHammers.format(SmithingHammers.skillBonus(minecraft.level.registryAccess(), held)));
            graphics.drawString(font, clip(text, valueWidth() - 18), x + 18, rowY(5), UiStyle.TEXT);
        }

        // Character Observation: how much more is revealed depends on the player's Insight.
        if (detail == InsightRules.Detail.LOW) {
            wideRow(graphics, 6, "work", hidden(insight, InsightRules.MEDIUM), UiStyle.MUTED);
        } else {
            int failPercent = (int) Math.round(ApprenticeRules.failChance(apprentice.shownLevel()) * 100);
            wideRow(graphics, 6, "work", Component.translatable("screen.overgearedmod.apprentice.work_value", failPercent,
                    ApprenticeRules.ticksPerPiece(apprentice.shownLevel()) / 20), UiStyle.TEXT);
        }
        if (detail == InsightRules.Detail.HIGH) {
            NpcGrade grade = apprentice.grade();
            Component cap = grade.levelCap() == Integer.MAX_VALUE ? Component.translatable("screen.overgearedmod.apprentice.no_cap")
                    : Component.literal(String.valueOf(grade.levelCap()));
            wideRow(graphics, 7, "grade", Component.translatable("screen.overgearedmod.apprentice.grade_value",
                    Component.translatable("npc.overgearedmod.grade." + grade.name().toLowerCase()), cap), grade == NpcGrade.NAMED ? UiStyle.GOLD : UiStyle.TEXT);
        } else {
            wideRow(graphics, 7, "grade", hidden(insight, InsightRules.HIGH), UiStyle.MUTED);
        }

        int hintY = buttonsY() + 18 + 8;
        for (FormattedCharSequence line : hint) {
            graphics.drawString(font, line, left + PADDING, hintY, UiStyle.MUTED);
            hintY += 10;
        }
    }

    private int valueWidth() {
        return WIDTH - 2 * PADDING - LABEL_WIDTH - BUTTON_WIDTH - 6;
    }

    private void label(GuiGraphics graphics, int row, String key) {
        graphics.drawString(font, Component.translatable("screen.overgearedmod.apprentice." + key), left + PADDING, rowY(row), UiStyle.MUTED);
    }

    private void row(GuiGraphics graphics, int row, String key, Component value, int color) {
        label(graphics, row, key);
        graphics.drawString(font, clip(value, valueWidth()), left + PADDING + LABEL_WIDTH, rowY(row), color);
    }

    /** A row with no button: its value may use the whole width. */
    private void wideRow(GuiGraphics graphics, int row, String key, Component value, int color) {
        label(graphics, row, key);
        graphics.drawString(font, clip(value, WIDTH - 2 * PADDING - LABEL_WIDTH), left + PADDING + LABEL_WIDTH, rowY(row), color);
    }

    /** What stays hidden until the player's Insight reaches {@code needed}. */
    private static Component hidden(int insight, int needed) {
        return Component.translatable("screen.overgearedmod.apprentice.hidden", insight, needed);
    }

    private String clip(Component text, int maxWidth) {
        return font.substrByWidth(text, maxWidth).getString();
    }

    /** The assigned container's block and position, or where he looks without one. */
    private Component container(ApprenticeSmith.Slot slot, String defaultKey) {
        Optional<BlockPos> pos = apprentice.assigned(slot);
        return pos.<Component>map(at -> Component.translatable("screen.overgearedmod.apprentice.at",
                        apprentice.level().getBlockState(at).getBlock().getName(), at.getX(), at.getY(), at.getZ()))
                .orElse(Component.translatable(defaultKey));
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
