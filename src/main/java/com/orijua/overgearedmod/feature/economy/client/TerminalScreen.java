package com.orijua.overgearedmod.feature.economy.client;

import com.orijua.overgearedmod.core.client.SatisfyButton;
import com.orijua.overgearedmod.core.client.ScrollList;
import com.orijua.overgearedmod.core.client.UiStyle;
import com.orijua.overgearedmod.core.config.FatigueBalance;
import com.orijua.overgearedmod.core.registry.ModAttachments;
import com.orijua.overgearedmod.feature.economy.Account;
import com.orijua.overgearedmod.feature.economy.AccountRules;
import com.orijua.overgearedmod.feature.economy.ClientTerminal;
import com.orijua.overgearedmod.feature.economy.DebtState;
import com.orijua.overgearedmod.feature.economy.OfferEntry;
import com.orijua.overgearedmod.feature.economy.TerminalBuyEmeraldsPayload;
import com.orijua.overgearedmod.feature.economy.TerminalBuyOfferPayload;
import com.orijua.overgearedmod.feature.economy.TerminalMenu;
import com.orijua.overgearedmod.feature.economy.TerminalPayDebtPayload;
import com.orijua.overgearedmod.feature.economy.TerminalRules;
import com.orijua.overgearedmod.feature.economy.TerminalService;
import com.orijua.overgearedmod.feature.economy.TerminalState;
import com.orijua.overgearedmod.feature.economy.TerminalWorkPayload;
import com.orijua.overgearedmod.feature.economy.TradeRules;
import com.orijua.overgearedmod.feature.economy.WorkOrder;
import com.orijua.overgearedmod.feature.fatigue.client.FatigueHudLayer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.List;
import java.util.Optional;

/**
 * The terminal's window ("Cuenta y comercio"), in the colors of a real-world device: the energy, the account with the
 * Satisfy emeralds stored, and the daily work. It only shows what the server last sent ({@link ClientTerminal});
 * every button sends an order and waits for the new state.
 */
public class TerminalScreen extends AbstractContainerScreen<TerminalMenu> {
    private static final int MARGIN = 12;
    /** "No Satisfy character" blocks buying, so it is red in every page. */
    private static final int NO_CHARACTER_COLOR = UiStyle.BAD;
    private static final int LEFT_WIDTH = 230;
    private static final int RIGHT_X = 254;

    private static final int PAGE_ACCOUNT = 0;
    private static final int PAGE_SHOP = 1;
    private static final int OFFER_X = 160;
    private static final int OFFER_WIDTH = 224;
    private static final int OFFER_TOP = 48;
    private static final int OFFER_ROW = 22;
    private static final int OFFER_ROWS = 6;

    private int page = PAGE_ACCOUNT;
    private int buySelected = 1;
    private long debtSelected = TerminalRules.DEBT_STEP;
    private final ScrollList offerScroll = new ScrollList(OFFER_ROWS);
    private Button pageAccount;
    private Button pageShop;
    private Button work;
    private Button buyMinus;
    private Button buyPlus;
    private Button buyMax;
    private Button buyEmeralds;
    private Button debtMinus;
    private Button debtPlus;
    private Button debtMax;
    private Button debtPay;
    private final Button[] rowButtons = new Button[OFFER_ROWS];

    public TerminalScreen(TerminalMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = 400;
        this.imageHeight = 226;
    }

