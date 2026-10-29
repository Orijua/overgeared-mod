package com.orijua.overgearedmod.client.tab;

import com.orijua.overgearedmod.client.ui.SatisfyScreen;
import com.orijua.overgearedmod.client.ui.SatisfyTab;
import net.minecraft.client.Minecraft;

import java.util.List;
import java.util.function.Supplier;

/** The tabs of the Satisfy window, in display order. Add a page by writing a SatisfyTab and listing it here. */
public final class SatisfyTabs {
    private static final List<Supplier<SatisfyTab>> TABS = List.of(
            StatusTab::new,
            SkillsTab::new,
            ProfileTab::new,
            QuestsTab::new);

    private SatisfyTabs() {
    }

    public static SatisfyScreen createScreen() {
        return new SatisfyScreen(TABS);
    }

    public static void openQuests() {
        Minecraft.getInstance().setScreen(createScreen().showing(QuestsTab.class));
    }
}
