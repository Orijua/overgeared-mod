package com.orijua.overgearedmod.quest;

import net.minecraft.resources.ResourceLocation;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** The route of a quest: every quest of its requires_quest chain, from the first to the last. Pure. */
public final class QuestRoute {
    private QuestRoute() {
    }

    /**
     * {@code quest}'s chain in order: back to the quest that requires nothing, then forward through every quest that
     * requires one already listed (siblings by id). Just {@code [quest]} when it isn't part of a chain.
     *
     * @param requires each quest → the quest its requires_quest points to
     */
    public static List<ResourceLocation> of(ResourceLocation quest, Map<ResourceLocation, ResourceLocation> requires) {
        ResourceLocation first = quest;
        Set<ResourceLocation> seen = new HashSet<>(Set.of(quest));
        while (requires.containsKey(first) && seen.add(requires.get(first))) {
            first = requires.get(first);
        }

        Map<ResourceLocation, List<ResourceLocation>> next = new HashMap<>();
        requires.forEach((later, earlier) -> next.computeIfAbsent(earlier, k -> new ArrayList<>()).add(later));
        next.values().forEach(list -> list.sort(null));

        List<ResourceLocation> route = new ArrayList<>();
        Set<ResourceLocation> listed = new HashSet<>();
        Deque<ResourceLocation> queue = new ArrayDeque<>(List.of(first));
        while (!queue.isEmpty()) {
            ResourceLocation current = queue.poll();
            if (listed.add(current)) {
                route.add(current);
                queue.addAll(next.getOrDefault(current, List.of()));
            }
        }
        return route;
    }
}
