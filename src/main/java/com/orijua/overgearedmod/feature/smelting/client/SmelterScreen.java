package com.orijua.overgearedmod.feature.smelting.client;

import com.orijua.overgearedmod.core.client.SatisfyButton;
import com.orijua.overgearedmod.core.client.UiStyle;
import com.orijua.overgearedmod.core.registry.ModRecipes;
import com.orijua.overgearedmod.feature.progression.ProgressionService;
import com.orijua.overgearedmod.feature.smelting.ManualSmeltPayload;
import com.orijua.overgearedmod.feature.smelting.MeltingRecipe;
import com.orijua.overgearedmod.feature.smelting.SmelterBlockEntity;
import com.orijua.overgearedmod.feature.smelting.SmelterMenu;
import com.orijua.overgearedmod.feature.smelting.SmelterRules;
import com.orijua.overgearedmod.feature.smelting.SmeltingMinigame;
import net.minecraft.Util;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;

import java.util.List;
import java.util.Optional;

/**
 * The smelter's Satisfy window: metal, fuel and ingot slots, a thermometer with the metal's zone in gold, the melting
 * progress, and Auto / Bellows. A side panel says what the metal needs (heat, smithing level) and whether it is met.
 * Bellows opens the minigame over the window: click or space pumps, Esc cancels.
 */
public class SmelterScreen extends AbstractContainerScreen<SmelterMenu> {
    private static final int SIDE_GAP = 4;
    private static final int SIDE_WIDTH = 140;
    private static final int PADDING = 6;
    private static final int LINE = 10;
    private static final int SLOT_BACKGROUND = 0xC01B2A40;
    private static final int HEAT_COLD = 0xFFFFB347;
    private static final int HEAT_HOT = 0xFFE0432B;
    private static final int ZONE = 0x90FFD700;
    /** The thermometer, left of the slots. */
    private static final int THERMO_X = 24;
    private static final int THERMO_TOP = 16;
    private static final int THERMO_HEIGHT = 56;
    private static final int GAME_WIDTH = 220;
    private static final int BAR_WIDTH = 180;
    /** The minigame's bar shows from this far below the zone to this far above it. */
    private static final int BAR_BELOW = 300;
    private static final int BAR_ABOVE = 450;
    private static final long RESULT_MILLIS = 1500;

    private Button autoButton;
    private Button putOutButton;
    private Button bellowsButton;
    private Optional<MeltingRecipe> recipe = Optional.empty();
    private SmeltingMinigame minigame;
    private long lastUpdate;
    private long resultShownUntil = -1;
    private boolean spaceDown;

    public SmelterScreen(SmelterMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
    }

    @Override
    protected void init() {
        super.init();
        leftPos = (width - imageWidth - SIDE_GAP - SIDE_WIDTH) / 2;
        autoButton = addRenderableWidget(Button.builder(Component.empty(), b -> pressButton(SmelterMenu.AUTO_BUTTON))
                .bounds(leftPos + 86, topPos + 58, 40, 14)
                .tooltip(Tooltip.create(Component.translatable("screen.overgearedmod.smelter_auto.tooltip")))
                .build(SatisfyButton::new));
        putOutButton = addRenderableWidget(Button.builder(Component.translatable("screen.overgearedmod.smelter_fire_off"),
                        b -> pressButton(SmelterMenu.PUT_OUT_BUTTON))
                .bounds(leftPos + 129, topPos + 18, 42, 14)
                .tooltip(Tooltip.create(Component.translatable("screen.overgearedmod.smelter_fire.tooltip")))
                .build(SatisfyButton::new));
        bellowsButton = addRenderableWidget(Button.builder(Component.translatable("screen.overgearedmod.smelter_bellows"), b -> startMinigame())
                .bounds(leftPos + 129, topPos + 58, 42, 14)
                .tooltip(Tooltip.create(Component.translatable("screen.overgearedmod.smelter_bellows.tooltip")))
                .build(SatisfyButton::new));
        containerTick();
    }

