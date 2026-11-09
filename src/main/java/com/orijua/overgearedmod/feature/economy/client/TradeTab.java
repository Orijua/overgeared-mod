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
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Consumer;

/**
 * The Trade tab: the equipment in the inventory that can be sold, with check boxes, what each piece pays in Satisfy
 * emeralds and the total of the chosen ones (the pieces of the same kind pay a little less each, as the server will
 * count them). One button sells what is chosen. The prices are the server's own rules read here to show them; the server
 * decides again when the order arrives. If a chosen piece is epic or better, it asks to confirm.
 */
public class TradeTab extends SatisfyTab {
    private static final int ROW = 24;
    private static final int LIST_TOP = 16;
    private static final int BUTTON_ROW = 22;
    private static final int BAR_SPACE = 6;
    private static final int BUTTON_HEIGHT = 18;
    private static final int ALL_WIDTH = 44;
    private static final int NONE_WIDTH = 52;
    private static final int ROW_FILL = 0x30FFFFFF;
    private static final int ROW_HOVER = 0x50FFFFFF;
    private static final int ROW_SELECTED = 0x70A0C8FF;

    private final ScrollList scroll = new ScrollList(4);
    /** The chosen slots with a copy of the piece the player saw in each. */
    private final TreeMap<Integer, ItemStack> chosen = new TreeMap<>();
    /** Price of each chosen slot inside the batch (set in addWidgets, same source as the button total). */
    private final Map<Integer, Long> batchPrices = new java.util.HashMap<>();
    private TradeSaturation shownSaturation = TradeSaturation.EMPTY;
    private boolean dirty;

    public TradeTab() {
        super(Component.translatable("tab.overgearedmod.trade"));
    }

    /** The slots 0 to 35 with a piece that can be sold: the hotbar (0 to 8) first, then the rest of the inventory. */
    private static List<Integer> slots() {
        return TradeGoods.sellableSlots(player().getInventory());
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

    /** Drops the chosen slots whose piece is not the one that was seen any more or cannot be sold; true if any went. */
    private boolean prune() {
        boolean changed = false;
        Inventory inventory = player().getInventory();
        Iterator<Map.Entry<Integer, ItemStack>> it = chosen.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<Integer, ItemStack> entry = it.next();
            ItemStack now = inventory.getItem(entry.getKey());
            if (now.isEmpty() || TradeGoods.refusal(now).isPresent() || !ItemStack.matches(now, entry.getValue())) {
                it.remove();
                changed = true;
            }
        }
        return changed;
    }

    private static List<ItemStack> stacksOf(TreeMap<Integer, ItemStack> chosen) {
        return new ArrayList<>(chosen.values());
    }

    private static boolean allPayable(long[] prices, TradeBalance balance) {
        for (long price : prices) {
            if (!TradeRules.payable(price, balance)) {
                return false;
            }
        }
        return true;
    }

    @Override
    protected void addWidgets(Consumer<AbstractWidget> addWidget) {
        prune();
        dirty = false;
        shownSaturation = TradeService.saturation(player());
        TradeBalance balance = TradeBalance.current();
        long[] prices = TradeGoods.salePrices(stacksOf(chosen), shownSaturation, today(), balance);
        batchPrices.clear();
        int n = 0;
        for (int slot : chosen.keySet()) {
            batchPrices.put(slot, prices[n++]);
        }
        long total = TradeRules.total(prices);
        int y = top + height - BUTTON_HEIGHT;
        addWidget.accept(button(Component.translatable("screen.overgearedmod.trade.select_all"), left, y, ALL_WIDTH, true, () -> {
            chosen.clear();
            Inventory inventory = player().getInventory();
            for (int slot : slots()) {
                chosen.put(slot, inventory.getItem(slot).copy());
            }
            dirty = true;
        }));
        addWidget.accept(button(Component.translatable("screen.overgearedmod.trade.select_none"), left + ALL_WIDTH + 4, y, NONE_WIDTH, true, () -> {
            chosen.clear();
            dirty = true;
        }));
        int sellX = left + ALL_WIDTH + NONE_WIDTH + 8;
        int sellWidth = Math.max(20, width - BAR_SPACE - ALL_WIDTH - NONE_WIDTH - 8);
        Component label = Component.translatable("screen.overgearedmod.trade.sell_selected", chosen.size(), total);
        label = UiStyle.ellipsize(font(), label, sellWidth - 6);
        boolean active = !chosen.isEmpty() && allPayable(prices, balance);
        addWidget.accept(button(label, sellX, y, sellWidth, active, this::press));
    }