    @Override
    protected void init() {
        super.init();
        work = addRenderableWidget(button(Component.translatable("screen.overgearedmod.terminal.work_button"), RIGHT_X, 156, 134, this::sendWork));

        pageAccount = addRenderableWidget(button(Component.translatable("screen.overgearedmod.terminal.page.account"), 256, 6, 64, () -> page = PAGE_ACCOUNT));
        pageShop = addRenderableWidget(button(Component.translatable("screen.overgearedmod.terminal.page.shop"), 324, 6, 64, () -> page = PAGE_SHOP));
        buyMinus = addRenderableWidget(button(Component.translatable("screen.overgearedmod.terminal.minus"), MARGIN, 62, 18, () -> buySelected--));
        buyPlus = addRenderableWidget(button(Component.translatable("screen.overgearedmod.terminal.plus"), 74, 62, 18, () -> buySelected++));
        buyMax = addRenderableWidget(button(Component.translatable("screen.overgearedmod.terminal.shop.max"), 96, 62, 46,
                () -> buySelected = maxBuyable(state().orElse(null))));
        buyEmeralds = addRenderableWidget(button(Component.translatable("screen.overgearedmod.terminal.shop.buy"), MARGIN, 112, 130, this::sendBuyEmeralds));
        debtMinus = addRenderableWidget(button(Component.translatable("screen.overgearedmod.terminal.minus"), MARGIN, 118, 18,
                () -> debtSelected -= TerminalRules.DEBT_STEP));
        debtPlus = addRenderableWidget(button(Component.translatable("screen.overgearedmod.terminal.plus"), 74, 118, 18,
                () -> debtSelected += TerminalRules.DEBT_STEP));
        debtMax = addRenderableWidget(button(Component.translatable("screen.overgearedmod.terminal.shop.max"), 96, 118, 46,
                () -> debtSelected = AccountRules.maxDebtPayment(account())));
        debtPay = addRenderableWidget(button(Component.translatable("screen.overgearedmod.terminal.debt_pay"), MARGIN, 138, 130, this::sendPayDebt));
        for (int i = 0; i < OFFER_ROWS; i++) {
            int row = i;
            rowButtons[i] = addRenderableWidget(button(Component.translatable("screen.overgearedmod.terminal.shop.buy"),
                    OFFER_X + OFFER_WIDTH - 58, OFFER_TOP, 54, () -> sendBuyOffer(offerScroll.first() + row)));
            rowButtons[i].setHeight(16);
        }
    }

    private static int maxBuyable(TerminalState state) {
        if (state == null || !state.characterAvailable()) {
            return 0;
        }
        long won = Minecraft.getInstance().player == null ? 0L : Minecraft.getInstance().player.getData(ModAttachments.ACCOUNT.get()).won();
        return TerminalRules.maxBuyable(state.emeraldRoom(), won, state.buyWonPerEmerald());
    }

    private void sendBuyEmeralds() {
        state().ifPresent(state -> {
            if (state.characterAvailable() && buySelected >= 1) {
                PacketDistributor.sendToServer(new TerminalBuyEmeraldsPayload(menu.containerId, buySelected, state.buyWonPerEmerald()));
            }
        });
    }

    private static Account account() {
        return Minecraft.getInstance().player == null ? Account.EMPTY : Minecraft.getInstance().player.getData(ModAttachments.ACCOUNT.get());
    }

    private void sendPayDebt() {
        Account account = account();
        long max = AccountRules.maxDebtPayment(account);
        if (state().isPresent() && debtSelected >= 1 && debtSelected <= max) {
            PacketDistributor.sendToServer(new TerminalPayDebtPayload(menu.containerId, debtSelected, account.debt()));
        }
    }

