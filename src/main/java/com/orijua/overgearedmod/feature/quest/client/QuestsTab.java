package com.orijua.overgearedmod.feature.quest.client;

import com.orijua.overgearedmod.core.client.SatisfyButton;
import com.orijua.overgearedmod.core.client.SatisfyConfirmScreen;
import com.orijua.overgearedmod.core.client.SatisfyTab;
import com.orijua.overgearedmod.core.client.ScrollList;
import com.orijua.overgearedmod.core.client.UiStyle;
import com.orijua.overgearedmod.core.registry.Definitions;
import com.orijua.overgearedmod.core.registry.ModAttachments;
import com.orijua.overgearedmod.core.registry.ModDatapackRegistries;
import com.orijua.overgearedmod.feature.progression.PlayerClassDefinition;
import com.orijua.overgearedmod.feature.progression.ProgressionService;
import com.orijua.overgearedmod.feature.quest.PlayerQuests;
import com.orijua.overgearedmod.feature.quest.QuestActionPayload;
import com.orijua.overgearedmod.feature.quest.QuestDefinition;
import com.orijua.overgearedmod.feature.quest.QuestService;
import com.orijua.overgearedmod.feature.quest.QuestTree;
import com.orijua.overgearedmod.feature.quest.QuestsSeenPayload;
import com.orijua.overgearedmod.feature.rumor.RumorDefinition;
import com.orijua.overgearedmod.feature.rumor.Rumors;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import javax.annotation.Nullable;

/**
 * Every quest as a tree ({@link QuestTree}): on the left the chains grouped by who gives them, in three sections
 * (story, repeatable, completed); on the right the chosen quest with its goal, progress, rewards and buttons. A fourth
 * section is the Journal of rumors heard from NPCs.
 */
public class QuestsTab extends SatisfyTab {
    private static final int PANEL_WIDTH = 340;
    private static final int SECTION_WIDTH = 78;
    private static final int LIST_TOP = 17;
    private static final int LIST_WIDTH = 150;
    private static final int ROW = 11;
    /** How far each branch of a chain is indented. */
    private static final int INDENT = 8;
    private static final int DETAIL_GAP = 10;
    private static final int ITEM = 18;

    private static final int GUIDE = 0x709DC4EA;
    private static final int SELECTED = 0x50FFFFFF;
    private static final int HOVERED = 0x25FFFFFF;
    private static final int DIVIDER = 0x609DC4EA;

    private enum Section { STORY, REPEATABLE, COMPLETED, RUMORS }

    private enum Kind { GROUP, QUEST, LOCKED, CHAIN, RUMOR }

    /**
     * A line of the list. A group or a finished chain folds what is under it ({@code foldKey}); a quest or a rumor (its
     * id in {@code quest}) can be chosen.
     * {@code depth} is the indent inside its group; {@code mark} goes before the text ("✔", "»", "○"); {@code right} is
     * drawn at the end of the line (an accepted quest shows its live progress there instead).
     */
    private record Line(Kind kind, Component text, int color, @Nullable ResourceLocation quest, int depth, String mark,
                        Component right, int rightColor, String foldKey) {
    }

    // What the player was looking at, kept while the game runs.
    private static Section section = Section.STORY;
    @Nullable
    private static ResourceLocation selected;
    @Nullable
    private static ResourceLocation selectedRumor;
    /** Folded groups, and finished chains that are unfolded (they start folded). */
    private static final Set<String> TOGGLED = new HashSet<>();

    private final List<Line> lines = new ArrayList<>();
    /** Made once the tab knows its height; a new one (back to the top) for each section. */
    @Nullable
    private ScrollList scroll;
    private boolean dirty;
    private PlayerQuests shownState;
    /** The rumors heard and which of them were solved when the list was built. */
    private List<ResourceLocation> shownRumors = List.of();
    private Set<ResourceLocation> shownSolved = Set.of();
    @Nullable
    private Button claimButton;
    private int repeatableCount;
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
    public int panelWidth() {
        return PANEL_WIDTH;
    }

