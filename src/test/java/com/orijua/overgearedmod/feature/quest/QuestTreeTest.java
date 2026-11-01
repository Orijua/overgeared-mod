package com.orijua.overgearedmod.feature.quest;

import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class QuestTreeTest {
    private static final ResourceLocation WOOD = id("wood");
    private static final ResourceLocation IRON = id("iron");
    private static final ResourceLocation JAFFA = id("jaffa");
    private static final ResourceLocation PLAGUE = id("plague");
    private static final ResourceLocation WATCH = id("watch");
    private static final ResourceLocation SECRET = id("secret");

    /** Smith: wood → iron → jaffa. Chief: plague (repeatable) → watch. Plus a secret quest. */
    private static final List<QuestTree.Quest> QUESTS = List.of(
            quest(WOOD, null, "smith"),
            quest(IRON, WOOD, "smith"),
            quest(JAFFA, IRON, "smith"),
            new QuestTree.Quest(PLAGUE, Optional.empty(), "chief", true, false),
            quest(WATCH, PLAGUE, "chief"),
            new QuestTree.Quest(SECRET, Optional.empty(), "class", false, true));

    @Test
    void aFreshChainShowsItsFirstQuestAndALockedNextStep() {
        QuestTree.View view = QuestTree.build(QUESTS, PlayerQuests.EMPTY);
        assertEquals(1, view.story().size(), "only the smith's chain: watch waits for the plague");
        QuestTree.Group smith = view.story().getFirst();
        assertEquals("smith", smith.key());
        assertEquals(List.of(new QuestTree.Node(WOOD, 0, QuestTree.State.CURRENT), new QuestTree.Node(null, 0, QuestTree.State.LOCKED)),
                smith.chains().getFirst().nodes());
    }

    @Test
    void doneStepsStayAndTheNextOneOpens() {
        PlayerQuests state = PlayerQuests.EMPTY.complete(WOOD).complete(IRON);
        List<QuestTree.Node> nodes = QuestTree.build(QUESTS, state).story().getFirst().chains().getFirst().nodes();
        assertEquals(List.of(new QuestTree.Node(WOOD, 0, QuestTree.State.DONE), new QuestTree.Node(IRON, 0, QuestTree.State.DONE),
                new QuestTree.Node(JAFFA, 0, QuestTree.State.CURRENT)), nodes, "the last step has no ??? under it");
    }

    @Test
    void aFinishedChainMovesToCompleted() {
        PlayerQuests state = PlayerQuests.EMPTY.complete(WOOD).complete(IRON).complete(JAFFA);
        QuestTree.View view = QuestTree.build(QUESTS, state);
        assertTrue(view.story().isEmpty());
        QuestTree.Chain chain = view.completed().getFirst().chains().getFirst();
        assertTrue(chain.finished());
        assertEquals(3, chain.done());
    }

    @Test
    void repeatableQuestsStandApartAndUnlockTheirOwnChain() {
        QuestTree.View before = QuestTree.build(QUESTS, PlayerQuests.EMPTY);
        assertEquals(List.of(PLAGUE), before.repeatable());

        QuestTree.View after = QuestTree.build(QUESTS, PlayerQuests.EMPTY.complete(PLAGUE));
        QuestTree.Group chief = after.story().get(1);
        assertEquals("chief", chief.key());
        assertEquals(List.of(new QuestTree.Node(WATCH, 0, QuestTree.State.CURRENT)), chief.chains().getFirst().nodes());
        assertEquals(List.of(PLAGUE), after.repeatable(), "still repeatable after doing it");
    }

    @Test
    void hiddenQuestsShowOnlyOnceDiscovered() {
        assertTrue(QuestTree.build(QUESTS, PlayerQuests.EMPTY).story().stream().noneMatch(group -> group.key().equals("class")));
        QuestTree.View view = QuestTree.build(QUESTS, PlayerQuests.EMPTY.discover(SECRET));
        assertEquals("class", view.story().getLast().key());
    }

    @Test
    void aHiddenNextStepIsNotHintedAt() {
        ResourceLocation hiddenNext = id("hidden_next");
        List<QuestTree.Quest> quests = List.of(quest(WOOD, null, "smith"),
                new QuestTree.Quest(hiddenNext, Optional.of(WOOD), "smith", false, true));
        assertEquals(List.of(new QuestTree.Node(WOOD, 0, QuestTree.State.CURRENT)),
                QuestTree.build(quests, PlayerQuests.EMPTY).story().getFirst().chains().getFirst().nodes());
    }

    @Test
    void branchesAreIndentedAndAStraightChainIsNot() {
        ResourceLocation side = id("side");
        List<QuestTree.Quest> quests = List.of(quest(WOOD, null, "smith"), quest(IRON, WOOD, "smith"), quest(side, WOOD, "smith"));
        List<QuestTree.Node> nodes = QuestTree.build(quests, PlayerQuests.EMPTY.complete(WOOD)).story().getFirst().chains().getFirst().nodes();
        assertEquals(List.of(new QuestTree.Node(WOOD, 0, QuestTree.State.DONE), new QuestTree.Node(IRON, 1, QuestTree.State.CURRENT),
                new QuestTree.Node(side, 1, QuestTree.State.CURRENT)), nodes);
    }

    @Test
    void aLoopingChainStillEnds() {
        List<QuestTree.Quest> loop = List.of(quest(WOOD, IRON, "smith"), quest(IRON, WOOD, "smith"));
        assertTrue(QuestTree.build(loop, PlayerQuests.EMPTY).story().isEmpty(), "neither can start, and building ends");
    }

    private static QuestTree.Quest quest(ResourceLocation id, ResourceLocation requires, String group) {
        return new QuestTree.Quest(id, Optional.ofNullable(requires), group, false, false);
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath("test", path);
    }
}
