package com.orijua.overgearedmod.client.tab;

import com.orijua.overgearedmod.client.ui.SatisfyTab;
import com.orijua.overgearedmod.client.ui.UiStyle;
import com.orijua.overgearedmod.network.QuestActionPayload;
import com.orijua.overgearedmod.quest.PlayerQuests;
import com.orijua.overgearedmod.quest.QuestDefinition;
import com.orijua.overgearedmod.quest.QuestService;
import com.orijua.overgearedmod.registry.Definitions;
import com.orijua.overgearedmod.registry.ModDatapackRegistries;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/** Every quest with its state: accept it, follow its progress, claim it when done. */
public class QuestsTab extends SatisfyTab {
    private static final int ROW = 22;
    private static final int LIST_TOP = 16;
    private static final int MAX_ROWS = 7;
    private static final int BUTTON_WIDTH = 64;

    private record Row(Definitions.Entry<QuestDefinition> quest, Button button) {
    }

    private final List<Row> rows = new ArrayList<>();
    private PlayerQuests shownState;

    public QuestsTab() {
        super(Component.translatable("tab.overgearedmod.quests"));
    }

    @Override
    protected void addWidgets(Consumer<AbstractWidget> addWidget) {
        rows.clear();
        shownState = QuestService.get(player());
        List<Definitions.Entry<QuestDefinition>> quests = Definitions.all(player().level(), ModDatapackRegistries.QUEST);
        for (int i = 0; i < Math.min(quests.size(), MAX_ROWS); i++) {
            Definitions.Entry<QuestDefinition> entry = quests.get(i);
            Button button = actionButton(entry, top + LIST_TOP + i * ROW - 4);
            addWidget.accept(button);
            rows.add(new Row(entry, button));
        }
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
            boolean available = QuestService.canAccept(state, entry.id(), entry.value());
            label = available ? Component.translatable("screen.overgearedmod.quest_accept") : Component.literal("✔");
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
    }

    private static List<FormattedCharSequence> tooltip(QuestDefinition quest) {
        List<FormattedCharSequence> lines = new ArrayList<>(font().split(quest.description(), 180));
        lines.add(quest.goal().describe().getVisualOrderText());
        lines.add(Component.translatable("screen.overgearedmod.quest_reward", quest.xpReward(), quest.itemRewards().size()).getVisualOrderText());
        return lines;
    }
}
