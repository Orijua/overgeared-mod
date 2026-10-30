package com.orijua.overgearedmod.client;

import com.orijua.overgearedmod.Balance;
import com.orijua.overgearedmod.client.ui.SatisfyButton;
import com.orijua.overgearedmod.client.ui.UiStyle;
import com.orijua.overgearedmod.equipment.EquipRequirements;
import com.orijua.overgearedmod.equipment.EquipmentRules;
import com.orijua.overgearedmod.equipment.ItemRating;
import com.orijua.overgearedmod.equipment.RatingRoller;
import com.orijua.overgearedmod.progression.PlayerStats;
import com.orijua.overgearedmod.progression.ProgressionService;
import com.orijua.overgearedmod.progression.StatType;
import com.orijua.overgearedmod.registry.ModRecipes;
import com.orijua.overgearedmod.smithing.ForgingRecipe;
import com.orijua.overgearedmod.smithing.ForgingRecipeInput;
import com.orijua.overgearedmod.smithing.SmithingAnvilMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.crafting.RecipeHolder;

import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * The smithing anvil as a Satisfy window. The slots keep vanilla's furnace positions; a side panel shows
 * the player's real rating odds (from DEX and smithing level) and, once the inputs match a forging recipe,
 * the requirements the result will have, ticked against the player's stats. The "Enhance" button appears
 * when the menu holds equipment + an enhancement stone and shows the success chance.
 */
public class SmithingAnvilScreen extends AbstractContainerScreen<SmithingAnvilMenu> {
    private static final int SIDE_GAP = 4;
    private static final int SIDE_WIDTH = 120;
    private static final int PADDING = 6;
    private static final int LINE = 10;
    private static final int SLOT_BACKGROUND = 0xC01B2A40;

    private Button enhanceButton;
    private double[] odds = new double[ItemRating.values().length];
    /** Empty when the inputs match no forging recipe; holds an empty Optional for a recipe without requirements. */
    private Optional<Optional<EquipRequirements>> recipeRequirements = Optional.empty();

    public SmithingAnvilScreen(SmithingAnvilMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
    }

    @Override
    protected void init() {
        super.init();
        // Center the window and its side panel together.
        leftPos = (width - imageWidth - SIDE_GAP - SIDE_WIDTH) / 2;
        enhanceButton = addRenderableWidget(Button.builder(Component.empty(),
                        b -> minecraft.gameMode.handleInventoryButtonClick(menu.containerId, SmithingAnvilMenu.ENHANCE_BUTTON))
                .bounds(leftPos + 96, topPos + 56, 72, 14)
                .build(SatisfyButton::new));
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        PlayerStats stats = ProgressionService.effectiveStats(minecraft.player);
        enhanceButton.visible = menu.canEnhance();
        if (enhanceButton.visible) {
            int level = menu.currentEnhancement();
            double chance = EquipmentRules.enhancementChance(Balance.current(), level, stats.dexterity());
            enhanceButton.setMessage(Component.translatable("screen.overgearedmod.enhance", level + 1, Math.round(chance * 100)));
        }
        odds = RatingRoller.odds(stats.dexterity(), ProgressionService.effectiveSmithingLevel(minecraft.player));
        // Recipes are synced to the client, so the screen can find the one the server will use.
        ForgingRecipeInput input = new ForgingRecipeInput(menu.getSlot(SmithingAnvilMenu.TOP_SLOT).getItem(),
                menu.getSlot(SmithingAnvilMenu.BOTTOM_SLOT).getItem());
        recipeRequirements = minecraft.level.getRecipeManager().getRecipeFor(ModRecipes.FORGING.get(), input, minecraft.level)
                .map(RecipeHolder::value)
                .map(ForgingRecipe::requirements);
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
        arrow(graphics, leftPos + 81, topPos + 38);
        graphics.fill(leftPos + 7, topPos + 76, leftPos + imageWidth - 7, topPos + 77, UiStyle.LINE_SOFT);
        sidePanel(graphics, leftPos + imageWidth + SIDE_GAP, topPos);
    }

    /** A right-pointing arrow between the inputs and the result. */
    private static void arrow(GuiGraphics graphics, int x, int y) {
        graphics.fill(x, y + 4, x + 15, y + 6, UiStyle.LINE_BRIGHT);
        for (int i = 0; i < 6; i++) {
            graphics.fill(x + 15 + i, y + i - 1, x + 16 + i, y + 11 - i, UiStyle.LINE_BRIGHT);
        }
    }

    private void sidePanel(GuiGraphics graphics, int x, int y) {
        int lines = 1 + odds.length + recipeRequirements.map(r -> 2 + r.map(req -> 1 + req.stats().size()).orElse(1)).orElse(0);
        UiStyle.frame(graphics, x, y, SIDE_WIDTH, PADDING * 2 + lines * LINE, 1.0f);
        int left = x + PADDING;
        int right = x + SIDE_WIDTH - PADDING;
        int row = y + PADDING;

        graphics.drawString(font, Component.translatable("screen.overgearedmod.anvil_odds"), left, row, UiStyle.HEADING);
        row += LINE;
        for (ItemRating rating : ItemRating.values()) {
            graphics.drawString(font, rating.displayName(), left, row, UiStyle.TEXT);
            String percent = percent(odds[rating.ordinal()]);
            graphics.drawString(font, percent, right - font.width(percent), row, UiStyle.TEXT);
            row += LINE;
        }

        if (recipeRequirements.isEmpty()) {
            return;
        }
        row += LINE;
        graphics.drawString(font, Component.translatable("screen.overgearedmod.anvil_requirements"), left, row, UiStyle.HEADING);
        row += LINE;
        Optional<EquipRequirements> requirements = recipeRequirements.get();
        if (requirements.isEmpty()) {
            graphics.drawString(font, Component.translatable("screen.overgearedmod.anvil_no_requirements"), left, row, UiStyle.MUTED);
            return;
        }
        PlayerStats stats = ProgressionService.effectiveStats(minecraft.player);
        requirement(graphics, Component.translatable("tooltip.overgearedmod.requires_level", requirements.get().level()),
                stats.level() >= requirements.get().level(), left, right, row);
        for (Map.Entry<StatType, Integer> stat : requirements.get().stats().entrySet()) {
            row += LINE;
            requirement(graphics, stat.getKey().displayName().copy().append(" " + stat.getValue()),
                    stats.get(stat.getKey()) >= stat.getValue(), left, right, row);
        }
    }

    /** One requirement line, green with a tick when the player meets it, red with a cross when not. */
    private void requirement(GuiGraphics graphics, Component text, boolean met, int left, int right, int row) {
        int color = met ? UiStyle.GOOD : UiStyle.BAD;
        graphics.drawString(font, text, left, row, color);
        String mark = met ? "✔" : "✘";
        graphics.drawString(font, mark, right - font.width(mark), row, color);
    }

    /** "60%", "4.0%", "0.10%": more decimals for the rare tiers so they don't all read 0%. */
    private static String percent(double fraction) {
        double percent = fraction * 100;
        String format = percent >= 10 ? "%.0f%%" : percent >= 1 ? "%.1f%%" : "%.2f%%";
        return String.format(Locale.ROOT, format, percent);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(font, title, titleLabelX, titleLabelY, UiStyle.HEADING);
        graphics.drawString(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, UiStyle.MUTED);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
    }
}