    private void sendBuyOffer(int position) {
        state().ifPresent(state -> {
            if (position >= 0 && position < state.offer().size() && !state.boughtToday().contains(position)) {
                OfferEntry entry = state.offer().get(position);
                PacketDistributor.sendToServer(new TerminalBuyOfferPayload(menu.containerId, entry.position(), entry.stack(), entry.price()));
            }
        });
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (page == PAGE_SHOP) {
            Optional<TerminalState> current = state();
            if (current.isPresent() && offerScroll.scroll(scrollY, current.get().offer().size())) {
                return true;
            }
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    private Button button(Component label, int x, int y, int width, Runnable action) {
        return Button.builder(label, pressed -> action.run()).bounds(leftPos + x, topPos + y, width, 18)
                .build(builder -> new SatisfyButton(builder, UiStyle.DEVICE));
    }

    private Optional<TerminalState> state() {
        return ClientTerminal.stateFor(menu.containerId);
    }

    private void sendWork() {
        state().ifPresent(state -> PacketDistributor.sendToServer(new TerminalWorkPayload(menu.containerId, state.order())));
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        UiStyle.devicePanel(graphics, leftPos, topPos, imageWidth, imageHeight);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        Minecraft minecraft = Minecraft.getInstance();
        Optional<TerminalState> current = state();
        LocalPlayer player = minecraft.player;
        boolean account = page == PAGE_ACCOUNT;
        work.visible = account;
        for (Button widget : List.of(buyMinus, buyPlus, buyMax, buyEmeralds)) {
            widget.visible = !account;
        }
        for (Button widget : List.of(debtMinus, debtPlus, debtMax, debtPay)) {
            widget.visible = false;
        }
        pageAccount.active = !account;
        pageShop.active = account;
        for (Button rowButton : rowButtons) {
            rowButton.visible = false;
        }
        if (current.isEmpty() || player == null || minecraft.level == null) {
            super.render(graphics, mouseX, mouseY, partialTick);
            return;
        }
        TerminalState state = current.get();
        // The day is the server's; until its new state arrives after the client's clock changed day, work and buying stay disabled.
        long today = state.today();
        boolean stateIsOld = TradeRules.day(minecraft.level.getGameTime()) != today;
        if (!account) {
            updateShop(state, player, stateIsOld);
            super.render(graphics, mouseX, mouseY, partialTick);
            drawShop(graphics, state, player, mouseX, mouseY);
            return;
        }
        int stored = state.characterAvailable() ? state.storedEmeralds() : 0;

        boolean worked = TerminalRules.workedOn(state.workDay(), today);
        int have = TerminalService.countOrder(player.getInventory(), state.order());
        work.active = !stateIsOld && !worked && have >= state.order().count();
        updateDebt(player, stateIsOld);

        super.render(graphics, mouseX, mouseY, partialTick);
        drawContent(graphics, state, player, stored, worked, have, stateIsOld);
    }

    /** The debt buttons of the account page: shown only while the debt is active. */
    private void updateDebt(LocalPlayer player, boolean stateIsOld) {
        Account account = player.getData(ModAttachments.ACCOUNT.get());
        boolean active = account.debtState() == DebtState.ACTIVE;
        long max = AccountRules.maxDebtPayment(account);
        debtSelected = TerminalRules.debtSelection(debtSelected, max);
        for (Button widget : List.of(debtMinus, debtPlus, debtMax, debtPay)) {
            widget.visible = active;
        }
        debtMinus.active = debtSelected > 1;
        debtPlus.active = debtSelected < max;
        debtMax.active = debtSelected < max;
        debtPay.active = !stateIsOld && max >= 1 && debtSelected >= 1 && debtSelected <= max;
    }

    /** The shop page buttons: the emerald selector and the buy button of each visible row. */
    private void updateShop(TerminalState state, LocalPlayer player, boolean stateIsOld) {
        long won = player.getData(ModAttachments.ACCOUNT.get()).won();
        int room = state.characterAvailable() ? state.emeraldRoom() : 0;
        buySelected = room == 0 ? 0 : Math.clamp(buySelected, 1, room);
        buyMinus.active = buySelected > 1;
        buyPlus.active = buySelected < room;
        buyMax.active = buySelected < maxBuyable(state);
        buyEmeralds.active = !stateIsOld && state.characterAvailable() && buySelected >= 1 && buySelected <= room
                && won >= TerminalRules.buyCost(buySelected, state.buyWonPerEmerald());

        List<OfferEntry> offer = state.offer();
        offerScroll.clamp(offer.size());
        for (int i = 0; i < OFFER_ROWS; i++) {
            int position = offerScroll.first() + i;
            Button rowButton = rowButtons[i];
            boolean shown = position < offer.size() && !state.boughtToday().contains(position);
            rowButton.visible = shown;
            if (shown) {
                rowButton.setY(topPos + OFFER_TOP + i * OFFER_ROW + 3);
                rowButton.active = !stateIsOld && state.characterAvailable() && state.freeSlots() > 0 && won >= offer.get(position).price();
            }
        }
    }

    private void drawShop(GuiGraphics graphics, TerminalState state, LocalPlayer player, int mouseX, int mouseY) {
        int x = leftPos;
        int y = topPos;
        UiStyle.heading(graphics, font, Component.translatable("screen.overgearedmod.terminal.title"), x + MARGIN, y + 8);
        UiStyle.text(graphics, font, Component.translatable("screen.overgearedmod.terminal.subtitle"), x + MARGIN, y + 20, UiStyle.MUTED);
        long won = player.getData(ModAttachments.ACCOUNT.get()).won();

        // Emeralds
        int room = state.characterAvailable() ? state.emeraldRoom() : 0;
        UiStyle.heading(graphics, font, Component.translatable("screen.overgearedmod.terminal.shop.emeralds"), x + MARGIN, y + 34);
        UiStyle.text(graphics, font, Component.translatable("screen.overgearedmod.terminal.shop.unit_price", state.buyWonPerEmerald()),
                x + MARGIN, y + 46, UiStyle.MUTED);
        Component number = Component.literal(Integer.toString(buySelected));
        UiStyle.text(graphics, font, number, x + 33 + (40 - font.width(number)) / 2, y + 67, UiStyle.TEXT);
        UiStyle.text(graphics, font, Component.translatable("screen.overgearedmod.terminal.shop.total",
                TerminalRules.buyCost(buySelected, state.buyWonPerEmerald())), x + MARGIN, y + 88, UiStyle.TEXT);
        UiStyle.text(graphics, font, UiStyle.ellipsize(font, state.characterAvailable()
                        ? Component.translatable("screen.overgearedmod.terminal.shop.room", room)
                        : Component.translatable("screen.overgearedmod.terminal.no_character"), 140),
                x + MARGIN, y + 100, state.characterAvailable() ? UiStyle.MUTED : NO_CHARACTER_COLOR);
        if (state.characterAvailable()) {
            UiStyle.text(graphics, font, Component.translatable("screen.overgearedmod.terminal.shop.free_slots", state.freeSlots()),
                    x + MARGIN, y + 112, state.freeSlots() == 0 ? UiStyle.BAD : UiStyle.MUTED);
        }
        UiStyle.text(graphics, font, Component.translatable("screen.overgearedmod.terminal.won", won), x + MARGIN, y + 140, UiStyle.GOLD);
        int noteY = y + 156;
        for (FormattedCharSequence line : font.split(Component.translatable("screen.overgearedmod.terminal.shop.note"), 140)) {
            graphics.drawString(font, line, x + MARGIN, noteY, UiStyle.MUTED);
            noteY += 10;
        }

        // The offer
        UiStyle.heading(graphics, font, Component.translatable("screen.overgearedmod.terminal.shop.offer"), x + OFFER_X, y + 34);
        List<OfferEntry> offer = state.offer();
        if (offer.isEmpty()) {
            UiStyle.text(graphics, font, Component.translatable("screen.overgearedmod.terminal.shop.no_offer"), x + OFFER_X, y + OFFER_TOP + 4, UiStyle.MUTED);
        }
        ItemStack hovered = ItemStack.EMPTY;
        for (int i = offerScroll.first(); i < offerScroll.end(offer.size()); i++) {
            OfferEntry entry = offer.get(i);
            int rowY = y + OFFER_TOP + (i - offerScroll.first()) * OFFER_ROW;
            int rowX = x + OFFER_X;
            boolean hover = mouseX >= rowX && mouseX < rowX + OFFER_WIDTH - 62 && mouseY >= rowY && mouseY < rowY + OFFER_ROW - 2;
            graphics.fill(rowX, rowY, rowX + OFFER_WIDTH - 4, rowY + OFFER_ROW - 2, hover ? 0x40FFFFFF : 0x20FFFFFF);
            graphics.renderItem(entry.stack(), rowX + 3, rowY + 2);
            int nameColor = UiStyle.ratingNameColor(entry.stack());
            graphics.drawString(font, UiStyle.ellipsize(font, entry.stack().getHoverName().copy().withColor(nameColor), OFFER_WIDTH - 24 - 62),
                    rowX + 24, rowY + 2, nameColor);
            UiStyle.text(graphics, font, Component.translatable("screen.overgearedmod.terminal.shop.price", entry.price()), rowX + 24, rowY + 11,
                    UiStyle.GOLD);
            if (state.boughtToday().contains(entry.position())) {
                Component bought = Component.translatable("screen.overgearedmod.terminal.shop.bought");
                UiStyle.text(graphics, font, bought, rowX + OFFER_WIDTH - 8 - font.width(bought), rowY + 7, UiStyle.GOOD);
            }
            if (hover) {
                hovered = entry.stack();
            }
        }
        offerScroll.renderBar(graphics, x + OFFER_X + OFFER_WIDTH - 1, y + OFFER_TOP, OFFER_ROWS * OFFER_ROW - 2, offer.size());
        drawMessage(graphics, state);
        if (!hovered.isEmpty()) {
            graphics.renderTooltip(font, hovered, mouseX, mouseY);
        }
    }

    private void drawMessage(GuiGraphics graphics, TerminalState state) {
        state.message().ifPresent(message -> UiStyle.text(graphics, font, UiStyle.ellipsize(font, message, imageWidth - 2 * MARGIN),
                leftPos + MARGIN, topPos + 206, UiStyle.COOLDOWN));
    }

    private void drawContent(GuiGraphics graphics, TerminalState state, LocalPlayer player, int stored, boolean worked, int have, boolean stateIsOld) {
        Minecraft minecraft = Minecraft.getInstance();
        int x = leftPos;
        int y = topPos;
        UiStyle.heading(graphics, font, Component.translatable("screen.overgearedmod.terminal.title"), x + MARGIN, y + 8);
        UiStyle.text(graphics, font, Component.translatable("screen.overgearedmod.terminal.subtitle"), x + MARGIN, y + 20, UiStyle.MUTED);

        // Energy
        if (FatigueBalance.current().enabled()) {
            float energy = player.getData(ModAttachments.FATIGUE);
            UiStyle.heading(graphics, font, Component.translatable("screen.overgearedmod.terminal.energy"), x + MARGIN, y + 34);
            Component text = FatigueHudLayer.energyText(energy);
            UiStyle.text(graphics, font, text, x + imageWidth - MARGIN - font.width(text), y + 34, FatigueHudLayer.colorFor(energy, FatigueBalance.current()));
            int color = FatigueHudLayer.colorFor(energy, FatigueBalance.current());
            UiStyle.bar(graphics, x + MARGIN, y + 46, imageWidth - 2 * MARGIN, 5, energy, color);
        }

        // Account
        UiStyle.heading(graphics, font, Component.translatable("screen.overgearedmod.terminal.account"), x + MARGIN, y + 60);
        long won = player.getData(ModAttachments.ACCOUNT.get()).won();
        UiStyle.text(graphics, font, Component.translatable("screen.overgearedmod.terminal.won", won), x + MARGIN, y + 72, UiStyle.GOLD);
        Component storedText = state.characterAvailable()
                ? Component.translatable("screen.overgearedmod.terminal.stored", stored)
                : Component.translatable("screen.overgearedmod.terminal.no_character");
        UiStyle.text(graphics, font, UiStyle.ellipsize(font, storedText, LEFT_WIDTH), x + MARGIN, y + 84,
                state.characterAvailable() ? UiStyle.TEXT : NO_CHARACTER_COLOR);

        // Debt
        Account account = player.getData(ModAttachments.ACCOUNT.get());
        if (account.debtState() == DebtState.ACTIVE) {
            UiStyle.heading(graphics, font, Component.translatable("screen.overgearedmod.terminal.debt"), x + MARGIN, y + 95);
            UiStyle.text(graphics, font, Component.translatable("screen.overgearedmod.terminal.debt_left", account.debt()), x + MARGIN, y + 106,
                    UiStyle.BAD);
            Component number = Component.literal(Long.toString(debtSelected));
            UiStyle.text(graphics, font, number, x + 33 + (40 - font.width(number)) / 2, y + 123, UiStyle.TEXT);
            UiStyle.text(graphics, font, Component.translatable("screen.overgearedmod.terminal.debt_total", debtSelected), x + MARGIN, y + 160,
                    UiStyle.MUTED);
        } else if (account.debtState() == DebtState.SETTLED) {
            UiStyle.heading(graphics, font, Component.translatable("screen.overgearedmod.terminal.debt"), x + MARGIN, y + 95);
            UiStyle.text(graphics, font, Component.translatable("screen.overgearedmod.terminal.debt_settled"), x + MARGIN, y + 106, UiStyle.GOOD);
        }

        // Work
        WorkOrder order = state.order();
        ItemStack icon = state.icon();
        long quota = AccountRules.quota(account, state.workPay(), state.debtQuotaPercent());
        UiStyle.heading(graphics, font, quota > 0
                ? Component.translatable("screen.overgearedmod.terminal.work_debt", state.workPay())
                : Component.translatable("screen.overgearedmod.terminal.work", state.workPay()), x + RIGHT_X, y + 60);
        graphics.renderItem(icon, x + RIGHT_X, y + 72);
        String key = "screen.overgearedmod.terminal.order." + order.id().toLanguageKey().replace('/', '.');
        Component name = Component.translatableWithFallback(key, icon.getHoverName().getString());
        UiStyle.text(graphics, font, UiStyle.ellipsize(font, Component.translatable("screen.overgearedmod.terminal.order", name, order.count()), 114),
                x + RIGHT_X + 22, y + 76, UiStyle.TEXT);
        UiStyle.text(graphics, font, Component.translatable("screen.overgearedmod.terminal.have", have, order.count()), x + RIGHT_X, y + 96,
                have >= order.count() ? UiStyle.GOOD : UiStyle.MUTED);
        if (quota > 0) {
            UiStyle.text(graphics, font, UiStyle.ellipsize(font, Component.translatable("screen.overgearedmod.terminal.work_split",
                    state.workPay() - quota, quota), 134), x + RIGHT_X, y + 138, UiStyle.MUTED);
        }
        if (worked) {
            UiStyle.text(graphics, font, Component.translatable("screen.overgearedmod.terminal.done"), x + RIGHT_X, y + 110, UiStyle.GOOD);
            UiStyle.text(graphics, font, Component.translatable("screen.overgearedmod.terminal.next_shift",
                    stateIsOld ? 0 : TerminalRules.minutesToNextDay(minecraft.level.getGameTime())), x + RIGHT_X, y + 122, UiStyle.MUTED);
        }

        // The result of the last action
        drawMessage(graphics, state);
    }
}