    private void pressButton(int id) {
        minecraft.gameMode.handleInventoryButtonClick(menu.containerId, id);
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        // Recipes are synced to the client, so the screen can tell what the metal needs.
        SingleRecipeInput input = new SingleRecipeInput(menu.getSlot(SmelterBlockEntity.INPUT).getItem());
        recipe = minecraft.level.getRecipeManager().getRecipeFor(ModRecipes.MELTING.get(), input, minecraft.level).map(RecipeHolder::value);
        autoButton.setMessage(Component.translatable(menu.melting() ? "screen.overgearedmod.smelter_stop" : "screen.overgearedmod.smelter_auto"));
        autoButton.visible = recipe.isPresent() || menu.melting();
        putOutButton.visible = menu.fireOn();
        putOutButton.active = minigame == null;
        bellowsButton.visible = recipe.isPresent();
        autoButton.active = minigame == null;
        bellowsButton.active = minigame == null && !menu.melting() && recipe.filter(r -> skill() >= r.smithingLevel()).isPresent();
    }

    private int skill() {
        return ProgressionService.effectiveSmithingLevel(minecraft.player);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        UiStyle.panel(graphics, leftPos, topPos, imageWidth, imageHeight);
        for (Slot slot : menu.slots) {
            int x = leftPos + slot.x - 1;
            int y = topPos + slot.y - 1;
            graphics.fill(x, y, x + 18, y + 18, SLOT_BACKGROUND);
            graphics.renderOutline(x, y, 18, 18, UiStyle.LINE_SOFT);
        }
        thermometer(graphics, leftPos + THERMO_X, topPos + THERMO_TOP);
        // The flame between metal and fuel, shrinking as the fuel burns.
        UiStyle.bar(graphics, leftPos + 57, topPos + 41, 16, 3, menu.burnLeft(), HEAT_HOT);
        arrow(graphics, leftPos + 81, topPos + 38);
        if (menu.melting()) {
            UiStyle.bar(graphics, leftPos + 81, topPos + 52, 22, 2, menu.meltProgress(), UiStyle.XP);
        }
        graphics.fill(leftPos + 7, topPos + 76, leftPos + imageWidth - 7, topPos + 77, UiStyle.LINE_SOFT);
        sidePanel(graphics, leftPos + imageWidth + SIDE_GAP, topPos);
    }

    /** A vertical thermometer from cold (bottom) to the hottest the smelter gets, with the metal's zone in gold. */
    private void thermometer(GuiGraphics graphics, int x, int y) {
        int width = 8;
        graphics.fill(x - 1, y - 1, x + width + 1, y + THERMO_HEIGHT + 1, UiStyle.LINE_SOFT);
        graphics.fill(x, y, x + width, y + THERMO_HEIGHT, SLOT_BACKGROUND);
        int melting = menu.meltingTemperature();
        if (melting > 0) {
            int zoneTop = y + THERMO_HEIGHT - heightOf(melting + SmelterRules.ZONE_WIDTH);
            int zoneBottom = y + THERMO_HEIGHT - heightOf(melting);
            graphics.fill(x, zoneTop, x + width, zoneBottom, ZONE);
        }
        int filled = heightOf(menu.temperature());
        graphics.fillGradient(x + 2, y + THERMO_HEIGHT - filled, x + width - 2, y + THERMO_HEIGHT, HEAT_HOT, HEAT_COLD);
    }

    private static int heightOf(int temperature) {
        return Math.clamp(temperature * THERMO_HEIGHT / SmelterRules.MAX_TEMPERATURE, 0, THERMO_HEIGHT);
    }

    private static void arrow(GuiGraphics graphics, int x, int y) {
        graphics.fill(x, y + 4, x + 15, y + 6, UiStyle.LINE_BRIGHT);
        for (int i = 0; i < 6; i++) {
            graphics.fill(x + 15 + i, y + i - 1, x + 16 + i, y + 11 - i, UiStyle.LINE_BRIGHT);
        }
    }

