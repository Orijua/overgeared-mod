package com.orijua.overgearedmod.client;

import com.orijua.overgearedmod.progression.PlayerStats;
import com.orijua.overgearedmod.registry.ModAttachments;
import com.orijua.overgearedmod.skill.Skill;
import com.orijua.overgearedmod.skill.SkillState;
import com.orijua.overgearedmod.skill.Skills;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.LayeredDraw;
import net.minecraft.client.player.LocalPlayer;

/**
 * Mana bar above the food bar, plus a thin line under it that shrinks while the held weapon's
 * skill is on cooldown. Registered right above vanilla's food layer, so it takes the next "row"
 * on the right side ({@link Gui#rightHeight}) and the air bubbles move up above it.
 */
public final class ManaHudLayer implements LayeredDraw.Layer {
    private static final int WIDTH = 81;          // same width as the food bar
    private static final int ROW_HEIGHT = 10;     // vanilla spacing between status rows
    private static final int BORDER = 0xFF000000;
    private static final int EMPTY = 0xFF1A2440;
    private static final int MANA = 0xFF3F8CFF;
    private static final int COOLDOWN = 0xFFFFC23F;

    @Override
    public void render(GuiGraphics graphics, DeltaTracker deltaTracker) {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        // Same visibility rule as the vanilla health/food bars (hidden in creative, spectator, F1).
        if (player == null || minecraft.options.hideGui || minecraft.gameMode == null || !minecraft.gameMode.canHurtPlayer()) {
            return;
        }

        PlayerStats stats = player.getData(ModAttachments.PLAYER_STATS);
        SkillState state = player.getData(ModAttachments.SKILL_STATE);
        Gui gui = minecraft.gui;

        int right = graphics.guiWidth() / 2 + 91;
        int left = right - WIDTH;
        int top = graphics.guiHeight() - gui.rightHeight;
        gui.rightHeight += ROW_HEIGHT;

        // Mana: 1px black border around a 5px bar.
        float manaFraction = stats.maxMana() <= 0 ? 0 : Math.min(1, state.mana() / stats.maxMana());
        graphics.fill(left - 1, top, right + 1, top + 7, BORDER);
        graphics.fill(left, top + 1, right, top + 6, EMPTY);
        graphics.fill(right - Math.round(WIDTH * manaFraction), top + 1, right, top + 6, MANA); // fills from the right, like food

        // Cooldown of the skill of the held weapon, if any.
        long remaining = state.cooldownRemaining(player.level().getGameTime());
        if (remaining > 0) {
            int total = Skills.forHeldItem(player.getMainHandItem()).map(Skill::cooldownTicks).orElse((int) remaining);
            float cooldownFraction = Math.min(1, remaining / (float) Math.max(1, total));
            graphics.fill(right - Math.round(WIDTH * cooldownFraction), top + 7, right, top + 9, COOLDOWN);
        }
    }
}
