package com.orijua.overgearedmod.client.tab;

import com.orijua.overgearedmod.client.ui.SatisfyTab;
import com.orijua.overgearedmod.client.ui.UiStyle;
import com.orijua.overgearedmod.goal.Goal;
import com.orijua.overgearedmod.goal.PlayerCounters;
import com.orijua.overgearedmod.network.ChooseClassPayload;
import com.orijua.overgearedmod.playerclass.PlayerClassDefinition;
import com.orijua.overgearedmod.progression.PlayerIdentity;
import com.orijua.overgearedmod.progression.ProgressionService;
import com.orijua.overgearedmod.progression.StatType;
import com.orijua.overgearedmod.registry.Definitions;
import com.orijua.overgearedmod.registry.ModAttachments;
import com.orijua.overgearedmod.registry.ModDatapackRegistries;
import com.orijua.overgearedmod.title.TitleDefinition;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/** Class (choose once) on top, titles with their progress below. */
public class ProfileTab extends SatisfyTab {
    private static final int TITLES_TOP = 64;
    private static final int TITLE_ROW = 12;

    public ProfileTab() {
        super(Component.translatable("tab.overgearedmod.profile"));
    }

    private static PlayerIdentity identity() {
        return player().getData(ModAttachments.PLAYER_IDENTITY);
    }

    private boolean showingClassButtons;

    @Override
    protected void addWidgets(Consumer<AbstractWidget> addWidget) {
        showingClassButtons = identity().playerClass().isEmpty();
        if (!showingClassButtons) {
            return;
        }
        List<Definitions.Entry<PlayerClassDefinition>> classes = Definitions.all(player().level(), ModDatapackRegistries.PLAYER_CLASS);
        int buttonWidth = classes.isEmpty() ? 0 : (width - (classes.size() - 1) * 4) / classes.size();
        for (int i = 0; i < classes.size(); i++) {
            Definitions.Entry<PlayerClassDefinition> entry = classes.get(i);
            Button button = Button.builder(entry.value().name(), b -> PacketDistributor.sendToServer(new ChooseClassPayload(entry.id())))
                    .bounds(left + i * (buttonWidth + 4), top + 14, buttonWidth, 18)
                    .tooltip(Tooltip.create(classTooltip(entry.value())))
                    .build();
            addWidget.accept(button);
        }
    }

    @Override
    public boolean needsRebuild() {
        return showingClassButtons && identity().playerClass().isPresent();
    }

    private static Component classTooltip(PlayerClassDefinition definition) {
        MutableComponent text = definition.description().copy();
        if (!definition.bonusStats().isEmpty()) {
            text.append("\n").append(bonusText(definition.bonusStats()));
        }
        if (definition.smithingBonus() > 0) {
            text.append("\n").append(Component.translatable("screen.overgearedmod.smithing_bonus", definition.smithingBonus()));
        }
        return text;
    }

    private static Component bonusText(Map<StatType, Integer> bonuses) {
        MutableComponent text = Component.empty();
        bonuses.forEach((stat, value) -> text.append(Component.literal("+" + value + " ")).append(stat.displayName()).append("  "));
        return text;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY) {
        UiStyle.heading(graphics, font(), Component.translatable("screen.overgearedmod.class"), left, top);
        ProgressionService.playerClass(player()).ifPresentOrElse(definition -> {
            UiStyle.text(graphics, font(), definition.name(), left + 50, top, UiStyle.TEXT);
            graphics.drawWordWrap(font(), definition.description(), left, top + 14, width, UiStyle.MUTED);
            UiStyle.text(graphics, font(), bonusText(definition.bonusStats()), left, top + 36, UiStyle.GOOD);
        }, () -> UiStyle.text(graphics, font(), Component.translatable("screen.overgearedmod.choose_class"), left + 50, top, UiStyle.MUTED));

        UiStyle.heading(graphics, font(), Component.translatable("screen.overgearedmod.titles"), left, top + TITLES_TOP - 14);
        PlayerIdentity identity = identity();
        PlayerCounters counters = player().getData(ModAttachments.PLAYER_COUNTERS);
        List<Definitions.Entry<TitleDefinition>> titles = Definitions.all(player().level(), ModDatapackRegistries.TITLE);
        for (int i = 0; i < titles.size(); i++) {
            TitleDefinition title = titles.get(i).value();
            int y = top + TITLES_TOP + i * TITLE_ROW;
            boolean earned = identity.hasTitle(titles.get(i).id());
            UiStyle.text(graphics, font(), Component.literal(earned ? "✔ " : "· ").append(title.name()), left, y, earned ? UiStyle.GOLD : UiStyle.MUTED);
            Goal goal = title.goal();
            Component progress = earned
                    ? bonusText(title.bonusStats())
                    : Component.literal(Math.min(goal.target(), goal.progress(counters, ProgressionService.get(player()))) + "/" + goal.target());
            UiStyle.text(graphics, font(), progress, left + width / 2, y, earned ? UiStyle.GOOD : UiStyle.MUTED);
            if (mouseX >= left && mouseX < left + width && mouseY >= y - 1 && mouseY < y + TITLE_ROW - 1) {
                graphics.renderTooltip(font(), List.of(title.description().getVisualOrderText(), goal.describe().getVisualOrderText()), mouseX, mouseY);
            }
        }
    }
}