    @Override
    protected void addWidgets(Consumer<AbstractWidget> addWidget) {
        shownState = QuestService.get(player());
        if (newWhenOpened == null) {
            newWhenOpened = shownState.discovered().stream().filter(shownState::isNew).collect(Collectors.toSet());
            if (!newWhenOpened.isEmpty()) {
                PacketDistributor.sendToServer(QuestsSeenPayload.INSTANCE);
            }
        }
        buildLines();
        if (scroll == null) {
            scroll = new ScrollList(visibleRows());
        }
        scroll.clamp(lines.size());

        for (Section each : Section.values()) {
            Component label = Component.translatable("screen.overgearedmod.quest_section_" + each.name().toLowerCase());
            if (each == Section.REPEATABLE) {
                label = label.copy().append(" (" + repeatableCount + ")");
            }
            Button button = Button.builder(label, b -> {
                        section = each;
                        scroll = new ScrollList(visibleRows());
                        dirty = true;
                    })
                    .bounds(left + each.ordinal() * (SECTION_WIDTH + 2), top - 3, SECTION_WIDTH, 14)
                    .build(SatisfyButton::new);
            button.active = each != section;
            addWidget.accept(button);
        }
        addDetailButtons(addWidget);
    }

    /** The list of the open section, and the chosen quest kept (or a sensible one picked) among its lines. */
    private void buildLines() {
        lines.clear();
        List<Definitions.Entry<QuestDefinition>> all = Definitions.all(player().level(), ModDatapackRegistries.QUEST);
        Map<ResourceLocation, QuestDefinition> byId = new HashMap<>();
        List<QuestTree.Quest> quests = new ArrayList<>();
        for (Definitions.Entry<QuestDefinition> entry : all) {
            byId.put(entry.id(), entry.value());
            QuestDefinition quest = entry.value();
            quests.add(new QuestTree.Quest(entry.id(), quest.requiresQuest(), groupKey(quest), quest.repeatable(), quest.isHidden()));
        }
        QuestTree.View view = QuestTree.build(quests, shownState);
        repeatableCount = view.repeatable().size();

        switch (section) {
            case STORY -> view.story().forEach(group -> addGroup(group, byId, false));
            case COMPLETED -> view.completed().forEach(group -> addGroup(group, byId, true));
            case REPEATABLE -> view.repeatable().forEach(id -> {
                int times = shownState.timesCompleted(id);
                if (isNew(id)) {
                    addQuest(id, byId.get(id), 0, newMark(), UiStyle.GOLD);
                } else {
                    addQuest(id, byId.get(id), 0, times > 0 ? Component.literal("×" + times) : Component.empty(), UiStyle.MUTED);
                }
            });
            case RUMORS -> {
                addRumors();
                return;
            }
        }

        List<ResourceLocation> shown = lines.stream().filter(line -> line.kind() == Kind.QUEST).map(Line::quest).toList();
        if (selected == null || !shown.contains(selected)) {
            selected = shown.stream().filter(shownState::isActive).findFirst().orElse(shown.isEmpty() ? null : shown.getFirst());
        }
    }

    /** The rumors heard, under who told them (the same groups as their quests): ? still a secret, ✔ found. */
    private void addRumors() {
        shownRumors = Rumors.heard(player());
        Map<String, List<ResourceLocation>> byGroup = new LinkedHashMap<>();
        Set<ResourceLocation> solved = new HashSet<>();
        for (ResourceLocation id : shownRumors) {
            Definitions.get(player().level(), ModDatapackRegistries.RUMOR, id).ifPresent(rumor -> {
                byGroup.computeIfAbsent(groupKey(rumor), key -> new ArrayList<>()).add(id);
                if (Rumors.solved(player(), rumor)) {
                    solved.add(id);
                }
            });
        }
        shownSolved = solved;
        byGroup.forEach((group, ids) -> {
            lines.add(new Line(Kind.GROUP, Component.translatable(group), UiStyle.HEADING, null, 0, "", Component.empty(), 0, ""));
            for (ResourceLocation id : ids) {
                RumorDefinition rumor = Definitions.get(player().level(), ModDatapackRegistries.RUMOR, id).orElseThrow();
                boolean found = solved.contains(id);
                lines.add(new Line(Kind.RUMOR, rumor.text(), found ? UiStyle.MUTED : UiStyle.TEXT, id, 0, found ? "✔" : "?",
                        Component.empty(), 0, ""));
            }
        });
        List<ResourceLocation> shown = lines.stream().filter(line -> line.kind() == Kind.RUMOR).map(Line::quest).toList();
        if (selectedRumor == null || !shown.contains(selectedRumor)) {
            selectedRumor = shown.isEmpty() ? null : shown.getLast();
        }
    }

