package com.orijua.overgearedmod.client;

import com.orijua.overgearedmod.client.ui.UiStyle;
import com.orijua.overgearedmod.registry.Definitions;
import com.orijua.overgearedmod.registry.ModAttachments;
import com.orijua.overgearedmod.registry.ModDatapackRegistries;
import com.orijua.overgearedmod.skill.PlayerSkills;
import com.orijua.overgearedmod.skill.SkillDefinition;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.LayeredDraw;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.util.Optional;

/** Three skill slots right of the hotbar: bound key, skill icon (or initial), and a dark overlay while on cooldown. */
final class SkillSlotsHudLayer implements LayeredDraw.Layer {
    private static final int SIZE = 20;
    private static final int GAP = 2;
    /** Empty slots are drawn as a fainter Satisfy frame. */
    private static final float EMPTY_ALPHA = 0.45f;
    private static final int COOLDOWN = 0xB0101828;

    @Override
    public void render(GuiGraphics graphics, DeltaTracker deltaTracker) {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null || minecraft.options.hideGui || player.isSpectator()) {
            return;
        }
        PlayerSkills skills = player.getData(ModAttachments.PLAYER_SKILLS);
        long now = player.level().getGameTime();
        int x = graphics.guiWidth() / 2 + 91 + 8;
        int y = graphics.guiHeight() - SIZE - 1;

        for (int slot = 0; slot < PlayerSkills.SLOT_COUNT; slot++) {
            int left = x + slot * (SIZE + GAP);
            Optional<ResourceLocation> id = skills.slot(slot);
            Optional<SkillDefinition> skill = id.flatMap(s -> Definitions.get(player.level(), ModDatapackRegistries.SKILL, s));

            UiStyle.frame(graphics, left, y, SIZE, SIZE, skill.isPresent() ? 1.0f : EMPTY_ALPHA);
            skill.ifPresent(def -> {
                ItemStack icon = def.iconStack();
                if (icon.isEmpty()) {
                    String name = def.name().getString();
                    graphics.drawCenteredString(minecraft.font, name.isEmpty() ? "?" : name.substring(0, 1), left + SIZE / 2, y + 6, UiStyle.TEXT);
                } else {
                    graphics.renderItem(icon, left + 2, y + 2);
                }
                long remaining = skills.cooldownRemaining(id.get(), now);
                if (remaining > 0 && def.cooldownTicks() > 0) {
                    int height = Math.round(SIZE * remaining / (float) def.cooldownTicks());
                    // Items are drawn at a higher depth; lift the overlay above them.
                    graphics.pose().pushPose();
                    graphics.pose().translate(0, 0, 200);
                    graphics.fill(left, y + SIZE - height, left + SIZE, y + SIZE, COOLDOWN);
                    graphics.pose().popPose();
                }
            });
            Component key = OvergearedmodClient.SKILL_KEYS.get(slot).getTranslatedKeyMessage();
            graphics.drawCenteredString(minecraft.font, key, left + SIZE / 2, y - 9, UiStyle.HEADING);
        }
    }
}
