package com.orijua.overgearedmod.feature.economy.client;

import com.orijua.overgearedmod.core.client.SatisfyButton;
import com.orijua.overgearedmod.core.client.SatisfyConfirmScreen;
import com.orijua.overgearedmod.core.client.SatisfyTab;
import com.orijua.overgearedmod.core.client.ScrollList;
import com.orijua.overgearedmod.core.client.UiStyle;
import com.orijua.overgearedmod.core.config.TradeBalance;
import com.orijua.overgearedmod.core.registry.ModAttachments;
import com.orijua.overgearedmod.core.registry.ModDataComponents;
import com.orijua.overgearedmod.core.registry.ModItems;
import com.orijua.overgearedmod.feature.economy.TradeGoods;
import com.orijua.overgearedmod.feature.economy.TradeMode;
import com.orijua.overgearedmod.feature.economy.TradeRules;
import com.orijua.overgearedmod.feature.economy.TradeSaturation;
import com.orijua.overgearedmod.feature.economy.TradeSellPayload;
import com.orijua.overgearedmod.feature.economy.TradeService;
import com.orijua.overgearedmod.feature.equipment.ItemRating;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * The Trade tab: the equipment in the inventory with what it pays if sold (Satisfy emeralds) or exported (Won), and the
 * two buttons that do it. The prices are the server's own rules read here to show them; the server decides again when
 * the order arrives. From epic up, both ask to confirm.
 */
public class TradeTab extends SatisfyTab {
    private static final int ROW = 24;
    private static final int LIST_TOP = 16;
    private static final int BUTTON_ROW = 22;
    private static final int BAR_SPACE = 6;
    private static final int BUTTON_HEIGHT = 18;
    private static final int ROW_FILL = 0x30FFFFFF;
    private static final int ROW_HOVER = 0x50FFFFFF;
    private static final int ROW_SELECTED = 0x70A0C8FF;

    private final ScrollList scroll = new ScrollList(4);
    private int selected = -1;
    private ItemStack shownStack = ItemStack.EMPTY;
    private TradeSaturation shownSaturation = TradeSaturation.EMPTY;

    public TradeTab() {
        super(Component.translatable("tab.overgearedmod.trade"));
    }

    /** Slots 0 to 35 that hold something: the hotbar (0 to 8) first, then the rest of the inventory. */
    private static List<Integer> slots() {
        Inventory inventory = player().getInventory();
        List<Integer> slots = new ArrayList<>();
        for (int slot = 0; slot < Inventory.INVENTORY_SIZE; slot++) {
            if (!inventory.getItem(slot).isEmpty()) {
                slots.add(slot);
            }
        }
        return slots;
    }

    private static long today() {
        return TradeRules.day(Minecraft.getInstance().level.getGameTime());
    }

    private int visibleRows() {
        return Math.max(1, (height - LIST_TOP - BUTTON_ROW) / ROW);
    }

    private int rowTop(int visibleIndex) {
        return top + LIST_TOP + visibleIndex * ROW;
    }

    private boolean hovered(double mouseX, double mouseY, int y) {
        return mouseX >= left && mouseX < left + width - BAR_SPACE && mouseY >= y && mouseY < y + ROW - 2;
    }

    private boolean sellableSelection() {
        if (selected < 0) {
            return false;
        }
        ItemStack stack = player().getInventory().getItem(selected);
        return !stack.isEmpty() && TradeGoods.refusal(stack).isEmpty();
    }

    @Override
    protected void addWidgets(Consumer<AbstractWidget> addWidget) {
        shownStack = selected < 0 ? ItemStack.EMPTY : player().getInventory().getItem(selected).copy();
        shownSaturation = TradeService.saturation(player());
        boolean active = sellableSelection();
        int buttonWidth = (width - BAR_SPACE - 4) / 2;
        int y = top + height - BUTTON_HEIGHT;
        addWidget.accept(button("screen.overgearedmod.trade.sell", TradeMode.SELL, left, y, buttonWidth, active));
        addWidget.accept(button("screen.overgearedmod.trade.export", TradeMode.EXPORT, left + buttonWidth + 4, y, buttonWidth, active));
    }

    private Button button(String key, TradeMode mode, int x, int y, int buttonWidth, boolean active) {
        Button button = Button.builder(Component.translatable(key), b -> press(mode))
                .bounds(x, y, buttonWidth, BUTTON_HEIGHT)
                .build(SatisfyButton::new);
        button.active = active;
        return button;
    }

    private void press(TradeMode mode) {
        if (!sellableSelection()) {
            return;
        }
        int slot = selected;
        ItemStack stack = player().getInventory().getItem(slot);
        Runnable send = () -> {
            PacketDistributor.sendToServer(new TradeSellPayload(slot, mode));
            selected = -1;
        };
        ItemRating rating = stack.get(ModDataComponents.ITEM_RATING);
        if (rating == null || rating.ordinal() < ItemRating.EPIC.ordinal()) {
            send.run();
            return;
        }
        long price = TradeGoods.price(stack, mode, shownSaturation, today(), TradeBalance.current());
        Component message = Component.translatable(mode == TradeMode.SELL
                ? "screen.overgearedmod.trade.confirm_sell" : "screen.overgearedmod.trade.confirm_export", price);
        Minecraft minecraft = Minecraft.getInstance();
        minecraft.setScreen(new SatisfyConfirmScreen(minecraft.screen,
                Component.translatable("screen.overgearedmod.trade.confirm_title", stack.getHoverName()), message, send));
    }