    private static String groupKey(RumorDefinition rumor) {
        ResourceLocation npc = BuiltInRegistries.ENTITY_TYPE.getKey(rumor.givers().getFirst());
        return "quest." + npc.getNamespace() + ".group." + npc.getPath();
    }

    private Optional<RumorDefinition> selectedRumor() {
        return selectedRumor == null ? Optional.empty() : Definitions.get(player().level(), ModDatapackRegistries.RUMOR, selectedRumor);
    }

    /** The chosen rumor: who tells it, all of it, and whether its secret was found. */
    private void renderRumorDetail(GuiGraphics graphics, ResourceLocation id, RumorDefinition rumor) {
        int x = detailX();
        int detailWidth = left + width - x;
        int y = top + LIST_TOP;
        graphics.drawString(font(), Component.translatable("screen.overgearedmod.npc_rumor"), x, y, UiStyle.HEADING);
        y += 10;
        Component teller = Component.translatable("screen.overgearedmod.rumor_from", Component.translatable(groupKey(rumor)));
        graphics.drawString(font(), teller, x, y, UiStyle.GOLD);
        y += 13;
        for (FormattedCharSequence line : font().split(rumor.text().copy().withStyle(ChatFormatting.ITALIC), detailWidth)) {
            graphics.drawString(font(), line, x, y, UiStyle.TEXT);
            y += 10;
        }
        y += 4;
        boolean found = shownSolved.contains(id);
        graphics.drawString(font(), Component.translatable(found ? "screen.overgearedmod.rumor_solved" : "screen.overgearedmod.rumor_open"),
                x, y, found ? UiStyle.GOOD : UiStyle.MUTED);
    }

    private void addGroup(QuestTree.Group group, Map<ResourceLocation, QuestDefinition> byId, boolean finished) {
        String groupFold = (finished ? "done/" : "story/") + group.key();
        boolean groupFolded = TOGGLED.contains(groupFold);
        lines.add(new Line(Kind.GROUP, Component.translatable(group.key()), UiStyle.HEADING, null, 0, groupFolded ? "▸" : "▾",
                Component.empty(), 0, groupFold));
        if (groupFolded) {
            return;
        }
        for (QuestTree.Chain chain : group.chains()) {
            if (finished && chain.nodes().size() > 1) {
                // A finished chain is one line, "✔ Smith · Bairan 6/6", unfolded on demand.
                String chainFold = "chain/" + chain.root();
                boolean chainFolded = !TOGGLED.contains(chainFold);
                Component label = group.chains().size() == 1 ? Component.translatable(group.key()) : byId.get(chain.root()).name();
                lines.add(new Line(Kind.CHAIN, label, UiStyle.GOOD, null, 0, chainFolded ? "▸" : "▾",
                        Component.literal(chain.done() + "/" + chain.nodes().size()), UiStyle.GOOD, chainFold));
                if (chainFolded) {
                    continue;
                }
                chain.nodes().forEach(node -> addQuest(node.id(), byId.get(node.id()), node.depth() + 1, Component.empty(), 0));
                continue;
            }
            for (QuestTree.Node node : chain.nodes()) {
                if (node.state() == QuestTree.State.LOCKED) {
                    lines.add(new Line(Kind.LOCKED, Component.literal("???"), UiStyle.MUTED, null, node.depth(), "·",
                            Component.empty(), 0, ""));
                } else {
                    addQuest(node.id(), byId.get(node.id()), node.depth(), isNew(node.id()) ? newMark() : Component.empty(), UiStyle.GOLD);
                }
            }
        }
    }

