package com.orijua.overgearedmod.client;

import com.orijua.overgearedmod.client.ui.SatisfyButton;
import com.orijua.overgearedmod.client.ui.UiStyle;
import com.orijua.overgearedmod.goal.DeliverGoal;
import com.orijua.overgearedmod.network.NpcQuestPayload;
import com.orijua.overgearedmod.network.NpcTradePayload;
import com.orijua.overgearedmod.quest.PlayerQuests;
import com.orijua.overgearedmod.quest.QuestDefinition;
import com.orijua.overgearedmod.quest.QuestService;
import com.orijua.overgearedmod.registry.Definitions;
import com.orijua.overgearedmod.registry.ModDatapackRegistries;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;

/**
 * An NPC's quest window (right-click on Smith or the village chief): the quests it gives that the player can accept or
 * has active, each with Accept, Deliver or Claim. A merchant also gets a Trade button. Quests with a giver can only be
 * dealt with here; the server checks it again (QuestService.atGiver).
 */
public final class NpcQuestScreen extends Screen {
    private static final int WIDTH = 270;
    private static final int PADDING = 10;
    private static final int ROW = 28;
    private static final int MAX_ROWS = 5;
    private static final int BUTTON_WIDTH = 64;

    private record Row(Definitions.Entry<QuestDefinition> quest, Button button, NpcQuestPayload.Action action) {
    }

    private final Entity npc;
    private final boolean merchant;
    private final List<Row> rows = new ArrayList<>();
    private PlayerQuests shownState;
    private int left;
    private int top;
    private int panelHeight;

    private NpcQuestScreen(Entity npc, boolean merchant) {
        super(npc.getDisplayName());
        this.npc = npc;
        this.merchant = merchant;
    }

    /** Client only: called from the NPC's interaction on the client side. */
    public static void open(Entity npc, boolean merchant) {
        Minecraft.getInstance().setScreen(new NpcQuestScreen(npc, merchant));
    }

    private Player player() {
        return minecraft.player;
    }

    private int listTop() {
        return top + PADDING + UiStyle.LINE + 6;
    }

    @Override
    protected void init() {
        rows.clear();
        shownState = QuestService.get(player());
        List<Definitions.Entry<QuestDefinition>> quests = Definitions.all(player().level(), ModDatapackRegistries.QUEST).stream()
                .filter(entry -> entry.value().isGivenBy(npc.getType()))
                .filter(entry -> shownState.isActive(entry.id()) || QuestService.canAccept(player(), entry.id(), entry.value()))
                .limit(MAX_ROWS)
                .toList();
        panelHeight = PADDING + UiStyle.LINE + 6 + Math.max(1, quests.size()) * ROW + (merchant ? 24 : 0) + PADDING;
        left = (width - WIDTH) / 2;
        top = (height - panelHeight) / 2;
        for (int i = 0; i < quests.size(); i++) {
            Definitions.Entry<QuestDefinition> entry = quests.get(i);
            NpcQuestPayload.Action action = actionFor(entry);
            Button button = Button.builder(label(action), b -> send(entry, action))
                    .bounds(left + WIDTH - PADDING - BUTTON_WIDTH, listTop() + i * ROW + 2, BUTTON_WIDTH, 16)
                    .build(SatisfyButton::new);
            button.active = enabled(entry, action);
            addRenderableWidget(button);
            rows.add(new Row(entry, button, action));
        }
        if (merchant) {
            addRenderableWidget(Button.builder(Component.translatable("screen.overgearedmod.npc_trade"),
                            b -> PacketDistributor.sendToServer(new NpcTradePayload(npc.getId())))
                    .bounds(left + (WIDTH - 90) / 2, top + panelHeight - PADDING - 18, 90, 18)
                    .build(SatisfyButton::new));
        }
    }

    /** Accept a new quest; for an active one, Deliver while items are still missing, otherwise Claim. */
    private NpcQuestPayload.Action actionFor(Definitions.Entry<QuestDefinition> entry) {
        if (!shownState.isActive(entry.id())) {
            return NpcQuestPayload.Action.ACCEPT;
        }
        boolean delivering = entry.value().goal() instanceof DeliverGoal;
        return delivering && !isDone(entry) ? NpcQuestPayload.Action.DELIVER : NpcQuestPayload.Action.CLAIM;
    }

