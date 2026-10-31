package com.orijua.overgearedmod.client.tab;

import com.orijua.overgearedmod.client.ui.SatisfyButton;
import com.orijua.overgearedmod.client.ui.SatisfyConfirmScreen;
import com.orijua.overgearedmod.client.ui.SatisfyTab;
import com.orijua.overgearedmod.client.ui.ScrollList;
import com.orijua.overgearedmod.client.ui.UiStyle;
import com.orijua.overgearedmod.network.QuestActionPayload;
import com.orijua.overgearedmod.network.QuestsSeenPayload;
import com.orijua.overgearedmod.playerclass.PlayerClassDefinition;
import com.orijua.overgearedmod.progression.ProgressionService;
import com.orijua.overgearedmod.quest.PlayerQuests;
import com.orijua.overgearedmod.quest.QuestDefinition;
import com.orijua.overgearedmod.quest.QuestRoute;
import com.orijua.overgearedmod.quest.QuestService;
import com.orijua.overgearedmod.registry.Definitions;
import com.orijua.overgearedmod.registry.ModAttachments;
import com.orijua.overgearedmod.registry.ModDatapackRegistries;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import javax.annotation.Nullable;

/** Every quest with its state: accept it, follow its progress, claim it when done. */
public class QuestsTab extends SatisfyTab {
    private static final int ROW = 22;
    private static final int LIST_TOP = 16;
    private static final int VISIBLE_ROWS = 7;
    private static final int BUTTON_WIDTH = 64;

    /** Height of a step of an unfolded route, under its quest. */
    private static final int STEP_ROW = 11;
    /** Room for the route toggle (▸/▾) before a quest's name. */
    private static final int NAME_X = 14;

    /**
     * A line of the list: a quest, or one step of a quest's unfolded route ({@code step}). {@code button} is the quest's
     * Accept/Claim, null for steps and for an NPC's quest, which has no Accept/Claim here.
     */
    private record Row(Definitions.Entry<QuestDefinition> quest, boolean step, int y, @Nullable Button button) {
    }

    /**
     * Whether the list also shows the route: quests already done and those still locked behind another. Off, it only
     * lists what can be accepted or is in progress. Kept while the game runs.
     */
    private static boolean showRoute;
    /** Quests whose own route is unfolded under them (their ▸/▾ button). Kept while the game runs. */
    private static final Set<ResourceLocation> UNFOLDED = new HashSet<>();

    private final List<Row> rows = new ArrayList<>();
    private boolean routeToggled;
    private PlayerQuests shownState;
    private final ScrollList scroll = new ScrollList(VISIBLE_ROWS);
    private int lineCount;
    /**
     * Discoveries that were unseen when this tab was first shown. They keep their "New!" mark while the
     * window stays open, even though the server already counts them as seen.
     */
    private Set<ResourceLocation> newWhenOpened;

    public QuestsTab() {
        super(Component.translatable("tab.overgearedmod.quests"));
    }

    /** "Quests •" while there are discoveries the player hasn't looked at yet. */
    @Override
    public Component title() {
        Component title = super.title();
        return QuestService.get(player()).hasNew() ? title.copy().append(Component.literal(" •").withStyle(ChatFormatting.GOLD)) : title;
    }

