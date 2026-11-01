package com.orijua.overgearedmod.feature.quest;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Accepted quests, each with the value its goal stored (the goal's progress at the moment of accepting, so only later
 * kills/forges count, or for a deliver goal what has been handed over so far),
 * the ids of quests already completed (and how many times, for repeatable ones), the hidden quests the player has
 * discovered, which of those discoveries they have already seen in the Quests tab (the rest are marked "New!"), and
 * the accepted quests they follow on the HUD (at most {@link #MAX_TRACKED}).
 */
public record PlayerQuests(Map<ResourceLocation, Integer> active, List<ResourceLocation> completed,
                           List<ResourceLocation> discovered, List<ResourceLocation> seen,
                           Map<ResourceLocation, Integer> timesCompleted, List<ResourceLocation> tracked) {
    public static final PlayerQuests EMPTY = new PlayerQuests(Map.of(), List.of(), List.of(), List.of(), Map.of(), List.of());

    /** How many quests the HUD panel follows at once. */
    public static final int MAX_TRACKED = 3;

    public static final Codec<PlayerQuests> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.unboundedMap(ResourceLocation.CODEC, Codec.INT).optionalFieldOf("active", Map.of()).forGetter(PlayerQuests::active),
            ResourceLocation.CODEC.listOf().optionalFieldOf("completed", List.of()).forGetter(PlayerQuests::completed),
            // Optional so worlds saved before hidden quests (and their "New!" mark) existed still load.
            ResourceLocation.CODEC.listOf().optionalFieldOf("discovered", List.of()).forGetter(PlayerQuests::discovered),
            ResourceLocation.CODEC.listOf().optionalFieldOf("seen", List.of()).forGetter(PlayerQuests::seen),
            // Optional too: worlds saved before the quest tree counted completions or followed quests on the HUD.
            Codec.unboundedMap(ResourceLocation.CODEC, Codec.INT).optionalFieldOf("times_completed", Map.of())
                    .forGetter(PlayerQuests::timesCompleted),
            ResourceLocation.CODEC.listOf().optionalFieldOf("tracked", List.of()).forGetter(PlayerQuests::tracked)
    ).apply(instance, PlayerQuests::new));

    public static final StreamCodec<ByteBuf, PlayerQuests> STREAM_CODEC = ByteBufCodecs.fromCodec(CODEC);

    public boolean isActive(ResourceLocation quest) {
        return active.containsKey(quest);
    }

    public boolean isCompleted(ResourceLocation quest) {
        return completed.contains(quest);
    }

    /** At least 1 once completed, even for saves from before completions were counted. */
    public int timesCompleted(ResourceLocation quest) {
        return Math.max(timesCompleted.getOrDefault(quest, 0), isCompleted(quest) ? 1 : 0);
    }

    public boolean isTracked(ResourceLocation quest) {
        return tracked.contains(quest);
    }

    public boolean isDiscovered(ResourceLocation quest) {
        return discovered.contains(quest);
    }

    /** Discovered but not yet looked at in the Quests tab. */
    public boolean isNew(ResourceLocation quest) {
        return isDiscovered(quest) && !seen.contains(quest);
    }

    public boolean hasNew() {
        return discovered.stream().anyMatch(quest -> !seen.contains(quest));
    }

    /** Also follows it on the HUD while there is room. */
    public PlayerQuests accept(ResourceLocation quest, int baseline) {
        Map<ResourceLocation, Integer> newActive = new HashMap<>(active);
        newActive.put(quest, baseline);
        List<ResourceLocation> newTracked = tracked.size() < MAX_TRACKED && !isTracked(quest) ? with(tracked, quest) : tracked;
        return new PlayerQuests(Map.copyOf(newActive), completed, discovered, seen, timesCompleted, newTracked);
    }

    /** Adds {@code amount} to what an active deliver quest has received; unchanged if the quest isn't active. */
    public PlayerQuests deliver(ResourceLocation quest, int amount) {
        if (!isActive(quest) || amount <= 0) {
            return this;
        }
        Map<ResourceLocation, Integer> newActive = new HashMap<>(active);
        newActive.merge(quest, amount, Integer::sum);
        return new PlayerQuests(Map.copyOf(newActive), completed, discovered, seen, timesCompleted, tracked);
    }

    public PlayerQuests abandon(ResourceLocation quest) {
        Map<ResourceLocation, Integer> newActive = new HashMap<>(active);
        newActive.remove(quest);
        return new PlayerQuests(Map.copyOf(newActive), completed, discovered, seen, timesCompleted, without(tracked, quest));
    }

    public PlayerQuests complete(ResourceLocation quest) {
        Map<ResourceLocation, Integer> newActive = new HashMap<>(active);
        newActive.remove(quest);
        List<ResourceLocation> newCompleted = isCompleted(quest) ? completed : with(completed, quest);
        Map<ResourceLocation, Integer> newTimes = new HashMap<>(timesCompleted);
        newTimes.put(quest, timesCompleted(quest) + 1);
        return new PlayerQuests(Map.copyOf(newActive), newCompleted, discovered, seen, Map.copyOf(newTimes), without(tracked, quest));
    }

    /**
     * Follows an accepted quest on the HUD, or stops following it. Unchanged (the same instance) if the quest isn't
     * active, or if it isn't followed yet and {@link #MAX_TRACKED} already are.
     */
    public PlayerQuests toggleTracked(ResourceLocation quest) {
        if (isTracked(quest)) {
            return new PlayerQuests(active, completed, discovered, seen, timesCompleted, without(tracked, quest));
        }
        if (!isActive(quest) || tracked.size() >= MAX_TRACKED) {
            return this;
        }
        return new PlayerQuests(active, completed, discovered, seen, timesCompleted, with(tracked, quest));
    }

    public PlayerQuests discover(ResourceLocation quest) {
        if (isDiscovered(quest)) {
            return this;
        }
        return new PlayerQuests(active, completed, with(discovered, quest), seen, timesCompleted, tracked);
    }

    /** Every discovery so far counts as seen; returns the same instance if there was nothing new. */
    public PlayerQuests seeAllDiscovered() {
        return hasNew() ? new PlayerQuests(active, completed, discovered, discovered, timesCompleted, tracked) : this;
    }

    private static List<ResourceLocation> with(List<ResourceLocation> list, ResourceLocation quest) {
        List<ResourceLocation> copy = new ArrayList<>(list);
        copy.add(quest);
        return List.copyOf(copy);
    }

    private static List<ResourceLocation> without(List<ResourceLocation> list, ResourceLocation quest) {
        return list.contains(quest) ? list.stream().filter(id -> !id.equals(quest)).toList() : list;
    }
}
