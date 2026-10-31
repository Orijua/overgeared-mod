package com.orijua.overgearedmod.client;

import com.orijua.overgearedmod.client.ui.UiStyle;
import com.orijua.overgearedmod.registry.Definitions;
import com.orijua.overgearedmod.registry.ModAttachments;
import com.orijua.overgearedmod.registry.ModDatapackRegistries;
import com.orijua.overgearedmod.skill.PlayerSkills;
import com.orijua.overgearedmod.skill.SkillDefinition;
import com.orijua.overgearedmod.skill.SkillService;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.LayeredDraw;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.util.Optional;

/**
 * Three skill slots right of the hotbar: bound key, skill icon (or initial), and a dark overlay while on cooldown.
 * A fourth slot, a little apart, shows while the held weapon has its own skill (Bisection on a Legendary Fracaso).
 */
final class SkillSlotsHudLayer implements LayeredDraw.Layer {
    private static final int SIZE = 20;
    private static final int GAP = 2;
    /** Extra space before the weapon-skill slot, so it reads as a separate thing. */
    private static final int WEAPON_GAP = 6;
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
        int x = graphics.guiWidth() / 2 + 91 + 8;
        int y = graphics.guiHeight() - SIZE - 1;

        for (int slot = 0; slot < PlayerSkills.SLOT_COUNT; slot++) {
            drawSlot(graphics, minecraft, player, skills, skills.slot(slot),
                    OvergearedmodClient.SKILL_KEYS.get(slot).getTranslatedKeyMessage(), x + slot * (SIZE + GAP), y);
        }
        Optional<ResourceLocation> weaponSkill = SkillService.weaponSkill(player.getMainHandItem());
        if (weaponSkill.isPresent()) {
            int left = x + PlayerSkills.SLOT_COUNT * (SIZE + GAP) + WEAPON_GAP;
            drawSlot(graphics, minecraft, player, skills, weaponSkill, OvergearedmodClient.WEAPON_SKILL.getTranslatedKeyMessage(), left, y);
        }
    }

    private static void drawSlot(GuiGraphics graphics, Minecraft minecraft, LocalPlayer player, PlayerSkills skills,
                                 Optional<ResourceLocation> id, Component key, int left, int y) {
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
            long remaining = skills.cooldownRemaining(id.get(), player.level().getGameTime());
            if (remaining > 0 && def.cooldownTicks() > 0) {
                int height = Math.round(SIZE * remaining / (float) def.cooldownTicks());
                // Items are drawn at a higher depth; lift the overlay above them.
                graphics.pose().pushPose();
                graphics.pose().translate(0, 0, 200);
                graphics.fill(left, y + SIZE - height, left + SIZE, y + SIZE, COOLDOWN);
                graphics.pose().popPose();
            }
        });
        graphics.drawCenteredString(minecraft.font, key, left + SIZE / 2, y - 9, UiStyle.HEADING);
    }
}