    private Button button(Component label, int x, int y, int buttonWidth, boolean active, Runnable action) {
        Button button = Button.builder(label, b -> action.run())
                .bounds(x, y, buttonWidth, BUTTON_HEIGHT)
                .build(SatisfyButton::new);
        button.active = active;
        return button;
    }

    private void press() {
        prune();
        if (chosen.isEmpty()) {
            return;
        }
        // The prices of this moment (live saturation), not the ones the tab was built with: the server rejects any other.
        TradeBalance balance = TradeBalance.current();
        long[] prices = TradeGoods.salePrices(stacksOf(chosen), TradeService.saturation(player()), today(), balance);
        if (!allPayable(prices, balance)) {
            return;
        }
        List<TradeSellPayload.Entry> entries = new ArrayList<>();
        boolean needsConfirm = false;
        int i = 0;
        for (Map.Entry<Integer, ItemStack> entry : chosen.entrySet()) {
            ItemStack seen = entry.getValue().copy();
            entries.add(new TradeSellPayload.Entry(entry.getKey(), seen, prices[i++]));
            ItemRating rating = seen.get(ModDataComponents.ITEM_RATING);
            if (rating != null && rating.ordinal() >= ItemRating.EPIC.ordinal()) {
                needsConfirm = true;
            }
        }
        batchPrices.clear();
        int n = 0;
        for (int slot : chosen.keySet()) {
            batchPrices.put(slot, prices[n++]);
        }
        long total = TradeRules.total(prices);
        int count = entries.size();
        Runnable send = () -> {
            PacketDistributor.sendToServer(new TradeSellPayload(entries));
            chosen.clear();
            dirty = true;
        };
        if (!needsConfirm) {
            send.run();
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        minecraft.setScreen(new SatisfyConfirmScreen(minecraft.screen,
                Component.translatable("screen.overgearedmod.trade.confirm_title"),
                Component.translatable("screen.overgearedmod.trade.confirm_sell", count, total), send));
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
                int slot = slots.get(i);
                if (chosen.remove(slot) == null) {
                    chosen.put(slot, player().getInventory().getItem(slot).copy());
                }
                dirty = true;
                return true;
            }
        }
        return false;
    }

    /** Rebuild (the button) when the choice or a chosen piece changed, or the saturation did. */
    @Override
    public boolean needsRebuild() {
        return prune() || dirty || !TradeService.saturation(player()).equals(shownSaturation);
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
            List<Component> tip = renderRow(graphics, stack, y, chosen.containsKey(slot), batchPrices.get(slot), hover, saturation, today, balance);
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
    private List<Component> renderRow(GuiGraphics graphics, ItemStack stack, int y, boolean isChosen, Long batchPrice, boolean hover,
                                      TradeSaturation saturation, long today, TradeBalance balance) {
        int rowWidth = width - BAR_SPACE;
        graphics.fill(left, y, left + rowWidth, y + ROW - 2, isChosen ? ROW_SELECTED : hover ? ROW_HOVER : ROW_FILL);
        UiStyle.checkbox(graphics, left + 3, y + (ROW - 2 - 9) / 2, isChosen);
        graphics.renderItem(stack, left + 15, y + 3);

        ItemRating rating = stack.get(ModDataComponents.ITEM_RATING);
        int nameColor = rating == null ? UiStyle.MUTED : 0xFF000000 | colorOf(rating);
        int textX = left + 35;
        int maxWidth = rowWidth - 35 - 4;
        graphics.drawString(font(), UiStyle.ellipsize(font(), stack.getHoverName().copy().withColor(nameColor), maxWidth),
                textX, y + 3, nameColor);

        long sell = isChosen && batchPrice != null ? batchPrice : TradeGoods.price(stack, saturation, today, balance);
        Component price = Component.translatable("screen.overgearedmod.trade.sell_price", sell);
        graphics.drawString(font(), price, textX, y + 13, UiStyle.TEXT);
        int percent = TradeRules.saturationPercent(saturation.pressure(TradeGoods.key(stack), today, balance), balance);
        if (percent < 100) {
            graphics.drawString(font(), Component.translatable("screen.overgearedmod.trade.saturation", percent),
                    textX + font().width(price) + 4, y + 13, UiStyle.BAD);
            return List.of(Component.translatable("screen.overgearedmod.trade.tip.saturation"));
        }
        return null;
    }

    private static int colorOf(ItemRating rating) {
        Integer color = rating.color().getColor();
        return color == null ? UiStyle.TEXT : color;
    }
}