    /** A quest line: ✔ done, » accepted, ○ to accept (grey when its class can't). */
    private void addQuest(ResourceLocation id, QuestDefinition quest, int depth, Component right, int rightColor) {
        String mark;
        int color;
        if (shownState.isActive(id)) {
            mark = "»";
            color = UiStyle.GOLD;
        } else if (shownState.isCompleted(id) && !quest.repeatable()) {
            mark = "✔";
            color = UiStyle.GOOD;
        } else {
            mark = "○";
            color = classAllows(quest) ? UiStyle.TEXT : UiStyle.MUTED;
        }
        lines.add(new Line(Kind.QUEST, quest.name(), color, id, depth, mark, right, rightColor, ""));
    }

    /** Accept or Claim (only quests without a giver: the rest are dealt with at the NPC), follow on the HUD, abandon. */
    private void addDetailButtons(Consumer<AbstractWidget> addWidget) {
        claimButton = null;
        Optional<QuestDefinition> found = selectedQuest();
        if (found.isEmpty() || section == Section.RUMORS) {
            return;
        }
        ResourceLocation id = selected;
        QuestDefinition quest = found.get();
        int x = detailX();
        int y = top + height - 16;
        boolean active = shownState.isActive(id);
        if (quest.giver().isEmpty()) {
            if (active) {
                Button claim = Button.builder(Component.translatable("screen.overgearedmod.quest_claim"),
                                b -> press(id, quest, QuestActionPayload.Action.CLAIM))
                        .bounds(x, y, 60, 16).build(SatisfyButton::new);
                claim.active = isDone(id, quest);
                claimButton = claim;
                addWidget.accept(claim);
                x += 62;
            } else if (QuestService.canAccept(player(), id, quest)) {
                addWidget.accept(Button.builder(Component.translatable("screen.overgearedmod.quest_accept"),
                                b -> press(id, quest, QuestActionPayload.Action.ACCEPT))
                        .bounds(x, y, 60, 16).build(SatisfyButton::new));
                x += 62;
            }
        }
        if (active) {
            boolean tracked = shownState.isTracked(id);
            Button track = Button.builder(Component.translatable(tracked ? "screen.overgearedmod.quest_untrack"
                                    : "screen.overgearedmod.quest_track"),
                            b -> PacketDistributor.sendToServer(new QuestActionPayload(id, QuestActionPayload.Action.TRACK)))
                    .bounds(x, y, 70, 16)
                    .tooltip(Tooltip.create(Component.translatable("screen.overgearedmod.quest_track_tooltip", PlayerQuests.MAX_TRACKED)))
                    .build(SatisfyButton::new);
            track.active = tracked || shownState.tracked().size() < PlayerQuests.MAX_TRACKED;
            addWidget.accept(track);
            addWidget.accept(Button.builder(Component.literal("✖"),
                            b -> PacketDistributor.sendToServer(new QuestActionPayload(id, QuestActionPayload.Action.ABANDON)))
                    .bounds(left + width - 16, y, 16, 16)
                    .tooltip(Tooltip.create(Component.translatable("screen.overgearedmod.quest_abandon")))
                    .build(SatisfyButton::new));
        }
    }

