package com.orijua.overgearedmod.feature.progression.client;

import com.orijua.overgearedmod.core.client.SatisfyButton;
import com.orijua.overgearedmod.core.client.SatisfyTab;
import com.orijua.overgearedmod.core.client.ScrollList;
import com.orijua.overgearedmod.core.client.UiStyle;
import com.orijua.overgearedmod.core.registry.Definitions;
import com.orijua.overgearedmod.core.registry.ModAttachments;
import com.orijua.overgearedmod.core.registry.ModDatapackRegistries;
import com.orijua.overgearedmod.feature.progression.ChooseClassPayload;
import com.orijua.overgearedmod.feature.progression.PlayerClassDefinition;
import com.orijua.overgearedmod.feature.progression.PlayerIdentity;
import com.orijua.overgearedmod.feature.progression.ProgressionService;
import com.orijua.overgearedmod.feature.progression.StatType;
import com.orijua.overgearedmod.feature.progression.TitleDefinition;
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

/** Class (choose once) on top, earned titles below. As in the canon, a title stays secret until it is earned. */
public class ProfileTab extends SatisfyTab {
    /** Where the title rows start when the class description fits in two lines; a longer one pushes them down. */
    private static final int TITLES_TOP = 64;
    private static final int TITLE_ROW = 12;
    /** Space between a title's name and its bonus column. */
    private static final int NAME_GAP = 6;

    private final ScrollList titleScroll = new ScrollList(9);

    public ProfileTab() {
        super(Component.translatable("tab.overgearedmod.profile"));
    }

    private static PlayerIdentity identity() {
        return player().getData(ModAttachments.PLAYER_IDENTITY);
    }

    private static List<Definitions.Entry<TitleDefinition>> earnedTitles() {
        PlayerIdentity identity = identity();
        return Definitions.all(player().level(), ModDatapackRegistries.TITLE).stream()
                .filter(entry -> identity.hasTitle(entry.id()))
                .toList();
    }

    private boolean showingClassButtons;

    @Override
    protected void addWidgets(Consumer<AbstractWidget> addWidget) {
        showingClassButtons = identity().playerClass().isEmpty();
        if (!showingClassButtons) {
            return;
        }
        List<Definitions.Entry<PlayerClassDefinition>> classes = Definitions.all(player().level(), ModDatapackRegistries.PLAYER_CLASS).stream()
                .filter(entry -> entry.value().selectable())
                .toList();
        int buttonWidth = classes.isEmpty() ? 0 : (width - (classes.size() - 1) * 4) / classes.size();
        for (int i = 0; i < classes.size(); i++) {
            Definitions.Entry<PlayerClassDefinition> entry = classes.get(i);
            Button button = Button.builder(entry.value().name(), b -> PacketDistributor.sendToServer(new ChooseClassPayload(entry.id())))
                    .bounds(left + i * (buttonWidth + 4), top + 14, buttonWidth, 18)
                    .tooltip(Tooltip.create(classTooltip(entry.value())))
                    .build(SatisfyButton::new);
            addWidget.accept(button);
        }
    }

    @Override
    public boolean mouseScrolled(double wheel) {
        return titleScroll.scroll(wheel, earnedTitles().size());
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

    /** A title's stat bonuses, then its smithing experience bonus if it has one. */
    private static Component titleBonusText(TitleDefinition title) {
        MutableComponent text = bonusText(title.bonusStats()).copy();
        if (title.smithingXpBonus() > 0) {
            text.append(Component.translatable("screen.overgearedmod.smithing_xp_bonus", Math.round(title.smithingXpBonus() * 100)));
        }
        return text;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY) {
        UiStyle.heading(graphics, font(), Component.translatable("screen.overgearedmod.class"), left, top);
        // The bonuses follow the description, however many lines it wraps to, and the titles follow both.
        int titlesTop = ProgressionService.playerClass(player()).map(definition -> {
            UiStyle.text(graphics, font(), definition.name(), left + 50, top, UiStyle.TEXT);
            graphics.drawWordWrap(font(), definition.description(), left, top + 14, width, UiStyle.MUTED);
            int bonusY = top + 14 + font().split(definition.description(), width).size() * font().lineHeight + 4;
            UiStyle.text(graphics, font(), bonusText(definition.bonusStats()), left, bonusY, UiStyle.GOOD);
            return Math.max(TITLES_TOP, bonusY + font().lineHeight + 19 - top);
        }).orElseGet(() -> {
            UiStyle.text(graphics, font(), Component.translatable("screen.overgearedmod.choose_class"), left + 50, top, UiStyle.MUTED);
            return TITLES_TOP;
        });

        UiStyle.heading(graphics, font(), Component.translatable("screen.overgearedmod.titles"), left, top + titlesTop - 14);
        List<Definitions.Entry<TitleDefinition>> titles = earnedTitles();
        if (titles.isEmpty()) {
            UiStyle.text(graphics, font(), Component.translatable("screen.overgearedmod.no_titles"), left, top + titlesTop, UiStyle.MUTED);
            return;
        }
        int visibleTitles = (height - titlesTop) / TITLE_ROW;
        titleScroll.setVisibleRows(visibleTitles);
        titleScroll.clamp(titles.size());
        for (int i = titleScroll.first(); i < titleScroll.end(titles.size()); i++) {
            TitleDefinition title = titles.get(i).value();
            int y = top + titlesTop + (i - titleScroll.first()) * TITLE_ROW;
            // A long name is cut before the bonus column; the tooltip gives it whole.
            Component name = UiStyle.ellipsize(font(), Component.literal("✔ ").append(title.name()), width / 2 - NAME_GAP);
            UiStyle.text(graphics, font(), name, left, y, UiStyle.GOLD);
            UiStyle.text(graphics, font(), titleBonusText(title), left + width / 2, y, UiStyle.GOOD);
            if (mouseX >= left && mouseX < left + width && mouseY >= y - 1 && mouseY < y + TITLE_ROW - 1) {
                graphics.renderTooltip(font(), List.of(title.name().getVisualOrderText(), title.description().getVisualOrderText(), title.goal().describe().getVisualOrderText()), mouseX, mouseY);
            }
        }
        titleScroll.renderBar(graphics, left + width + 3, top + titlesTop - 1, visibleTitles * TITLE_ROW, titles.size());
    }
}