    /** Temperature and fuel, then what the metal needs: its heat and smithing level, met or not, and the extra-ingot odds. */
    private void sidePanel(GuiGraphics graphics, int x, int y) {
        int lines = recipe.isPresent() ? 8 : 4;
        UiStyle.frame(graphics, x, y, SIDE_WIDTH, PADDING * 2 + lines * LINE, 1.0f);
        int left = x + PADDING;
        int right = x + SIDE_WIDTH - PADDING;
        int row = y + PADDING;
        graphics.drawString(font, Component.translatable("screen.overgearedmod.smelter_heat"), left, row, UiStyle.HEADING);
        row += LINE;
        graphics.drawString(font, Component.translatable("screen.overgearedmod.smelter_temperature", menu.temperature()), left, row, UiStyle.TEXT);
        row += LINE;
        graphics.drawString(font, Component.translatable("screen.overgearedmod.smelter_fuel_heat", menu.fuelHeat()), left, row, UiStyle.MUTED);
        row += LINE;
        if (recipe.isEmpty()) {
            graphics.drawString(font, Component.translatable("screen.overgearedmod.smelter_put_metal"), left, row, UiStyle.MUTED);
            return;
        }
        MeltingRecipe metal = recipe.get();
        row += LINE;
        graphics.drawString(font, metal.result().getHoverName(), left, row, UiStyle.HEADING);
        row += LINE;
        requirement(graphics, Component.translatable("screen.overgearedmod.smelter_needs_heat", metal.minTemperature()),
                menu.temperature() >= metal.minTemperature(), left, right, row);
        row += LINE;
        requirement(graphics, Component.translatable("screen.overgearedmod.smelter_needs_level", metal.smithingLevel()),
                skill() >= metal.smithingLevel(), left, right, row);
        row += LINE;
        graphics.drawString(font, Component.translatable("screen.overgearedmod.smelter_extra_odds",
                Math.round(SmelterRules.autoExtraChance(skill()) * 100)), left, row, UiStyle.MUTED);
    }

    private void requirement(GuiGraphics graphics, Component text, boolean met, int left, int right, int row) {
        int color = met ? UiStyle.GOOD : UiStyle.BAD;
        graphics.drawString(font, text, left, row, color);
        String mark = met ? "✔" : "✘";
        graphics.drawString(font, mark, right - font.width(mark), row, color);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(font, title, titleLabelX, titleLabelY, UiStyle.HEADING);
        graphics.drawString(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, UiStyle.MUTED);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        if (minigame == null) {
            renderTooltip(graphics, mouseX, mouseY);
        } else {
            renderMinigame(graphics, Util.getMillis());
        }
    }

    // ---- Manual melting: the bellows minigame ----

    /** Hot enough: the minigame starts. Cold: the server lights the fire and says when it is ready; no game yet. */
    private void startMinigame() {
        if (recipe.isEmpty()) {
            return;
        }
        pressButton(SmelterMenu.MANUAL_BUTTON); // the server checks the heat and notes when it started
        if (menu.temperature() < recipe.get().minTemperature()) {
            return;
        }
        minigame = new SmeltingMinigame(recipe.get().minTemperature());
        lastUpdate = Util.getMillis();
        resultShownUntil = -1;
    }

    private void pump() {
        minigame.pump();
        minecraft.player.playSound(SoundEvents.FIRECHARGE_USE, 0.25f, 0.8f + minecraft.player.getRandom().nextFloat() * 0.3f);
    }

    /** Sends the result when time is up, then leaves it on screen for a moment. */
    private void updateMinigame(long now) {
        minigame.update(now - lastUpdate);
        lastUpdate = now;
        if (minigame.done() && resultShownUntil < 0) {
            PacketDistributor.sendToServer(new ManualSmeltPayload(menu.containerId, (float) minigame.precision()));
            resultShownUntil = now + RESULT_MILLIS;
        }
        if (resultShownUntil >= 0 && now >= resultShownUntil) {
            minigame = null;
        }
    }