    @Override
    protected void addWidgets(Consumer<AbstractWidget> addWidget) {
        rows.clear();
        shownState = QuestService.get(player());
        if (newWhenOpened == null) {
            newWhenOpened = shownState.discovered().stream().filter(shownState::isNew).collect(Collectors.toSet());
            if (!newWhenOpened.isEmpty()) {
                PacketDistributor.sendToServer(QuestsSeenPayload.INSTANCE);
            }
        }
        addWidget.accept(Button.builder(Component.translatable(showRoute ? "screen.overgearedmod.quest_route_hide"
                                : "screen.overgearedmod.quest_route_show"), b -> {
                            showRoute = !showRoute;
                            routeToggled = true;
                        })
                .bounds(left + width - 96, top - 3, 96, 14)
                .tooltip(Tooltip.create(Component.translatable("screen.overgearedmod.quest_route_tooltip")))
                .build(SatisfyButton::new));
        List<Definitions.Entry<QuestDefinition>> all = Definitions.all(player().level(), ModDatapackRegistries.QUEST);
        Map<ResourceLocation, Definitions.Entry<QuestDefinition>> byId = new HashMap<>();
        Map<ResourceLocation, ResourceLocation> requires = new HashMap<>();
        for (Definitions.Entry<QuestDefinition> entry : all) {
            byId.put(entry.id(), entry);
            entry.value().requiresQuest().ifPresent(previous -> requires.put(entry.id(), previous));
        }
        // Hidden quests stay out of the list (not even locked) until the player discovers them.
        List<Definitions.Entry<QuestDefinition>> quests = all.stream()
                .filter(entry -> entry.value().isVisibleTo(entry.id(), shownState))
                .filter(entry -> showRoute || shownState.isActive(entry.id()) || QuestService.canAccept(player(), entry.id(), entry.value()))
                .toList();
        // Each quest, followed by the other steps of its route when unfolded.
        List<Row> lines = new ArrayList<>();
        Set<ResourceLocation> inChains = new HashSet<>();
        for (Definitions.Entry<QuestDefinition> entry : quests) {
            lines.add(new Row(entry, false, 0, null));
            List<ResourceLocation> route = QuestRoute.of(entry.id(), requires);
            if (route.size() > 1) {
                inChains.add(entry.id());
            }
            if (UNFOLDED.contains(entry.id())) {
                route.stream().filter(step -> !step.equals(entry.id()) && byId.containsKey(step))
                        .forEach(step -> lines.add(new Row(byId.get(step), true, 0, null)));
            }
        }
        lineCount = lines.size();
        scroll.clamp(lineCount);
        int y = top + LIST_TOP;
        int bottom = top + LIST_TOP + VISIBLE_ROWS * ROW;
        for (int i = scroll.first(); i < lineCount; i++) {
            Row line = lines.get(i);
            int lineHeight = line.step() ? STEP_ROW : ROW;
            if (y + lineHeight > bottom) {
                break;
            }
            Button button = null;
            if (!line.step()) {
                Definitions.Entry<QuestDefinition> entry = line.quest();
                if (inChains.contains(entry.id())) {
                    addWidget.accept(routeButton(entry, y - 2));
                }
                // An NPC's quest is accepted and claimed at the NPC: here it only shows where to go.
                button = entry.value().giver().isPresent() ? null : actionButton(entry, y - 4);
                if (button != null) {
                    addWidget.accept(button);
                }
                if (shownState.isActive(entry.id())) {
                    addWidget.accept(abandonButton(entry, y - 4));
                }
            }
            rows.add(new Row(line.quest(), line.step(), y, button));
            y += lineHeight;
        }
    }

    /** ▸ unfolds this quest's route under it (done ✔, in progress », to come ○); ▾ folds it again. */
    private Button routeButton(Definitions.Entry<QuestDefinition> entry, int y) {
        boolean unfolded = UNFOLDED.contains(entry.id());
        return Button.builder(Component.literal(unfolded ? "▾" : "▸"), b -> {
                    if (!UNFOLDED.remove(entry.id())) {
                        UNFOLDED.add(entry.id());
                    }
                    routeToggled = true;
                })
                .bounds(left, y, 12, 12)
                .tooltip(Tooltip.create(Component.translatable(unfolded ? "screen.overgearedmod.quest_route_fold"
                        : "screen.overgearedmod.quest_route_unfold")))
                .build(SatisfyButton::new);
    }

    private Button abandonButton(Definitions.Entry<QuestDefinition> entry, int y) {
        return Button.builder(Component.literal("✖"),
                        b -> PacketDistributor.sendToServer(new QuestActionPayload(entry.id(), QuestActionPayload.Action.ABANDON)))
                .bounds(left + width - BUTTON_WIDTH - 18, y, 16, 16)
                .tooltip(Tooltip.create(Component.translatable("screen.overgearedmod.quest_abandon")))
                .build(SatisfyButton::new);
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
        Button button = Button.builder(label, b -> press(entry, action))
                .bounds(left + width - BUTTON_WIDTH, y, BUTTON_WIDTH, 16)
                .build(SatisfyButton::new);
        button.active = enabled;
        return button;
    }

