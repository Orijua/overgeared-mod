package com.orijua.overgearedmod.client;

import com.orijua.overgearedmod.equipment.EquipmentRules;
import com.orijua.overgearedmod.progression.ProgressionService;
import com.orijua.overgearedmod.smithing.SmithingAnvilMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

/**
 * Placeholder look: vanilla furnace background (same slot positions). The "Enhance" button appears
 * when the menu holds equipment + an enhancement stone and shows the success chance.
 */
public class SmithingAnvilScreen extends AbstractContainerScreen<SmithingAnvilMenu> {
    private static final ResourceLocation BACKGROUND = ResourceLocation.withDefaultNamespace("textures/gui/container/furnace.png");

    private Button enhanceButton;

    public SmithingAnvilScreen(SmithingAnvilMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
    }

    @Override
    protected void init() {
        super.init();
        enhanceButton = addRenderableWidget(Button.builder(Component.empty(),
                        b -> minecraft.gameMode.handleInventoryButtonClick(menu.containerId, SmithingAnvilMenu.ENHANCE_BUTTON))
                .bounds(leftPos + 96, topPos + 56, 72, 14)
                .build());
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        enhanceButton.visible = menu.canEnhance();
        if (enhanceButton.visible) {
            int level = menu.currentEnhancement();
            double chance = EquipmentRules.enhancementChance(level, ProgressionService.effectiveStats(minecraft.player).dexterity());
            enhanceButton.setMessage(Component.translatable("screen.overgearedmod.enhance", level + 1, Math.round(chance * 100)));
        }
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.blit(BACKGROUND, leftPos, topPos, 0, 0, imageWidth, imageHeight);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
    }
}
