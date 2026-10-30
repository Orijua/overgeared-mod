package com.orijua.overgearedmod.client.tab;

import com.orijua.overgearedmod.client.ui.SatisfyTab;
import com.orijua.overgearedmod.client.ui.ScrollList;
import com.orijua.overgearedmod.client.ui.UiStyle;
import com.orijua.overgearedmod.network.QuestActionPayload;
import com.orijua.overgearedmod.playerclass.PlayerClassDefinition;
import com.orijua.overgearedmod.quest.PlayerQuests;
import com.orijua.overgearedmod.quest.QuestDefinition;
import com.orijua.overgearedmod.quest.QuestService;
import com.orijua.overgearedmod.registry.Definitions;
import com.orijua.overgearedmod.registry.ModAttachments;
import com.orijua.overgearedmod.registry.ModDatapackRegistries;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

/** Every quest with its state: accept it, follow its progress, claim it when done. */
public class QuestsTab extends SatisfyTab {
    private static final int ROW = 22;
    private static final int LIST_TOP = 16;
    private static final int VISIBLE_ROWS = 7;
    private static final int BUTTON_WIDTH = 64;

    private record Row(Definitions.Entry<QuestDefinition> quest, Button button) {
    }

    private final List<Row> rows = new ArrayList<>();
    private PlayerQuests shownState;
    private final ScrollList scroll = new ScrollList(VISIBLE_ROWS);
    private int questCount;

    public QuestsTab() {
        super(Component.translatable("tab.overgearedmod.quests"));
    }

    @Override
    protected void addWidgets(Consumer<AbstractWidget> addWidget) {
        rows.clear();
        shownState = QuestService.get(player());
        // Hidden quests stay out of the list (not even locked) until the player discovers them.
        List<Definitions.Entry<QuestDefinition>> quests = Definitions.all(player().level(), ModDatapackRegistries.QUEST).stream()
                .filter(entry -> entry.value().isVisibleTo(entry.id(), shownState))
                .toList();
        questCount = quests.size();
        scroll.clamp(questCount);
        for (int i = scroll.first(); i < scroll.end(questCount); i++) {
            Definitions.Entry<QuestDefinition> entry = quests.get(i);
            int y = top + LIST_TOP + rows.size() * ROW - 4;
            Button button = actionButton(entry, y);
            addWidget.accept(button);
            if (shownState.isActive(entry.id())) {
                addWidget.accept(abandonButton(entry, y));
            }
            rows.add(new Row(entry, button));
        }
    }

    private Button abandonButton(Definitions.Entry<QuestDefinition> entry, int y) {
        return Button.builder(Component.literal("✖"),
                        b -> PacketDistributor.sendToServer(new QuestActionPayload(entry.id(), QuestActionPayload.Action.ABANDON)))
                .bounds(left + width - BUTTON_WIDTH - 18, y, 16, 16)
                .tooltip(Tooltip.create(Component.translatable("screen.overgearedmod.quest_abandon")))
                .build();
    }

    private Button actionButton(Definitions.Entry<QuestDefinition> entry, int y) {
        PlayerQuests state = shownState;
        boolean active = state.isActive(entry.id());
        Component label;
        boolean enabled;
        QuestActionPayload.Action action;
        if (active) {
            label = Component.translatable("screen.overgearedmod.quest_claim");
            enabled = isDone(entry, state);
            action = QuestActionPayload.Action.CLAIM;
        } else {
            boolean available = QuestService.canAccept(player(), entry.id(), entry.value());
            boolean locked = !entry.value().allowsClass(player().getData(ModAttachments.PLAYER_IDENTITY).playerClass())
                    || !entry.value().isUnlockedBy(state);
            label = available ? Component.translatable("screen.overgearedmod.quest_accept")
                    : locked ? Component.translatable("screen.overgearedmod.quest_locked")
                    : Component.literal("✔");
            enabled = available;
            action = QuestActionPayload.Action.ACCEPT;
        }
        Button button = Button.builder(label, b -> PacketDistributor.sendToServer(new QuestActionPayload(entry.id(), action)))
                .bounds(left + width - BUTTON_WIDTH, y, BUTTON_WIDTH, 16)
                .build();
        button.active = enabled;
        return button;
    }