    /** Claiming a quest that gives a class replaces the current one, so it asks first. */
    private static void press(Definitions.Entry<QuestDefinition> entry, QuestActionPayload.Action action) {
        Runnable send = () -> PacketDistributor.sendToServer(new QuestActionPayload(entry.id(), action));
        Optional<Component> newClass = entry.value().classReward().flatMap(QuestsTab::className);
        if (action != QuestActionPayload.Action.CLAIM || newClass.isEmpty()) {
            send.run();
            return;
        }
        Component message = ProgressionService.playerClass(player()).map(PlayerClassDefinition::name)
                .map(current -> Component.translatable("screen.overgearedmod.confirm_class_change", current, newClass.get()))
                .orElseGet(() -> Component.translatable("screen.overgearedmod.confirm_class_first", newClass.get()));
        Minecraft minecraft = Minecraft.getInstance();
        minecraft.setScreen(new SatisfyConfirmScreen(minecraft.screen,
                Component.translatable("screen.overgearedmod.confirm_class_title"), message, send));
    }

    private boolean isDone(Definitions.Entry<QuestDefinition> entry, PlayerQuests state) {
        Integer baseline = state.active().get(entry.id());
        return baseline != null && QuestService.progress(player(), entry.value(), baseline) >= entry.value().goal().target();
    }

    @Override
    public boolean mouseScrolled(double wheel) {
        return scroll.scroll(wheel, lineCount);
    }

    /** Buttons depend on quest state and progress, so rebuild whenever either changes. */
    @Override
    public boolean needsRebuild() {
        if (routeToggled) {
            routeToggled = false;
            return true;
        }
        PlayerQuests state = QuestService.get(player());
        if (!state.equals(shownState)) {
            return true;
        }
        return rows.stream().anyMatch(row -> row.button() != null && state.isActive(row.quest().id())
                && row.button().active != isDone(row.quest(), state));
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY) {
        if (rows.isEmpty()) {
            UiStyle.text(graphics, font(), Component.translatable("screen.overgearedmod.no_quests"), left, top, UiStyle.MUTED);
            return;
        }
        UiStyle.heading(graphics, font(), Component.translatable("tab.overgearedmod.quests"), left, top);
        PlayerQuests state = QuestService.get(player());
        for (Row row : rows) {
            Definitions.Entry<QuestDefinition> entry = row.quest();
            QuestDefinition quest = entry.value();
            int y = row.y();
            if (row.step()) {
                renderStep(graphics, entry, state, y);
                continue;
            }
            UiStyle.text(graphics, font(), quest.name(), left + NAME_X, y, UiStyle.TEXT);
            if (newWhenOpened.contains(entry.id())) {
                UiStyle.text(graphics, font(), Component.translatable("screen.overgearedmod.quest_new"),
                        left + NAME_X + font().width(quest.name()) + 4, y, UiStyle.GOLD);
            }

            Integer stored = state.active().get(entry.id());
            if (stored != null) {
                int progress = QuestService.progress(player(), quest, stored);
                UiStyle.text(graphics, font(), Component.literal(progress + "/" + quest.goal().target()), left + 124, y,
                        progress >= quest.goal().target() ? UiStyle.GOOD : UiStyle.MUTED);
            }
            quest.giverHint().ifPresent(hint -> graphics.drawString(font(), Component.literal("↳ ").append(hint), left + NAME_X + 6,
                    y + 10, UiStyle.GOLD));
            if (mouseX >= left + NAME_X && mouseX < left + width - BUTTON_WIDTH && mouseY >= y - 2 && mouseY < y + ROW - 4) {
                graphics.renderTooltip(font(), tooltip(quest), mouseX, mouseY);
            }
        }
        scroll.renderBar(graphics, left + width + 3, top + LIST_TOP - 6, VISIBLE_ROWS * ROW - 4, lineCount);
    }

    /** One step of an unfolded route: ✔ done, » in progress, ○ still to come; an undiscovered hidden quest stays "???". */
    private void renderStep(GuiGraphics graphics, Definitions.Entry<QuestDefinition> step, PlayerQuests state, int y) {
        boolean done = state.isCompleted(step.id());
        boolean active = state.isActive(step.id());
        String mark = done ? "✔ " : active ? "» " : "○ ";
        Component name = step.value().isVisibleTo(step.id(), state) ? step.value().name() : Component.literal("???");
        int color = done ? UiStyle.GOOD : active ? UiStyle.GOLD : UiStyle.MUTED;
        graphics.drawString(font(), Component.literal(mark).append(name), left + NAME_X + 8, y, color);
    }

    private static List<FormattedCharSequence> tooltip(QuestDefinition quest) {
        List<FormattedCharSequence> lines = new ArrayList<>(font().split(quest.description(), 180));
        lines.add(quest.goal().describe().getVisualOrderText());
        quest.giverHint().ifPresent(hint -> lines.add(hint.getVisualOrderText()));
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
