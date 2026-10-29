package com.orijua.overgearedmod.client.tab;

import com.orijua.overgearedmod.client.OvergearedmodClient;
import com.orijua.overgearedmod.client.ui.SatisfyTab;
import com.orijua.overgearedmod.client.ui.ScrollList;
import com.orijua.overgearedmod.client.ui.UiStyle;
import com.orijua.overgearedmod.network.AssignSkillPayload;
import com.orijua.overgearedmod.registry.Definitions;
import com.orijua.overgearedmod.registry.ModAttachments;
import com.orijua.overgearedmod.registry.ModDatapackRegistries;
import com.orijua.overgearedmod.skill.PlayerSkills;
import com.orijua.overgearedmod.skill.SkillDefinition;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

/** Learned skills, each with one button per slot key to equip it there (press again to unequip). */
public class SkillsTab extends SatisfyTab {
    private static final int ROW = 22;
    private static final int LIST_TOP = 16;
    private static final int VISIBLE_ROWS = 7;
    private static final int SLOT_BUTTON = 18;

    private record Row(ResourceLocation id, SkillDefinition skill, List<Button> slotButtons) {
    }

    private final List<Row> rows = new ArrayList<>();
    private final ScrollList scroll = new ScrollList(VISIBLE_ROWS);
    private int shownLearnedCount;

    public SkillsTab() {
        super(Component.translatable("tab.overgearedmod.skills"));
    }

    private static PlayerSkills skills() {
        return player().getData(ModAttachments.PLAYER_SKILLS);
    }

    @Override
    protected void addWidgets(Consumer<AbstractWidget> addWidget) {
        rows.clear();
        List<ResourceLocation> learned = skills().learned();
        shownLearnedCount = learned.size();
        scroll.clamp(learned.size());
        for (int i = scroll.first(); i < scroll.end(learned.size()); i++) {
            ResourceLocation id = learned.get(i);
            Optional<SkillDefinition> skill = Definitions.get(player().level(), ModDatapackRegistries.SKILL, id);
            if (skill.isEmpty()) {
                continue;
            }
            List<Button> buttons = new ArrayList<>();
            for (int slot = 0; slot < PlayerSkills.SLOT_COUNT; slot++) {
                int targetSlot = slot;
                Button button = Button.builder(OvergearedmodClient.SKILL_KEYS.get(slot).getTranslatedKeyMessage(), b -> toggle(targetSlot, id))
                        .bounds(left + width - (PlayerSkills.SLOT_COUNT - slot) * (SLOT_BUTTON + 2), rowY(rows.size()) - 2, SLOT_BUTTON, SLOT_BUTTON)
                        .build();
                buttons.add(button);
                addWidget.accept(button);
            }
            rows.add(new Row(id, skill.get(), buttons));
        }
        tick();
    }

    private int rowY(int index) {
        return top + LIST_TOP + index * ROW;
    }

    private static void toggle(int slot, ResourceLocation skill) {
        boolean alreadyThere = skills().slot(slot).filter(skill::equals).isPresent();
        PacketDistributor.sendToServer(new AssignSkillPayload(slot, alreadyThere ? Optional.empty() : Optional.of(skill)));
    }

    @Override
    public boolean needsRebuild() {
        return skills().learned().size() != shownLearnedCount;
    }

    @Override
    public boolean mouseScrolled(double wheel) {
        return scroll.scroll(wheel, skills().learned().size());
    }

    /** Marks the slot(s) where each skill is equipped with a check. */
    @Override
    public void tick() {
        PlayerSkills skills = skills();
        for (Row row : rows) {
            for (int slot = 0; slot < row.slotButtons().size(); slot++) {
                boolean equippedHere = skills.slot(slot).filter(row.id()::equals).isPresent();
                row.slotButtons().get(slot).setMessage(equippedHere
                        ? Component.literal("✔")
                        : OvergearedmodClient.SKILL_KEYS.get(slot).getTranslatedKeyMessage());
            }
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY) {
        if (rows.isEmpty()) {
            UiStyle.text(graphics, font(), Component.translatable("screen.overgearedmod.no_skills"), left, top, UiStyle.MUTED);
            return;
        }
        UiStyle.heading(graphics, font(), Component.translatable("screen.overgearedmod.skills_header"), left, top);
        Row hovered = null;
        for (int i = 0; i < rows.size(); i++) {
            Row row = rows.get(i);
            int y = rowY(i);
            UiStyle.text(graphics, font(), row.skill().name(), left, y, UiStyle.TEXT);
            UiStyle.text(graphics, font(), Component.translatable("screen.overgearedmod.skill_cost",
                    (int) row.skill().manaCost(), row.skill().cooldownTicks() / 20), left + 120, y, UiStyle.MUTED);
            if (mouseX >= left && mouseX < left + 115 && mouseY >= y - 2 && mouseY < y + ROW - 4) {
                hovered = row;
            }
        }
        scroll.renderBar(graphics, left + width + 3, top + LIST_TOP - 2, VISIBLE_ROWS * ROW - 4, shownLearnedCount);
        if (hovered != null) {
            graphics.renderTooltip(font(), font().split(hovered.skill().description(), 180), mouseX, mouseY);
        }
    }
}
