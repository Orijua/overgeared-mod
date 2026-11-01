package com.orijua.overgearedmod.client.tab;

import com.orijua.overgearedmod.Balance;
import com.orijua.overgearedmod.client.ui.SatisfyButton;
import com.orijua.overgearedmod.client.ui.SatisfyTab;
import com.orijua.overgearedmod.insight.InsightRules;
import com.orijua.overgearedmod.insight.Observation;
import com.orijua.overgearedmod.client.ui.UiStyle;
import com.orijua.overgearedmod.network.AllocateStatPayload;
import com.orijua.overgearedmod.progression.PlayerStats;
import com.orijua.overgearedmod.progression.ProgressionRules;
import com.orijua.overgearedmod.progression.ProgressionService;
import com.orijua.overgearedmod.progression.StatEffects;
import com.orijua.overgearedmod.progression.StatModifier;
import com.orijua.overgearedmod.progression.StatSource;
import com.orijua.overgearedmod.progression.StatType;
import com.orijua.overgearedmod.registry.ModAttachments;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
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
            Button button = Button.builder(Component.literal("+"),
                            b -> PacketDistributor.sendToServer(new AllocateStatPayload(stat, pointsPerClick())))
                    .bounds(left + width - 18, top + STATS_TOP + i * STAT_ROW - 4, 16, 16)
                    .tooltip(Tooltip.create(Component.translatable("screen.overgearedmod.allocate_hint")))
                    .build(SatisfyButton::new);
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
        Component xpText = Component.translatable("screen.overgearedmod.xp", stats.xp(), xpNeeded);
        int xpTextX = left + width - font().width(xpText);
        UiStyle.bar(graphics, half, top + 2, xpTextX - 6 - half, 4, stats.xp() / (float) xpNeeded, UiStyle.XP);
        UiStyle.text(graphics, font(), xpText, xpTextX, top, UiStyle.TEXT);

        UiStyle.text(graphics, font(), Component.translatable("screen.overgearedmod.mana", (int) mana, (int) ProgressionRules.maxMana(Balance.current(), effective.intelligence())), left, top + 16, UiStyle.TEXT);
        UiStyle.text(graphics, font(), Component.translatable("screen.overgearedmod.smithing", ProgressionService.effectiveSmithingLevel(player())), half, top + 16, UiStyle.MUTED);
        UiStyle.text(graphics, font(), Component.translatable("screen.overgearedmod.free_points", stats.freePoints()), left, top + 30,
                stats.freePoints() > 0 ? UiStyle.GOOD : UiStyle.MUTED);
        Component insight = Component.translatable("screen.overgearedmod.insight", Observation.insight(player()));
        UiStyle.text(graphics, font(), insight, half, top + 30, UiStyle.MUTED);
        boolean overInsight = mouseX >= half && mouseX < half + font().width(insight) && mouseY >= top + 28 && mouseY < top + 40;

        StatType[] types = StatType.values();
        StatType hovered = null;
        for (int i = 0; i < types.length; i++) {
            int y = top + STATS_TOP + i * STAT_ROW;
            UiStyle.text(graphics, font(), types[i].displayName(), left, y, UiStyle.TEXT);
            UiStyle.text(graphics, font(), Component.literal(String.valueOf(stats.get(types[i]))), left + width - 60, y, UiStyle.GOLD);
            int bonus = effective.get(types[i]) - stats.get(types[i]);
            if (bonus > 0) {
                UiStyle.text(graphics, font(), Component.literal("+" + bonus), left + width - 42, y, UiStyle.GOOD);
            }
            if (mouseX >= left && mouseX < left + width - 20 && mouseY >= y - 4 && mouseY < y + STAT_ROW - 4) {
                hovered = types[i];
            }
        }
        if (hovered != null) {
            graphics.renderTooltip(font(), tooltip(hovered), mouseX, mouseY);
        } else if (overInsight) {
            graphics.renderTooltip(font(), font().split(Component.translatable("screen.overgearedmod.insight.tooltip",
                    InsightRules.OBSERVE_TICKS / 20), 200), mouseX, mouseY);
        }
    }

    /** Control: 100 points, Shift: 10, plain click: 1. The server spends only what's left. */
    private static int pointsPerClick() {
        return Screen.hasControlDown() ? 100 : Screen.hasShiftDown() ? 10 : 1;
    }

    /** What one point does, then where the current value comes from ("Allocated: 5", "Warrior: +3"...). */
    private List<FormattedCharSequence> tooltip(StatType stat) {
        List<FormattedCharSequence> lines = new ArrayList<>(font().split(describe(stat), 200));
        List<Component> sources = new ArrayList<>();
        for (StatModifier modifier : ProgressionService.modifiers(player())) {
            if (modifier.stat() == stat && modifier.value() != 0) {
                sources.add(Component.translatable("screen.overgearedmod.stat_source", modifier.source().name(), amount(modifier))
                        .withStyle(ChatFormatting.GRAY));
            }
        }
        if (!sources.isEmpty()) {
            lines.add(FormattedCharSequence.EMPTY);
            lines.add(Component.translatable("screen.overgearedmod.stat_sources").getVisualOrderText());
            sources.forEach(line -> lines.add(line.getVisualOrderText()));
        }
        return lines;
    }

    /** "5" for allocated points, "+3" for a flat bonus, "+10%" for a percentage. */
    private static String amount(StatModifier modifier) {
        String sign = modifier.source() == StatSource.ALLOCATED || modifier.value() < 0 ? "" : "+";
        return switch (modifier.operation()) {
            case ADD -> sign + number(modifier.value());
            case MULTIPLY -> sign + number(modifier.value() * 100) + "%";
        };
    }

    /** "Per point: ..." built from the real numbers in StatEffects and Balance, so it never goes stale. */
    private static Component describe(StatType stat) {
        Balance balance = Balance.current();
        String key = "stat.overgearedmod." + stat.getSerializedName() + ".desc";
        return switch (stat) {
            case STRENGTH, STAMINA -> Component.translatable(key, number(StatEffects.perPoint(stat)));
            case AGILITY, DEXTERITY -> Component.translatable(key, number(StatEffects.perPoint(stat) * 100));
            case INTELLIGENCE -> Component.translatable(key, number(balance.manaPerIntelligence()), number(balance.manaRegenPerIntelligence()));
        };
    }

    private static String number(double value) {
        return value == Math.rint(value) ? String.valueOf((long) value) : String.valueOf(Math.round(value * 100) / 100.0);
    }
}
