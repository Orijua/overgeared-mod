package com.orijua.overgearedmod.client.tab;

import com.orijua.overgearedmod.Balance;
import com.orijua.overgearedmod.client.ui.SatisfyTab;
import com.orijua.overgearedmod.client.ui.UiStyle;
import com.orijua.overgearedmod.network.AllocateStatPayload;
import com.orijua.overgearedmod.progression.PlayerStats;
import com.orijua.overgearedmod.progression.ProgressionRules;
import com.orijua.overgearedmod.progression.ProgressionService;
import com.orijua.overgearedmod.progression.StatType;
import com.orijua.overgearedmod.registry.ModAttachments;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/** Level, XP, resources and the five stats with "+" buttons. */
public class StatusTab extends SatisfyTab {
    private static final int STATS_TOP = 54;
    private static final int STAT_ROW = 18;

    private final List<Button> plusButtons = new ArrayList<>();

    public StatusTab() {
        super(Component.translatable("tab.overgearedmod.status"));
    }

    private static PlayerStats stats() {
        return player().getData(ModAttachments.PLAYER_STATS);
    }

    @Override
    protected void addWidgets(Consumer<AbstractWidget> addWidget) {
        plusButtons.clear();
        StatType[] types = StatType.values();
        for (int i = 0; i < types.length; i++) {
            StatType stat = types[i];
            Button button = Button.builder(Component.literal("+"), b -> PacketDistributor.sendToServer(new AllocateStatPayload(stat)))
                    .bounds(left + width - 18, top + STATS_TOP + i * STAT_ROW - 4, 16, 16)
                    .build();
            plusButtons.add(button);
            addWidget.accept(button);
        }
        tick();
    }

    @Override
    public void tick() {
        boolean hasPoints = stats().freePoints() > 0;
        plusButtons.forEach(button -> button.active = hasPoints);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY) {
        PlayerStats stats = stats();
        PlayerStats effective = ProgressionService.effectiveStats(player());
        float mana = player().getData(ModAttachments.MANA);
        int half = left + width / 2;

        UiStyle.heading(graphics, font(), Component.translatable("screen.overgearedmod.level", stats.level()), left, top);
        int xpNeeded = ProgressionRules.xpToNextLevel(Balance.current(), stats.level());
        UiStyle.bar(graphics, half, top + 2, width / 2, 4, stats.xp() / (float) xpNeeded, 0xFF55CC55);

        UiStyle.text(graphics, font(), Component.translatable("screen.overgearedmod.mana", (int) mana, (int) ProgressionRules.maxMana(Balance.current(), effective.intelligence())), left, top + 16, UiStyle.TEXT);
        UiStyle.text(graphics, font(), Component.translatable("screen.overgearedmod.smithing", ProgressionService.effectiveSmithingLevel(player())), half, top + 16, UiStyle.MUTED);
        UiStyle.text(graphics, font(), Component.translatable("screen.overgearedmod.free_points", stats.freePoints()), left, top + 30,
                stats.freePoints() > 0 ? UiStyle.GOOD : UiStyle.MUTED);

        StatType[] types = StatType.values();
        for (int i = 0; i < types.length; i++) {
            int y = top + STATS_TOP + i * STAT_ROW;
            UiStyle.text(graphics, font(), types[i].displayName(), left, y, UiStyle.TEXT);
            UiStyle.text(graphics, font(), Component.literal(String.valueOf(stats.get(types[i]))), left + width - 60, y, UiStyle.GOLD);
            int bonus = effective.get(types[i]) - stats.get(types[i]);
            if (bonus > 0) {
                UiStyle.text(graphics, font(), Component.literal("+" + bonus), left + width - 42, y, UiStyle.GOOD);
            }
        }
    }
}