    private boolean isDone(Definitions.Entry<QuestDefinition> entry, PlayerQuests state) {
        Integer baseline = state.active().get(entry.id());
        return baseline != null && QuestService.progress(player(), entry.value(), baseline) >= entry.value().goal().target();
    }

    @Override
    public boolean mouseScrolled(double wheel) {
        return scroll.scroll(wheel, questCount);
    }

    /** Buttons depend on quest state and progress, so rebuild whenever either changes. */
    @Override
    public boolean needsRebuild() {
        PlayerQuests state = QuestService.get(player());
        if (!state.equals(shownState)) {
            return true;
        }
        return rows.stream().anyMatch(row -> state.isActive(row.quest().id()) && row.button().active != isDone(row.quest(), state));
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY) {
        if (rows.isEmpty()) {
            UiStyle.text(graphics, font(), Component.translatable("screen.overgearedmod.no_quests"), left, top, UiStyle.MUTED);
            return;
        }
        UiStyle.heading(graphics, font(), Component.translatable("tab.overgearedmod.quests"), left, top);
        PlayerQuests state = QuestService.get(player());
        for (int i = 0; i < rows.size(); i++) {
            Definitions.Entry<QuestDefinition> entry = rows.get(i).quest();
            QuestDefinition quest = entry.value();
            int y = top + LIST_TOP + i * ROW;
            UiStyle.text(graphics, font(), quest.name(), left, y, UiStyle.TEXT);

            Integer baseline = state.active().get(entry.id());
            if (baseline != null) {
                int progress = QuestService.progress(player(), quest, baseline);
                UiStyle.text(graphics, font(), Component.literal(progress + "/" + quest.goal().target()), left + 124, y,
                        progress >= quest.goal().target() ? UiStyle.GOOD : UiStyle.MUTED);
            }
            if (mouseX >= left && mouseX < left + width - BUTTON_WIDTH && mouseY >= y - 2 && mouseY < y + ROW - 4) {
                graphics.renderTooltip(font(), tooltip(quest), mouseX, mouseY);
            }
        }
        scroll.renderBar(graphics, left + width + 3, top + LIST_TOP - 6, VISIBLE_ROWS * ROW - 4, questCount);
    }

    private static List<FormattedCharSequence> tooltip(QuestDefinition quest) {
        List<FormattedCharSequence> lines = new ArrayList<>(font().split(quest.description(), 180));
        lines.add(quest.goal().describe().getVisualOrderText());
        lines.add(Component.translatable("screen.overgearedmod.quest_reward", quest.xpReward(), quest.itemRewards().size()).getVisualOrderText());
        quest.requiredClass().flatMap(QuestsTab::className).ifPresent(name ->
                lines.add(Component.translatable("screen.overgearedmod.quest_requires_class", name).getVisualOrderText()));
        quest.requiresQuest().flatMap(QuestsTab::questName).ifPresent(name ->
                lines.add(Component.translatable("screen.overgearedmod.quest_requires_quest", name).getVisualOrderText()));
        quest.classReward().flatMap(QuestsTab::className).ifPresent(name ->
                lines.add(Component.translatable("screen.overgearedmod.quest_class_reward", name).getVisualOrderText()));
        return lines;
    }

    private static Optional<Component> className(ResourceLocation classId) {
        return Definitions.get(player().level(), ModDatapackRegistries.PLAYER_CLASS, classId).map(PlayerClassDefinition::name);
    }

    private static Optional<Component> questName(ResourceLocation questId) {
        return Definitions.get(player().level(), ModDatapackRegistries.QUEST, questId).map(QuestDefinition::name);
    }
}