    /** Claiming a quest that gives a class replaces the current one, so it asks first. */
    private static void press(ResourceLocation id, QuestDefinition quest, QuestActionPayload.Action action) {
        Runnable send = () -> PacketDistributor.sendToServer(new QuestActionPayload(id, action));
        Optional<Component> newClass = quest.classReward().flatMap(QuestsTab::className);
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

    @Override
    public boolean mouseClicked(double mouseX, double mouseY) {
        Line line = lineAt(mouseX, mouseY);
        if (line == null) {
            return false;
        }
        switch (line.kind()) {
            case GROUP, CHAIN -> {
                if (line.foldKey().isEmpty()) {
                    return false;
                }
                if (!TOGGLED.remove(line.foldKey())) {
                    TOGGLED.add(line.foldKey());
                }
            }
            case QUEST -> selected = line.quest();
            case RUMOR -> selectedRumor = line.quest();
            case LOCKED -> {
                return false;
            }
        }
        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0f));
        return true;
    }

    @Override
    public boolean mouseScrolled(double wheel) {
        return scroll.scroll(wheel, lines.size());
    }

    /** Rebuild when the quests changed, a section or fold was clicked, or the chosen quest's Claim became possible. */
    @Override
    public boolean needsRebuild() {
        if (dirty) {
            dirty = false;
            return true;
        }
        if (!QuestService.get(player()).equals(shownState)) {
            return true;
        }
        if (section == Section.RUMORS && (!Rumors.heard(player()).equals(shownRumors) || shownRumors.stream().anyMatch(id ->
                shownSolved.contains(id) != Definitions.get(player().level(), ModDatapackRegistries.RUMOR, id)
                        .map(rumor -> Rumors.solved(player(), rumor)).orElse(false)))) {
            return true;
        }
        return claimButton != null && selected != null && selectedQuest().isPresent()
                && claimButton.active != isDone(selected, selectedQuest().get());
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY) {
        int listTop = top + LIST_TOP;
        graphics.fill(left + LIST_WIDTH + DETAIL_GAP / 2, listTop - 2, left + LIST_WIDTH + DETAIL_GAP / 2 + 1, top + height, DIVIDER);
        if (lines.isEmpty()) {
            UiStyle.text(graphics, font(), Component.translatable("screen.overgearedmod.no_quests_" + section.name().toLowerCase()),
                    left, listTop, UiStyle.MUTED);
        }
        Line hovered = lineAt(mouseX, mouseY);
        int y = listTop;
        for (int i = scroll.first(); i < scroll.end(lines.size()); i++) {
            renderLine(graphics, lines.get(i), y, lines.get(i) == hovered, mouseX, mouseY);
            y += ROW;
        }
        scroll.renderBar(graphics, left + LIST_WIDTH + 1, listTop, visibleRows() * ROW, lines.size());
        if (section == Section.RUMORS) {
            selectedRumor().ifPresent(rumor -> renderRumorDetail(graphics, selectedRumor, rumor));
        } else {
            selectedQuest().ifPresent(quest -> renderDetail(graphics, selected, quest, mouseX, mouseY));
        }
    }

    private void renderLine(GuiGraphics graphics, Line line, int y, boolean hovered, int mouseX, int mouseY) {
        if (line.kind() == Kind.QUEST && line.quest().equals(selected) || line.kind() == Kind.RUMOR && line.quest().equals(selectedRumor)) {
            graphics.fill(left, y - 1, left + LIST_WIDTH, y + ROW - 1, SELECTED);
        } else if (hovered && line.kind() != Kind.LOCKED && line.kind() != Kind.GROUP || hovered && !line.foldKey().isEmpty()) {
            graphics.fill(left, y - 1, left + LIST_WIDTH, y + ROW - 1, HOVERED);
        }
        int x = left + 1;
        if (line.kind() == Kind.QUEST || line.kind() == Kind.LOCKED || line.kind() == Kind.RUMOR) {
            // The chain's guide line, then the step's mark.
            int guide = left + 4 + line.depth() * INDENT;
            graphics.fill(guide, y - 1, guide + 1, y + ROW - 1, GUIDE);
            graphics.fill(guide + 1, y + 3, guide + 4, y + 4, GUIDE);
            x = guide + 6;
        }
        int markColor = line.kind() == Kind.GROUP ? UiStyle.HEADING : line.color();
        graphics.drawString(font(), line.mark(), x, y, markColor);
        x += font().width(line.mark()) + 3;

        Component right = line.right();
        int rightColor = line.rightColor();
        Integer stored = line.kind() == Kind.QUEST ? shownState.active().get(line.quest()) : null;
        if (stored != null) {
            // Live: kills and forges move it without the quest state changing.
            QuestDefinition quest = Definitions.get(player().level(), ModDatapackRegistries.QUEST, line.quest()).orElseThrow();
            int progress = QuestService.progress(player(), quest, stored);
            right = Component.literal(progress + "/" + quest.goal().target());
            rightColor = progress >= quest.goal().target() ? UiStyle.GOOD : UiStyle.MUTED;
        }
        int rightWidth = font().width(right);
        int room = left + LIST_WIDTH - 3 - rightWidth - x - (rightWidth > 0 ? 4 : 0);
        String full = line.text().getString();
        String shown = font().plainSubstrByWidth(full, room);
        boolean cut = shown.length() < full.length();
        if (cut) {
            shown = font().plainSubstrByWidth(full, room - font().width("…")) + "…";
        }
        graphics.drawString(font(), shown, x, y, line.color());
        if (rightWidth > 0) {
            graphics.drawString(font(), right, left + LIST_WIDTH - 3 - rightWidth, y, rightColor);
        }
        if (cut && hovered) {
            graphics.renderTooltip(font(), line.text(), mouseX, mouseY);
        }
    }

    /** The chosen quest: name, where to go, story, goal and progress, requirements, rewards. Buttons sit at the bottom. */
    private void renderDetail(GuiGraphics graphics, ResourceLocation id, QuestDefinition quest, int mouseX, int mouseY) {
        int x = detailX();
        int detailWidth = left + width - x;
        int y = top + LIST_TOP;
        for (FormattedCharSequence line : font().split(quest.name(), detailWidth)) {
            graphics.drawString(font(), line, x, y, UiStyle.HEADING);
            y += 10;
        }
        if (quest.giver().isPresent()) {
            for (FormattedCharSequence line : font().split(quest.giverHint().orElseThrow(), detailWidth)) {
                graphics.drawString(font(), line, x, y, UiStyle.GOLD);
                y += 10;
            }
        }
        // Up to 4 lines of story, fewer when the name or the hint took two lines, so the buttons stay clear.
        int storyLines = Math.max(2, 4 - (y - top - LIST_TOP) / 10 + 2);
        y += 3;
        List<FormattedCharSequence> story = font().split(quest.description(), detailWidth);
        for (int i = 0; i < Math.min(story.size(), storyLines); i++) {
            graphics.drawString(font(), story.get(i), x, y, UiStyle.MUTED);
            y += 10;
        }
        y += 3;
        for (FormattedCharSequence line : font().split(quest.goal().describe(), detailWidth)) {
            graphics.drawString(font(), line, x, y, UiStyle.TEXT);
            y += 10;
        }

        Integer stored = shownState.active().get(id);
        if (stored != null) {
            int progress = QuestService.progress(player(), quest, stored);
            int target = quest.goal().target();
            boolean done = progress >= target;
            Component count = Component.literal(progress + "/" + target);
            int barWidth = detailWidth - font().width(count) - 6;
            UiStyle.bar(graphics, x + 1, y + 2, barWidth, 5, target == 0 ? 1 : (float) progress / target, done ? UiStyle.XP : UiStyle.COOLDOWN);
            graphics.drawString(font(), count, x + barWidth + 5, y, done ? UiStyle.GOOD : UiStyle.TEXT);
            y += 12;
        } else if (shownState.isCompleted(id)) {
            Component completed = quest.repeatable()
                    ? Component.translatable("screen.overgearedmod.quest_completed_times", shownState.timesCompleted(id))
                    : Component.translatable("screen.overgearedmod.quest_completed");
            graphics.drawString(font(), Component.literal("✔ ").append(completed), x, y, UiStyle.GOOD);
            y += 12;
        }
        Optional<Component> requiredClass = classAllows(quest) ? Optional.empty() : quest.requiredClass().flatMap(QuestsTab::className);
        if (requiredClass.isPresent()) {
            graphics.drawString(font(), Component.translatable("screen.overgearedmod.quest_requires_class", requiredClass.get()), x, y, UiStyle.BAD);
            y += 10;
        }

        y += 2;
        graphics.drawString(font(), Component.translatable("screen.overgearedmod.quest_rewards"), x, y, UiStyle.MUTED);
        y += 10;
        int rewardX = x;
        if (quest.xpReward() > 0) {
            Component xp = Component.literal("+" + quest.xpReward() + " XP");
            graphics.drawString(font(), xp, rewardX, y + 5, UiStyle.GOOD);
            rewardX += font().width(xp) + 4;
        }
        ItemStack hoveredItem = ItemStack.EMPTY;
        for (ItemStack reward : quest.itemRewards()) {
            graphics.renderItem(reward, rewardX, y);
            graphics.renderItemDecorations(font(), reward, rewardX, y);
            if (mouseX >= rewardX && mouseX < rewardX + 16 && mouseY >= y && mouseY < y + 16) {
                hoveredItem = reward;
            }
            rewardX += ITEM;
        }
        y += ITEM;
        Optional<Component> newClass = quest.classReward().flatMap(QuestsTab::className);
        if (newClass.isPresent()) {
            graphics.drawString(font(), Component.translatable("screen.overgearedmod.quest_class_reward", newClass.get()), x, y, UiStyle.GOLD);
        }
        if (!hoveredItem.isEmpty()) {
            graphics.renderTooltip(font(), hoveredItem, mouseX, mouseY);
        }
    }

    @Nullable
    private Line lineAt(double mouseX, double mouseY) {
        int listTop = top + LIST_TOP;
        if (mouseX < left || mouseX >= left + LIST_WIDTH || mouseY < listTop - 1) {
            return null;
        }
        int index = scroll.first() + (int) ((mouseY - listTop + 1) / ROW);
        return index < scroll.end(lines.size()) ? lines.get(index) : null;
    }

    private Optional<QuestDefinition> selectedQuest() {
        return selected == null ? Optional.empty() : Definitions.get(player().level(), ModDatapackRegistries.QUEST, selected);
    }

    private int visibleRows() {
        return (height - LIST_TOP) / ROW;
    }

    private int detailX() {
        return left + LIST_WIDTH + DETAIL_GAP;
    }

    private boolean isNew(ResourceLocation id) {
        return newWhenOpened.contains(id);
    }

    private static Component newMark() {
        return Component.translatable("screen.overgearedmod.quest_new");
    }

    private boolean isDone(ResourceLocation id, QuestDefinition quest) {
        Integer stored = QuestService.get(player()).active().get(id);
        return stored != null && QuestService.progress(player(), quest, stored) >= quest.goal().target();
    }

    private static boolean classAllows(QuestDefinition quest) {
        return quest.allowsClass(player().getData(ModAttachments.PLAYER_IDENTITY).playerClass());
    }

    /**
     * What a quest is grouped under: its giver ("quest.&lt;namespace&gt;.group.&lt;npc&gt;", e.g. "Smith · Bairan"), the
     * secret class quests, or general adventure. A translation key, drawn as the group's heading.
     */
    private static String groupKey(QuestDefinition quest) {
        if (quest.giver().isPresent()) {
            ResourceLocation npc = BuiltInRegistries.ENTITY_TYPE.getKey(quest.giver().get());
            return "quest." + npc.getNamespace() + ".group." + npc.getPath();
        }
        return quest.isHidden() || quest.requiredClass().isPresent()
                ? "screen.overgearedmod.quest_group_class" : "screen.overgearedmod.quest_group_general";
    }

    private static Optional<Component> className(ResourceLocation classId) {
        return Definitions.get(player().level(), ModDatapackRegistries.PLAYER_CLASS, classId).map(PlayerClassDefinition::name);
    }
}