    @Override
    public boolean mouseScrolled(double wheel) {
        scroll.setVisibleRows(visibleRows());
        return scroll.scroll(wheel, slots().size());
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY) {
        List<Integer> slots = slots();
        scroll.setVisibleRows(visibleRows());
        scroll.clamp(slots.size());
        for (int i = scroll.first(); i < scroll.end(slots.size()); i++) {
            if (hovered(mouseX, mouseY, rowTop(i - scroll.first()))) {
                selected = slots.get(i);
                return true;
            }
        }
        return false;
    }

    /** Rebuild (the buttons) when the selected slot holds something else or the saturation changed. */
    @Override
    public boolean needsRebuild() {
        if (selected >= 0 && !ItemStack.matches(player().getInventory().getItem(selected), shownStack)) {
            selected = -1;
            return true;
        }
        return !TradeService.saturation(player()).equals(shownSaturation);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY) {
        long won = player().getData(ModAttachments.ACCOUNT.get()).won();
        int emeralds = player().getInventory().countItem(ModItems.SATISFY_EMERALD.get());
        UiStyle.heading(graphics, font(), Component.translatable("screen.overgearedmod.trade.balance", won, emeralds), left, top);

        List<Integer> slots = slots();
        if (slots.isEmpty()) {
            graphics.drawWordWrap(font(), Component.translatable("screen.overgearedmod.trade.empty"),
                    left, top + LIST_TOP + 4, width, UiStyle.MUTED);
            return;
        }
        scroll.setVisibleRows(visibleRows());
        scroll.clamp(slots.size());
        TradeBalance balance = TradeBalance.current();
        TradeSaturation saturation = TradeService.saturation(player());
        long today = today();
        List<Component> tooltip = null;
        for (int i = scroll.first(); i < scroll.end(slots.size()); i++) {
            int y = rowTop(i - scroll.first());
            int slot = slots.get(i);
            ItemStack stack = player().getInventory().getItem(slot);
            boolean hover = hovered(mouseX, mouseY, y);
            List<Component> tip = renderRow(graphics, stack, y, slot == selected, hover, saturation, today, balance);
            if (hover && tip != null) {
                tooltip = tip;
            }
        }
        scroll.renderBar(graphics, left + width - 3, top + LIST_TOP, visibleRows() * ROW - 2, slots.size());
        if (tooltip != null) {
            graphics.renderComponentTooltip(font(), tooltip, mouseX, mouseY);
        }
    }

    /** Draws one two-line row; returns the lines its tooltip would show, or null for none. */
    private List<Component> renderRow(GuiGraphics graphics, ItemStack stack, int y, boolean isSelected, boolean hover,
                                      TradeSaturation saturation, long today, TradeBalance balance) {
        int rowWidth = width - BAR_SPACE;
        graphics.fill(left, y, left + rowWidth, y + ROW - 2, isSelected ? ROW_SELECTED : hover ? ROW_HOVER : ROW_FILL);
        graphics.renderItem(stack, left + 3, y + 3);

        Optional<TradeRules.Refusal> refusal = TradeGoods.refusal(stack);
        ItemRating rating = stack.get(ModDataComponents.ITEM_RATING);
        int nameColor = refusal.isPresent() || rating == null ? UiStyle.MUTED : 0xFF000000 | colorOf(rating);
        int textX = left + 23;
        int maxWidth = rowWidth - 23 - 4;
        graphics.drawString(font(), UiStyle.ellipsize(font(), stack.getHoverName().copy().withColor(nameColor), maxWidth),
                textX, y + 3, nameColor);

        if (refusal.isPresent()) {
            graphics.drawString(font(), UiStyle.ellipsize(font(), refusal.get().message(), maxWidth), textX, y + 13, UiStyle.MUTED);
            return List.of(refusal.get().message());
        }
        long sell = TradeGoods.price(stack, TradeMode.SELL, saturation, today, balance);
        long export = TradeGoods.price(stack, TradeMode.EXPORT, saturation, today, balance);
        Component prices = Component.translatable("screen.overgearedmod.trade.sell_price", sell)
                .append(Component.literal(" · "))
                .append(Component.translatable("screen.overgearedmod.trade.export_price", export));
        graphics.drawString(font(), prices, textX, y + 13, UiStyle.TEXT);
        double factor = TradeRules.saturation(saturation.pressure(TradeGoods.key(stack), today, balance), balance);
        if (factor < 1.0) {
            int percent = (int) Math.round(factor * 100.0);
            graphics.drawString(font(), Component.translatable("screen.overgearedmod.trade.saturation", percent),
                    textX + font().width(prices) + 4, y + 13, UiStyle.BAD);
            return List.of(Component.translatable("screen.overgearedmod.trade.tip.saturation"));
        }
        return null;
    }

    private static int colorOf(ItemRating rating) {
        Integer color = rating.color().getColor();
        return color == null ? UiStyle.TEXT : color;
    }
}
