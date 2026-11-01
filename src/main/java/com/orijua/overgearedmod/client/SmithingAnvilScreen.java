package com.orijua.overgearedmod.client;

import com.orijua.overgearedmod.Balance;
import com.orijua.overgearedmod.client.ui.SatisfyButton;
import com.orijua.overgearedmod.client.ui.UiStyle;
import com.orijua.overgearedmod.equipment.EquipRequirements;
import com.orijua.overgearedmod.equipment.EquipmentRules;
import com.orijua.overgearedmod.equipment.ItemRating;
import com.orijua.overgearedmod.equipment.RatingRoller;
import com.orijua.overgearedmod.network.ManualForgePayload;
import com.orijua.overgearedmod.playerclass.PlayerClassDefinition;
import com.orijua.overgearedmod.progression.PlayerStats;
import com.orijua.overgearedmod.progression.ProgressionService;
import com.orijua.overgearedmod.progression.StatType;
import com.orijua.overgearedmod.registry.Definitions;
import com.orijua.overgearedmod.registry.ModAttachments;
import com.orijua.overgearedmod.registry.ModDatapackRegistries;
import com.orijua.overgearedmod.registry.ModRecipes;
import com.orijua.overgearedmod.smithing.ForgingExtras;
import com.orijua.overgearedmod.smithing.ForgingMinigame;
import com.orijua.overgearedmod.smithing.ForgingRecipe;
import com.orijua.overgearedmod.smithing.ForgingRecipeInput;
import com.orijua.overgearedmod.smithing.ForgingRules;
import com.orijua.overgearedmod.smithing.SmithingAnvilMenu;
import com.orijua.overgearedmod.smithing.SmithingHammers;
import net.minecraft.Util;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * The smithing anvil as a Satisfy window. The slots keep vanilla's furnace positions; the result slot only
 * previews the item. <b>Auto</b> forges it after a short progress bar; <b>Manual</b> opens the forging
 * minigame over the window (click or space; Esc cancels) and sends its precision to the server.
 * A side panel shows the player's rating odds (automatic and with a perfect manual forge) and, once the
 * inputs match a recipe, the result's requirements ticked against the player's stats.
 * The "Enhance" button appears when the menu holds equipment + an enhancement stone.
 */
public class SmithingAnvilScreen extends AbstractContainerScreen<SmithingAnvilMenu> {
    private static final int SIDE_GAP = 4;
    private static final int SIDE_WIDTH = 140;
    private static final int PADDING = 6;
    private static final int LINE = 10;
    private static final int SLOT_BACKGROUND = 0xC01B2A40;
    private static final int GAME_WIDTH = 220;
    private static final int BAR_WIDTH = 180;
    private static final int HEAT_COLD = 0xFFFFB347;
    private static final int HEAT_HOT = 0xFFE0432B;
    /** How long the final precision stays on screen before the minigame window closes. */
    private static final long RESULT_MILLIS = 1500;

    private Button enhanceButton;
    private Button autoButton;
    private Button manualButton;
    private Button batchButton;
    /** Which tooltip the batch button shows now, so it is only rebuilt when that changes. */
    private String batchTooltipKey = "";
    private double[] autoOdds = new double[ItemRating.values().length];
    private double[] manualOdds = new double[ItemRating.values().length];
    /** The forging recipe the inputs match, if any (found on the client, even one for another class). */
    private Optional<ForgingRecipe> recipe = Optional.empty();

    /** The manual minigame being played over the window, or null. */
    private ForgingMinigame minigame;
    private long resultShownUntil = -1;
    /** Ignores the key-repeat presses while space is held, so holding it doesn't strike again and again. */
    private boolean spaceDown;

    public SmithingAnvilScreen(SmithingAnvilMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
    }

