package com.orijua.overgearedmod.feature.economy.client;

import com.orijua.overgearedmod.core.client.SatisfyButton;
import com.orijua.overgearedmod.core.client.UiStyle;
import com.orijua.overgearedmod.core.config.FatigueBalance;
import com.orijua.overgearedmod.core.registry.ModAttachments;
import com.orijua.overgearedmod.feature.economy.ClientTerminal;
import com.orijua.overgearedmod.feature.economy.Money;
import com.orijua.overgearedmod.feature.economy.TerminalConvertPayload;
import com.orijua.overgearedmod.feature.economy.TerminalMenu;
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
 * conversion of Satisfy emeralds, and the daily work. It only shows what the server last sent ({@link ClientTerminal});
 * every button sends an order and waits for the new state.
 */
public class TerminalScreen extends AbstractContainerScreen<TerminalMenu> {
    private static final int MARGIN = 12;
    private static final int LEFT_WIDTH = 230;
    private static final int RIGHT_X = 254;

    private int selected = 1;
    private Button minus;
    private Button plus;
    private Button all;
    private Button convert;
    private Button work;

    public TerminalScreen(TerminalMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = 400;
        this.imageHeight = 226;
    }

    @Override
    protected void init() {
        super.init();
        minus = addRenderableWidget(button(Component.translatable("screen.overgearedmod.terminal.minus"), MARGIN, 96, 18, () -> selected--));
        plus = addRenderableWidget(button(Component.translatable("screen.overgearedmod.terminal.plus"), 74, 96, 18, () -> selected++));
        all = addRenderableWidget(button(Component.translatable("screen.overgearedmod.terminal.all"), 96, 96, 46,
                () -> selected = state().map(TerminalState::storedEmeralds).orElse(0)));
        convert = addRenderableWidget(button(Component.translatable("screen.overgearedmod.terminal.convert"), MARGIN, 156, 130, this::sendConvert));
        work = addRenderableWidget(button(Component.translatable("screen.overgearedmod.terminal.work_button"), RIGHT_X, 156, 134, this::sendWork));
    }

    private Button button(Component label, int x, int y, int width, Runnable action) {
        return Button.builder(label, pressed -> action.run()).bounds(leftPos + x, topPos + y, width, 18)
                .build(builder -> new SatisfyButton(builder, UiStyle.DEVICE));
    }

    private Optional<TerminalState> state() {
        return ClientTerminal.stateFor(menu.containerId);
    }

    private void sendConvert() {
        state().ifPresent(state -> {
            if (state.characterAvailable() && selected >= 1) {
                PacketDistributor.sendToServer(new TerminalConvertPayload(menu.containerId, selected, state.storedEmeralds()));
            }
        });
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
        if (current.isEmpty() || player == null || minecraft.level == null) {
            super.render(graphics, mouseX, mouseY, partialTick);
            return;
        }
        TerminalState state = current.get();
        int stored = state.characterAvailable() ? state.storedEmeralds() : 0;
        selected = stored == 0 ? 0 : Math.clamp(selected, 1, stored);
        minus.active = selected > 1;
        plus.active = selected < stored;
        all.active = selected < stored;
        convert.active = stored > 0 && TerminalRules.net(selected, state.wonPerEmerald(), state.commission()) >= 1;

        // The day is the server's; until its new state arrives after the client's clock changed day, work stays disabled.
        long today = state.today();
        boolean stateIsOld = TradeRules.day(minecraft.level.getGameTime()) != today;
        boolean worked = TerminalRules.workedOn(state.workDay(), today);
        int have = TerminalService.countOrder(player.getInventory(), state.order());
        work.active = !stateIsOld && !worked && have >= state.order().count();

        super.render(graphics, mouseX, mouseY, partialTick);
        drawContent(graphics, state, player, stored, worked, have, stateIsOld);
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
                state.characterAvailable() ? UiStyle.TEXT : UiStyle.MUTED);
        Component number = Component.literal(Integer.toString(selected));
        UiStyle.text(graphics, font, number, x + 33 + (40 - font.width(number)) / 2, y + 101, UiStyle.TEXT);

        long gross = TerminalRules.gross(selected, state.wonPerEmerald());
        long net = TerminalRules.net(selected, state.wonPerEmerald(), state.commission());
        UiStyle.text(graphics, font, Component.translatable("screen.overgearedmod.terminal.gross", gross), x + MARGIN, y + 120, UiStyle.TEXT);
        UiStyle.text(graphics, font, UiStyle.ellipsize(font, Component.translatable("screen.overgearedmod.terminal.commission",
                Money.percent(state.commission()), gross - net), LEFT_WIDTH), x + MARGIN, y + 132, UiStyle.MUTED);
        UiStyle.text(graphics, font, Component.translatable("screen.overgearedmod.terminal.net", net), x + MARGIN, y + 144, UiStyle.GOOD);

        List<FormattedCharSequence> warning = font.split(Component.translatable("screen.overgearedmod.terminal.warning"), imageWidth - 2 * MARGIN);
        for (int i = 0; i < warning.size() && i < 2; i++) {
            graphics.drawString(font, warning.get(i), x + MARGIN, y + 180 + i * 10, UiStyle.MUTED);
        }

        // Work
        WorkOrder order = state.order();
        ItemStack icon = state.icon();
        UiStyle.heading(graphics, font, Component.translatable("screen.overgearedmod.terminal.work", state.workPay()), x + RIGHT_X, y + 60);
        graphics.renderItem(icon, x + RIGHT_X, y + 72);
        String key = "screen.overgearedmod.terminal.order." + order.id().toLanguageKey().replace('/', '.');
        Component name = Component.translatableWithFallback(key, icon.getHoverName().getString());
        UiStyle.text(graphics, font, UiStyle.ellipsize(font, Component.translatable("screen.overgearedmod.terminal.order", name, order.count()), 114),
                x + RIGHT_X + 22, y + 76, UiStyle.TEXT);
        UiStyle.text(graphics, font, Component.translatable("screen.overgearedmod.terminal.have", have, order.count()), x + RIGHT_X, y + 96,
                have >= order.count() ? UiStyle.GOOD : UiStyle.MUTED);
        if (worked) {
            UiStyle.text(graphics, font, Component.translatable("screen.overgearedmod.terminal.done"), x + RIGHT_X, y + 110, UiStyle.GOOD);
            UiStyle.text(graphics, font, Component.translatable("screen.overgearedmod.terminal.next_shift",
                    stateIsOld ? 0 : TerminalRules.minutesToNextDay(minecraft.level.getGameTime())), x + RIGHT_X, y + 122, UiStyle.MUTED);
        }

        // The result of the last action
        state.message().ifPresent(message -> UiStyle.text(graphics, font, UiStyle.ellipsize(font, message, imageWidth - 2 * MARGIN),
                x + MARGIN, y + 206, UiStyle.COOLDOWN));
    }
}
