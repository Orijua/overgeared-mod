package com.orijua.overgearedmod.client;

import com.orijua.overgearedmod.network.AllocateStatPayload;
import com.orijua.overgearedmod.progression.PlayerStats;
import com.orijua.overgearedmod.progression.ProgressionRules;
import com.orijua.overgearedmod.progression.StatType;
import com.orijua.overgearedmod.registry.ModAttachments;
import com.orijua.overgearedmod.skill.SkillState;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;

/**
 * The Satisfy-style status window (key K). Reads the attachments synced to the local player,
 * so it always shows the server's values; "+" only asks the server to spend a point.
 */
public class StatusScreen extends Screen {
    private static final int PANEL_WIDTH = 220;
    private static final int PANEL_HEIGHT = 196;
    private static final int LINE = 12;
    private static final int TEXT = 0xFFFFFF;
    private static final int MUTED = 0xA0A0A0;

    private final List<Button> plusButtons = new ArrayList<>();
    private int left;
    private int top;

    public StatusScreen() {
        super(Component.translatable("screen.overgearedmod.status"));
    }

    private PlayerStats stats() {
        return minecraft.player.getData(ModAttachments.PLAYER_STATS);
    }

    @Override
    protected void init() {
        left = (width - PANEL_WIDTH) / 2;
        top = (height - PANEL_HEIGHT) / 2;
        plusButtons.clear();
        StatType[] types = StatType.values();
        for (int i = 0; i < types.length; i++) {
            StatType stat = types[i];
            Button button = Button.builder(Component.literal("+"),
                            b -> PacketDistributor.sendToServer(new AllocateStatPayload(stat)))
                    .bounds(left + PANEL_WIDTH - 30, statY(i) - 4, 16, 16)
                    .build();
            plusButtons.add(addRenderableWidget(button));
        }
    }

    private int statY(int index) {
        return top + 78 + index * (LINE + 6);
    }

    @Override
    public void tick() {
        boolean hasPoints = stats().freePoints() > 0;
        plusButtons.forEach(button -> button.active = hasPoints);
    }

    /** Screen.render draws this first, then the buttons, so the panel stays behind them. */
    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.renderBackground(graphics, mouseX, mouseY, partialTick);
        graphics.fill(left, top, left + PANEL_WIDTH, top + PANEL_HEIGHT, 0xC0101018);
        graphics.renderOutline(left, top, PANEL_WIDTH, PANEL_HEIGHT, 0xFF5A5AA0);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick); // background + panel + buttons

        PlayerStats stats = stats();
        SkillState skill = minecraft.player.getData(ModAttachments.SKILL_STATE);

        graphics.drawCenteredString(font, title, left + PANEL_WIDTH / 2, top + 8, 0xFFD700);

        int x = left + 12;
        int y = top + 26;
        graphics.drawString(font, Component.translatable("screen.overgearedmod.level", stats.level()), x, y, TEXT);
        graphics.drawString(font, Component.translatable("screen.overgearedmod.xp",
                stats.xp(), ProgressionRules.xpToNextLevel(stats.level())), x + 90, y, MUTED);
        y += LINE;
        graphics.drawString(font, Component.translatable("screen.overgearedmod.mana",
                (int) skill.mana(), (int) stats.maxMana()), x, y, 0x55AAFF);
        graphics.drawString(font, Component.translatable("screen.overgearedmod.smithing", stats.smithingLevel()), x + 90, y, MUTED);
        y += LINE;
        graphics.drawString(font, Component.translatable("screen.overgearedmod.free_points", stats.freePoints()), x, y,
                stats.freePoints() > 0 ? 0x55FF55 : MUTED);

        StatType[] types = StatType.values();
        for (int i = 0; i < types.length; i++) {
            graphics.drawString(font, types[i].displayName(), x, statY(i), TEXT);
            graphics.drawString(font, String.valueOf(stats.get(types[i])), left + PANEL_WIDTH - 60, statY(i), 0xFFD700);
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