    @Override
    protected void init() {
        super.init();
        // Center the window and its side panel together.
        leftPos = (width - imageWidth - SIDE_GAP - SIDE_WIDTH) / 2;
        enhanceButton = addRenderableWidget(Button.builder(Component.empty(), b -> pressButton(SmithingAnvilMenu.ENHANCE_BUTTON))
                .bounds(leftPos + 96, topPos + 56, 72, 14)
                .build(SatisfyButton::new));
        autoButton = addRenderableWidget(Button.builder(Component.translatable("screen.overgearedmod.forge_auto"),
                        b -> pressButton(menu.autoForging() ? SmithingAnvilMenu.STOP_BUTTON : SmithingAnvilMenu.AUTO_FORGE_BUTTON))
                .bounds(leftPos + 86, topPos + 58, 38, 14)
                .tooltip(Tooltip.create(Component.translatable("screen.overgearedmod.forge_auto.tooltip")))
                .build(SatisfyButton::new));
        manualButton = addRenderableWidget(Button.builder(Component.translatable("screen.overgearedmod.forge_manual"), b -> startMinigame())
                .bounds(leftPos + 127, topPos + 58, 42, 14)
                .tooltip(Tooltip.create(Component.translatable("screen.overgearedmod.forge_manual.tooltip")))
                .build(SatisfyButton::new));
        // Right of the result slot: how many items one press of Auto makes.
        batchButton = addRenderableWidget(Button.builder(Component.empty(), b -> pressButton(SmithingAnvilMenu.BATCH_BUTTON))
                .bounds(leftPos + 138, topPos + 37, 30, 14)
                .build(SatisfyButton::new));
        batchTooltipKey = "";
        containerTick();
    }

    private void pressButton(int id) {
        minecraft.gameMode.handleInventoryButtonClick(menu.containerId, id);
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        PlayerStats stats = ProgressionService.effectiveStats(minecraft.player);
        int smithing = ProgressionService.effectiveSmithingLevel(minecraft.player);
        enhanceButton.visible = menu.canEnhance();
        if (enhanceButton.visible) {
            int level = menu.currentEnhancement();
            double chance = EquipmentRules.enhancementChance(Balance.current(), level, stats.dexterity());
            enhanceButton.setMessage(Component.translatable("screen.overgearedmod.enhance", level + 1, Math.round(chance * 100)));
        }
        boolean forgeable = menu.canForge() && !enhanceButton.visible;
        autoButton.visible = forgeable;
        manualButton.visible = forgeable;
        batchButton.visible = forgeable;
        boolean idle = !menu.autoForging() && minigame == null;
        autoButton.active = minigame == null; // while forging it is the Stop button
        autoButton.setMessage(Component.translatable(menu.autoForging() ? "screen.overgearedmod.forge_stop" : "screen.overgearedmod.forge_auto"));
        manualButton.active = idle;
        batchButton.active = idle && ForgingRules.batchUnlocked(1, smithing);
        updateBatchButton(smithing);

        // Recipes are synced to the client, so the screen can find the one the server will use.
        ForgingRecipeInput input = new ForgingRecipeInput(menu.getSlot(SmithingAnvilMenu.TOP_SLOT).getItem(),
                menu.getSlot(SmithingAnvilMenu.BOTTOM_SLOT).getItem());
        recipe = minecraft.level.getRecipeManager().getRecipeFor(ModRecipes.FORGING.get(), input, minecraft.level)
                .map(RecipeHolder::value);
        // A recipe with a rating range (Fracaso: Unique or Legendary) shows the odds within it.
        ForgingExtras extras = recipe.map(ForgingRecipe::extras).orElse(ForgingExtras.NONE);
        // The hammer in the off hand counts in both, as when the server forges.
        double hammer = SmithingHammers.skillBonus(minecraft.level.registryAccess(), minecraft.player.getOffhandItem());
        autoOdds = RatingRoller.odds(stats.dexterity(), smithing, hammer, extras.minRating(), extras.maxRating());
        manualOdds = RatingRoller.odds(stats.dexterity(), smithing, ForgingRules.manualSkillBonus(1) + hammer, extras.minRating(), extras.maxRating());
    }

