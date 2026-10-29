package com.orijua.overgearedmod.client;

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

import java.util.Optional;

/** Three skill slots right of the hotbar: bound key, skill initial, and a dark overlay while on cooldown. */
final class SkillSlotsHudLayer implements LayeredDraw.Layer {
    private static final int SIZE = 20;
    private static final int GAP = 2;
    private static final int EMPTY = 0x80000000;
    private static final int FILLED = 0xC0203060;
    private static final int COOLDOWN = 0xB0000000;

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

            graphics.fill(left, y, left + SIZE, y + SIZE, skill.isPresent() ? FILLED : EMPTY);
            graphics.renderOutline(left, y, SIZE, SIZE, 0xFF5A5AA0);
            skill.ifPresent(def -> {
                String name = def.name().getString();
                String initial = name.isEmpty() ? "?" : name.substring(0, 1);
                graphics.drawCenteredString(minecraft.font, initial, left + SIZE / 2, y + 6, 0xFFFFFF);
                long remaining = skills.cooldownRemaining(id.get(), now);
                if (remaining > 0 && def.cooldownTicks() > 0) {
                    int height = Math.round(SIZE * remaining / (float) def.cooldownTicks());
                    graphics.fill(left, y + SIZE - height, left + SIZE, y + SIZE, COOLDOWN);
                }
            });
            Component key = OvergearedmodClient.SKILL_KEYS.get(slot).getTranslatedKeyMessage();
            graphics.drawString(minecraft.font, key, left + 2, y - 9, 0xA0A0A0);
        }
    }
}