    private boolean enabled(Definitions.Entry<QuestDefinition> entry, NpcQuestPayload.Action action) {
        return switch (action) {
            case ACCEPT -> true;
            case DELIVER -> entry.value().goal() instanceof DeliverGoal goal && carries(goal);
            case CLAIM -> isDone(entry);
        };
    }

    private boolean carries(DeliverGoal goal) {
        Inventory inventory = player().getInventory();
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            if (goal.accepts(inventory.getItem(slot))) {
                return true;
            }
        }
        return false;
    }

    private boolean isDone(Definitions.Entry<QuestDefinition> entry) {
        Integer stored = shownState.active().get(entry.id());
        return stored != null && QuestService.progress(player(), entry.value(), stored) >= entry.value().goal().target();
    }

    private static Component label(NpcQuestPayload.Action action) {
        return Component.translatable(switch (action) {
            case ACCEPT -> "screen.overgearedmod.quest_accept";
            case DELIVER -> "screen.overgearedmod.quest_deliver";
            case CLAIM -> "screen.overgearedmod.quest_claim";
        });
    }

    private void send(Definitions.Entry<QuestDefinition> entry, NpcQuestPayload.Action action) {
        PacketDistributor.sendToServer(new NpcQuestPayload(npc.getId(), entry.id(), action));
    }

    /** Closes when the NPC is gone or left behind; rebuilds when quests, progress or the inventory change the buttons. */
    @Override
    public void tick() {
        if (!npc.isAlive() || npc.distanceToSqr(player()) > QuestService.GIVER_REACH * QuestService.GIVER_REACH) {
            onClose();
            return;
        }
        if (!QuestService.get(player()).equals(shownState)
                || rows.stream().anyMatch(row -> row.button().active != enabled(row.quest(), row.action()))) {
            rebuildWidgets();
        }
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.renderBackground(graphics, mouseX, mouseY, partialTick);
        UiStyle.panel(graphics, left, top, WIDTH, panelHeight);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        graphics.drawCenteredString(font, title, left + WIDTH / 2, top + PADDING, UiStyle.HEADING);
        graphics.fill(left + PADDING, top + PADDING + UiStyle.LINE, left + WIDTH - PADDING, top + PADDING + UiStyle.LINE + 1, UiStyle.LINE_SOFT);
        if (rows.isEmpty()) {
            UiStyle.text(graphics, font, Component.translatable("screen.overgearedmod.npc_no_quests"), left + PADDING, listTop() + 4,
                    UiStyle.MUTED);
            return;
        }
        int textWidth = WIDTH - 2 * PADDING - BUTTON_WIDTH - 6;
        for (int i = 0; i < rows.size(); i++) {
            Definitions.Entry<QuestDefinition> entry = rows.get(i).quest();
            QuestDefinition quest = entry.value();
            int y = listTop() + i * ROW;
            UiStyle.text(graphics, font, quest.name(), left + PADDING, y, UiStyle.TEXT);
            Integer stored = shownState.active().get(entry.id());
            Component detail = quest.goal().describe();
            int color = UiStyle.MUTED;
            if (stored != null) {
                int progress = QuestService.progress(player(), quest, stored);
                detail = detail.copy().append("  " + progress + "/" + quest.goal().target());
                color = progress >= quest.goal().target() ? UiStyle.GOOD : UiStyle.MUTED;
            }
            graphics.drawString(font, font.substrByWidth(detail, textWidth).getString(), left + PADDING, y + 11, color);
            if (mouseX >= left + PADDING && mouseX < left + PADDING + textWidth && mouseY >= y - 2 && mouseY < y + ROW - 4) {
                graphics.renderTooltip(font, tooltip(quest), mouseX, mouseY);
            }
        }
    }

    private List<FormattedCharSequence> tooltip(QuestDefinition quest) {
        List<FormattedCharSequence> lines = new ArrayList<>(font.split(quest.description(), 200));
        lines.add(quest.goal().describe().getVisualOrderText());
        lines.add(Component.translatable("screen.overgearedmod.quest_reward", quest.xpReward(), quest.itemRewards().size()).getVisualOrderText());
        return lines;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