    /** "×5" (or "Máx") while idle, "3/10" while a batch runs; the tooltip says what it does and the next unlock. */
    private void updateBatchButton(int smithing) {
        int choice = menu.batchChoice();
        if (menu.batchCount() > 1) {
            batchButton.setMessage(Component.literal(Math.min(menu.batchDone() + 1, menu.batchCount()) + "/" + menu.batchCount()));
        } else {
            batchButton.setMessage(batchLabel(choice));
        }
        int next = choice + 1;
        while (next < ForgingRules.BATCH_SIZES.length && ForgingRules.batchUnlocked(next, smithing)) {
            next++;
        }
        String key = choice + ":" + next;
        if (key.equals(batchTooltipKey)) {
            return;
        }
        batchTooltipKey = key;
        MutableComponent text = Component.translatable(ForgingRules.batchUnlocked(1, smithing)
                ? "screen.overgearedmod.batch.tooltip" : "screen.overgearedmod.batch.locked");
        if (next < ForgingRules.BATCH_SIZES.length) {
            text.append("\n").append(Component.translatable("screen.overgearedmod.batch.next",
                    batchLabel(next), ForgingRules.BATCH_UNLOCK_LEVELS[next]).withStyle(s -> s.withColor(UiStyle.MUTED)));
        }
        batchButton.setTooltip(Tooltip.create(text));
    }

