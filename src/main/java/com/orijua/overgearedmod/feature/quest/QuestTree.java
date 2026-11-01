package com.orijua.overgearedmod.feature.quest;

import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import javax.annotation.Nullable;

/**
 * What the Quests tab shows, as a tree. Pure: the tab hands it every quest and the player's state.
 * <ul>
 *   <li><b>Story</b>: the requires_quest chains, grouped by who gives them. Of each chain only the quests already done
 *   and the current ones show; under a current quest that leads on, a single locked "???" says the chain goes on.</li>
 *   <li><b>Completed</b>: the chains with nothing left to do, to show folded. A failed quest ends its chain.</li>
 *   <li><b>Repeatable</b>: repeatable quests, apart from the chains (a quest that requires one starts its own chain).</li>
 * </ul>
 * Undiscovered hidden quests never show, not even as "???".
 */
public final class QuestTree {
    private QuestTree() {
    }

    /** One quest as the tree needs it. {@code group} is whatever the tab groups by (who gives it). */
    public record Quest(ResourceLocation id, Optional<ResourceLocation> requires, String group, boolean repeatable,
                        boolean hidden) {
    }

    public enum State { DONE, FAILED, CURRENT, LOCKED }

    /**
     * A line of a chain. {@code id} is null for the locked "???". {@code depth} only grows where a chain branches, so a
     * straight chain stays in one column.
     */
    public record Node(@Nullable ResourceLocation id, int depth, State state) {
    }

    /** A chain from its first quest, with the nodes to show in order. */
    public record Chain(ResourceLocation root, List<Node> nodes) {
        /** Nothing current and nothing locked: every quest it shows is done (or failed for good). */
        public boolean finished() {
            return nodes.stream().allMatch(node -> node.state() == State.DONE || node.state() == State.FAILED);
        }

        public long done() {
            return nodes.stream().filter(node -> node.state() == State.DONE).count();
        }
    }

    /** Chains of one giver, in the order the quests came. */
    public record Group(String key, List<Chain> chains) {
    }

    public record View(List<Group> story, List<Group> completed, List<ResourceLocation> repeatable) {
    }

    public static View build(List<Quest> quests, PlayerQuests state) {
        Map<ResourceLocation, Quest> byId = new HashMap<>();
        quests.forEach(quest -> byId.put(quest.id(), quest));

        // Repeatable quests stand apart, so a quest that requires one is the first of its own chain.
        Map<ResourceLocation, List<Quest>> next = new HashMap<>();
        List<Quest> roots = new ArrayList<>();
        for (Quest quest : quests) {
            if (quest.repeatable()) {
                continue;
            }
            Optional<Quest> previous = quest.requires().map(byId::get).filter(p -> !p.repeatable());
            if (previous.isPresent()) {
                next.computeIfAbsent(previous.get().id(), k -> new ArrayList<>()).add(quest);
            } else {
                roots.add(quest);
            }
        }

        Map<String, List<Chain>> story = new LinkedHashMap<>();
        Map<String, List<Chain>> completed = new LinkedHashMap<>();
        for (Quest root : roots) {
            List<Node> nodes = new ArrayList<>();
            visit(root, 0, next, state, nodes, new HashSet<>());
            if (nodes.isEmpty()) {
                continue;
            }
            Chain chain = new Chain(root.id(), List.copyOf(nodes));
            (chain.finished() ? completed : story).computeIfAbsent(root.group(), k -> new ArrayList<>()).add(chain);
        }

        List<ResourceLocation> repeatable = quests.stream()
                .filter(Quest::repeatable)
                .filter(quest -> visible(quest, state))
                .map(Quest::id)
                .toList();
        return new View(groups(story), groups(completed), repeatable);
    }

    private static void visit(Quest quest, int depth, Map<ResourceLocation, List<Quest>> next, PlayerQuests state,
                              List<Node> nodes, Set<ResourceLocation> seen) {
        if (!seen.add(quest.id()) || !visible(quest, state)) {
            return;
        }
        boolean unlocked = quest.requires().map(state::isCompleted).orElse(true);
        List<Quest> after = next.getOrDefault(quest.id(), List.of());
        int childDepth = after.size() > 1 ? depth + 1 : depth;
        if (state.isCompleted(quest.id())) {
            nodes.add(new Node(quest.id(), depth, State.DONE));
            after.forEach(child -> visit(child, childDepth, next, state, nodes, seen));
        } else if (state.isFailed(quest.id())) {
            nodes.add(new Node(quest.id(), depth, State.FAILED)); // nothing after it can ever be taken
        } else if (unlocked) {
            nodes.add(new Node(quest.id(), depth, State.CURRENT));
            // A secret next step stays a secret: only an open one is hinted at.
            if (after.stream().anyMatch(child -> !child.hidden())) {
                nodes.add(new Node(null, depth, State.LOCKED));
            }
        }
    }

    private static boolean visible(Quest quest, PlayerQuests state) {
        return !quest.hidden() || state.isDiscovered(quest.id());
    }

    private static List<Group> groups(Map<String, List<Chain>> byKey) {
        return byKey.entrySet().stream().map(entry -> new Group(entry.getKey(), List.copyOf(entry.getValue()))).toList();
    }
}