    private void renderMinigame(GuiGraphics graphics, long now) {
        updateMinigame(now);
        if (minigame == null) {
            return;
        }
        graphics.pose().pushPose();
        graphics.pose().translate(0, 0, 400); // above the items in the slots
        graphics.fill(0, 0, width, height, 0xA0000000);

        Component instruction = minigame.done()
                ? Component.translatable("minigame.overgearedmod.smelt_result", Math.round(minigame.precision() * 100))
                : Component.translatable("minigame.overgearedmod.smelt_pump");
        List<FormattedCharSequence> lines = font.split(instruction, GAME_WIDTH - 2 * PADDING);
        int panelHeight = PADDING + LINE + 4 + lines.size() * LINE + 6 + 10 + 4 + 4 + 6 + LINE + LINE + PADDING;
        int x = (width - GAME_WIDTH) / 2;
        int y = (height - panelHeight) / 2;
        int center = x + GAME_WIDTH / 2;
        UiStyle.panel(graphics, x, y, GAME_WIDTH, panelHeight);

        int row = y + PADDING;
        graphics.drawCenteredString(font, Component.translatable("minigame.overgearedmod.smelt_title"), center, row, UiStyle.HEADING);
        row += LINE;
        graphics.fill(x + PADDING, row, x + GAME_WIDTH - PADDING, row + 1, UiStyle.LINE_SOFT);
        row += 4;
        for (FormattedCharSequence line : lines) {
            graphics.drawCenteredString(font, line, center, row, UiStyle.TEXT);
            row += LINE;
        }
        row += 6;
        heatBar(graphics, center - BAR_WIDTH / 2, row);
        row += 10 + 4;
        UiStyle.bar(graphics, center - BAR_WIDTH / 2, row, BAR_WIDTH, 4, (float) (1 - minigame.progress()), UiStyle.XP);
        row += 4 + 6;
        graphics.drawCenteredString(font, feedback(), center, row, UiStyle.TEXT);
        row += LINE;
        graphics.drawCenteredString(font, Component.translatable("minigame.overgearedmod.cancel"), center, row, UiStyle.MUTED);
        graphics.pose().popPose();
    }

    /** The metal's temperature on a bar around its zone (gold): keep the marker inside it. */
    private void heatBar(GuiGraphics graphics, int x, int y) {
        int height = 8;
        int low = minigame.zoneMin() - BAR_BELOW;
        int range = BAR_BELOW + SmelterRules.ZONE_WIDTH + BAR_ABOVE;
        graphics.fill(x - 1, y - 1, x + BAR_WIDTH + 1, y + height + 1, UiStyle.LINE_SOFT);
        graphics.fill(x, y, x + BAR_WIDTH, y + height, SLOT_BACKGROUND);
        int zoneLeft = x + BAR_BELOW * BAR_WIDTH / range;
        graphics.fill(zoneLeft, y, zoneLeft + SmelterRules.ZONE_WIDTH * BAR_WIDTH / range, y + height, ZONE);
        int filled = (int) Math.clamp((minigame.temperature() - low) * BAR_WIDTH / range, 0, BAR_WIDTH);
        graphics.fillGradient(x, y + 2, x + filled, y + height - 2, HEAT_COLD, HEAT_HOT);
        graphics.fill(x + filled - 1, y - 2, x + filled + 1, y + height + 2, UiStyle.LINE_BRIGHT);
    }

    private Component feedback() {
        if (minigame.inZone()) {
            return Component.translatable("minigame.overgearedmod.smelt_in_zone").withStyle(s -> s.withColor(UiStyle.GOLD));
        }
        return minigame.temperature() > minigame.zoneMax()
                ? Component.translatable("minigame.overgearedmod.smelt_too_hot").withStyle(s -> s.withColor(UiStyle.BAD))
                : Component.translatable("minigame.overgearedmod.smelt_too_cold");
    }

    // While the minigame is open it takes every click and key, so nothing else in the smelter reacts.

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (minigame != null) {
            if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT && !minigame.done()) {
                pump();
            }
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        return minigame != null || super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        return minigame != null || super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (minigame != null) {
            if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
                minigame = null; // cancelled: nothing is sent, nothing is spent
            } else if (keyCode == GLFW.GLFW_KEY_SPACE && !spaceDown && !minigame.done()) {
                spaceDown = true;
                pump();
            }
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean keyReleased(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_SPACE) {
            spaceDown = false;
            if (minigame != null) {
                return true;
            }
        }
        return super.keyReleased(keyCode, scanCode, modifiers);
    }
}
