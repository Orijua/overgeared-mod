package com.orijua.overgearedmod.core.client;

import com.orijua.overgearedmod.feature.npc.client.WorkshopTab;
import com.orijua.overgearedmod.feature.progression.client.ProfileTab;
import com.orijua.overgearedmod.feature.progression.client.StatusTab;
import com.orijua.overgearedmod.feature.quest.client.QuestsTab;
import com.orijua.overgearedmod.feature.skill.client.SkillsTab;
import net.minecraft.client.Minecraft;

import java.util.List;
import java.util.function.Supplier;

/** The tabs of the Satisfy window, in display order. Add a page by writing a SatisfyTab and listing it here. */
public final class SatisfyTabs {
    private static final List<Supplier<SatisfyTab>> TABS = List.of(
            StatusTab::new,
            SkillsTab::new,
            ProfileTab::new,
            QuestsTab::new,
            WorkshopTab::new);

    private SatisfyTabs() {
    }

    public static SatisfyScreen createScreen() {
        return new SatisfyScreen(TABS);
    }

    public static void openQuests() {
        Minecraft.getInstance().setScreen(createScreen().showing(QuestsTab.class));
    }

    public static void openWorkshop() {
        Minecraft.getInstance().setScreen(createScreen().showing(WorkshopTab.class));
    }
}