    private static Component batchLabel(int index) {
        int size = ForgingRules.BATCH_SIZES[index];
        return size == ForgingRules.BATCH_ALL ? Component.translatable("screen.overgearedmod.batch.all") : Component.literal("×" + size);
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
        if (menu.autoForging()) {
            UiStyle.bar(graphics, leftPos + 81, topPos + 52, 22, 2, menu.autoProgress(), UiStyle.XP);
        }
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
        int left = x + PADDING;
        int right = x + SIDE_WIDTH - PADDING;
        // A long class name ("Legendary Smith Successor") wraps, leaving room for the tick on its first line.
        List<FormattedCharSequence> classLines = requiredClassText()
                .map(text -> font.split(text, right - left - font.width("✘") - 4))
                .orElse(List.of());
        int lines = 3 + autoOdds.length + classLines.size()
                + recipe.map(r -> 2 + r.requirements().map(req -> 1 + req.stats().size()).orElse(1)).orElse(0);
        UiStyle.frame(graphics, x, y, SIDE_WIDTH, PADDING * 2 + lines * LINE, 1.0f);
        int autoColumn = right - 44;
        int row = y + PADDING;

        graphics.drawString(font, Component.translatable("screen.overgearedmod.anvil_odds"), left, row, UiStyle.HEADING);
        row += LINE;
        rightAligned(graphics, Component.translatable("screen.overgearedmod.anvil_col_auto"), autoColumn, row, UiStyle.MUTED);
        rightAligned(graphics, Component.translatable("screen.overgearedmod.anvil_col_manual"), right, row, UiStyle.MUTED);
        row += LINE;
        for (ItemRating rating : ItemRating.values()) {
            graphics.drawString(font, rating.displayName(), left, row, UiStyle.TEXT);
            rightAligned(graphics, Component.literal(percent(autoOdds[rating.ordinal()])), autoColumn, row, UiStyle.TEXT);
            rightAligned(graphics, Component.literal(percent(manualOdds[rating.ordinal()])), right, row, UiStyle.GOLD);
            row += LINE;
        }
        graphics.drawString(font, Component.translatable("screen.overgearedmod.anvil_manual_note"), left, row, UiStyle.MUTED);
        row += LINE;

        if (recipe.isEmpty()) {
            return;
        }
        row += LINE;
        graphics.drawString(font, Component.translatable("screen.overgearedmod.anvil_requirements"), left, row, UiStyle.HEADING);
        row += LINE;
        if (!classLines.isEmpty()) {
            Optional<ResourceLocation> requiredClass = recipe.get().extras().requiredClass();
            boolean isClass = minecraft.player.getData(ModAttachments.PLAYER_IDENTITY).playerClass().equals(requiredClass);
            int color = isClass ? UiStyle.GOOD : UiStyle.BAD;
            String mark = isClass ? "✔" : "✘";
            graphics.drawString(font, mark, right - font.width(mark), row, color);
            for (FormattedCharSequence line : classLines) {
                graphics.drawString(font, line, left, row, color);
                row += LINE;
            }
        }
        Optional<EquipRequirements> requirements = recipe.get().requirements();
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

    /** "Only: <class>" when the recipe is for one class (only the Legendary Smith Successor can use Fracaso's blueprint). */
    private Optional<Component> requiredClassText() {
        return recipe.flatMap(r -> r.extras().requiredClass()).map(id -> {
            Component name = Definitions.get(minecraft.level, ModDatapackRegistries.PLAYER_CLASS, id)
                    .map(PlayerClassDefinition::name).orElse(Component.literal(id.toString()));
            return Component.translatable("screen.overgearedmod.anvil_class", name);
        });
    }

    private void rightAligned(GuiGraphics graphics, Component text, int right, int y, int color) {
        graphics.drawString(font, text, right - font.width(text), y, color);
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
        if (minigame == null) {
            renderTooltip(graphics, mouseX, mouseY);
        } else {
            renderMinigame(graphics, Util.getMillis());
        }
    }

    // ---- Manual forging minigame ----

    private void startMinigame() {
        pressButton(SmithingAnvilMenu.MANUAL_FORGE_BUTTON); // the server notes when it started
        minigame = new ForgingMinigame(0.5 + minecraft.player.getRandom().nextDouble() * 0.3);
        resultShownUntil = -1;
    }

    private void press(long now) {
        if (minigame.phase() == ForgingMinigame.Phase.HEAT) {
            minigame.startHeating(now);
        } else if (minigame.phase() == ForgingMinigame.Phase.HAMMER) {
            minigame.strike(now);
            minecraft.player.playSound(SoundEvents.ANVIL_PLACE, 0.4f, 0.8f + (float) minigame.lastStrike() * 0.6f);
        }
    }

    private void release(long now) {
        minigame.stopHeating(now);
    }

    /** Sends the result once the last strike lands, then leaves it on screen for a moment. */
    private void updateMinigame(long now) {
        minigame.update(now);
        if (minigame.phase() == ForgingMinigame.Phase.DONE && resultShownUntil < 0) {
            PacketDistributor.sendToServer(new ManualForgePayload(menu.containerId, (float) minigame.precision()));
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

        List<FormattedCharSequence> instructions = font.split(instruction(), GAME_WIDTH - 2 * PADDING);
        int panelHeight = PADDING + LINE + 4 + instructions.size() * LINE + 6 + 10 + 6 + LINE + LINE + PADDING;
        int x = (width - GAME_WIDTH) / 2;
        int y = (height - panelHeight) / 2;
        int center = x + GAME_WIDTH / 2;
        UiStyle.panel(graphics, x, y, GAME_WIDTH, panelHeight);

        int row = y + PADDING;
        graphics.drawCenteredString(font, Component.translatable("minigame.overgearedmod.title"), center, row, UiStyle.HEADING);
        row += LINE;
        graphics.fill(x + PADDING, row, x + GAME_WIDTH - PADDING, row + 1, UiStyle.LINE_SOFT);
        row += 4;
        for (FormattedCharSequence line : instructions) {
            graphics.drawCenteredString(font, line, center, row, UiStyle.TEXT);
            row += LINE;
        }
        row += 6;
        drawBar(graphics, center - BAR_WIDTH / 2, row, now);
        row += 10 + 6;
        Component feedback = feedback();
        if (feedback != null) {
            graphics.drawCenteredString(font, feedback, center, row, UiStyle.TEXT);
        }
        row += LINE;
        graphics.drawCenteredString(font, Component.translatable("minigame.overgearedmod.cancel"), center, row, UiStyle.MUTED);
        graphics.pose().popPose();
    }

    private Component instruction() {
        return switch (minigame.phase()) {
            case HEAT -> Component.translatable("minigame.overgearedmod.heat");
            case HAMMER -> Component.translatable("minigame.overgearedmod.hammer", minigame.strikesDone() + 1, ForgingMinigame.STRIKES);
            case DONE -> Component.translatable("minigame.overgearedmod.result", Math.round(minigame.precision() * 100));
        };
    }

    /** The temperature bar with its golden band, or the strike bar with its middle zone and the marker. */
    private void drawBar(GuiGraphics graphics, int x, int y, long now) {
        int height = 8;
        graphics.fill(x - 1, y - 1, x + BAR_WIDTH + 1, y + height + 1, UiStyle.LINE_SOFT);
        graphics.fill(x, y, x + BAR_WIDTH, y + height, SLOT_BACKGROUND);
        if (minigame.phase() == ForgingMinigame.Phase.HEAT) {
            int zoneLeft = x + (int) (minigame.zoneStart() * BAR_WIDTH);
            graphics.fill(zoneLeft, y, zoneLeft + (int) (ForgingMinigame.ZONE_WIDTH * BAR_WIDTH), y + height, 0x90FFD700);
            int filled = (int) (minigame.temperature(now) * BAR_WIDTH);
            graphics.fillGradient(x, y + 2, x + filled, y + height - 2, HEAT_COLD, HEAT_HOT);
        } else {
            int middle = x + BAR_WIDTH / 2;
            int perfect = (int) (ForgingMinigame.PERFECT_MARGIN * BAR_WIDTH);
            graphics.fill(middle - BAR_WIDTH / 6, y, middle + BAR_WIDTH / 6, y + height, 0x4055FF55);
            graphics.fill(middle - perfect, y, middle + perfect, y + height, 0x90FFD700);
            int marker = x + (int) (minigame.markerPosition(now) * BAR_WIDTH);
            graphics.fill(marker - 1, y - 2, marker + 1, y + height + 2, UiStyle.LINE_BRIGHT);
        }
    }

    /** How the last strike (or the heating) went, or null before there is anything to say. */
    private Component feedback() {
        double score = minigame.strikesDone() > 0 ? minigame.lastStrike()
                : minigame.phase() == ForgingMinigame.Phase.HAMMER ? minigame.heatScore() : -1;
        if (score < 0) {
            return null;
        }
        if (score >= 0.95) {
            return Component.translatable("minigame.overgearedmod.perfect").withStyle(s -> s.withColor(UiStyle.GOLD));
        }
        if (score >= 0.6) {
            return Component.translatable("minigame.overgearedmod.good").withStyle(s -> s.withColor(UiStyle.GOOD));
        }
        if (score >= 0.3) {
            return Component.translatable("minigame.overgearedmod.fair");
        }
        return Component.translatable("minigame.overgearedmod.miss").withStyle(s -> s.withColor(UiStyle.BAD));
    }

    // While the minigame is open it takes every click and key, so nothing else in the anvil reacts.

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (minigame != null) {
            if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
                press(Util.getMillis());
            }
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (minigame != null) {
            if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
                release(Util.getMillis());
            }
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
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
            } else if (keyCode == GLFW.GLFW_KEY_SPACE && !spaceDown) {
                spaceDown = true;
                press(Util.getMillis());
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
                release(Util.getMillis());
                return true;
            }
        }
        return super.keyReleased(keyCode, scanCode, modifiers);
    }
}
