package com.orijua.overgearedmod.quest;

import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class QuestRouteTest {
    private static final ResourceLocation WOOD = id("wood");
    private static final ResourceLocation IRON = id("iron");
    private static final ResourceLocation JAFFA = id("jaffa");
    private static final ResourceLocation SWORD = id("sword");
    private static final ResourceLocation LONE = id("lone");
    /** wood → iron → jaffa → sword. */
    private static final Map<ResourceLocation, ResourceLocation> CHAIN = Map.of(IRON, WOOD, JAFFA, IRON, SWORD, JAFFA);

    @Test
    void anyQuestOfAChainGivesTheWholeChainInOrder() {
        List<ResourceLocation> route = List.of(WOOD, IRON, JAFFA, SWORD);
        assertEquals(route, QuestRoute.of(WOOD, CHAIN));
        assertEquals(route, QuestRoute.of(JAFFA, CHAIN));
        assertEquals(route, QuestRoute.of(SWORD, CHAIN));
    }

    @Test
    void aQuestOutsideAnyChainIsItsOwnRoute() {
        assertEquals(List.of(LONE), QuestRoute.of(LONE, CHAIN));
    }

    @Test
    void branchesAreListedAfterTheirStep() {
        ResourceLocation side = id("a_side_quest");
        Map<ResourceLocation, ResourceLocation> branched = Map.of(IRON, WOOD, side, WOOD, JAFFA, IRON);
        assertEquals(List.of(WOOD, side, IRON, JAFFA), QuestRoute.of(JAFFA, branched), "both quests after wood, by id");
    }

    @Test
    void aLoopingChainStillEnds() {
        Map<ResourceLocation, ResourceLocation> loop = Map.of(WOOD, IRON, IRON, WOOD);
        assertEquals(2, QuestRoute.of(WOOD, loop).size());
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath("test", path);
    }
}
