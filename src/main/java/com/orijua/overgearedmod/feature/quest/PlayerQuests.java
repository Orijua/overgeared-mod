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
import java.util.OptionalLong;

/**
 * Accepted quests, each with the value its goal stored (the goal's progress at the moment of accepting, so only later
 * kills/forges count, or for a deliver goal what has been handed over so far),
 * the ids of quests already completed (and how many times, for repeatable ones), the hidden quests the player has
 * discovered, which of those discoveries they have already seen in the Quests tab (the rest are marked "New!"),
 * the accepted quests they follow on the HUD (at most {@link #MAX_TRACKED}), and for quests with a time limit: when
 * each one fails (in overworld day time), which ones have already given their last-day warning, and the quests failed
 * for good.
 */
public record PlayerQuests(Map<ResourceLocation, Integer> active, List<ResourceLocation> completed,
                           List<ResourceLocation> discovered, List<ResourceLocation> seen,
                           Map<ResourceLocation, Integer> timesCompleted, List<ResourceLocation> tracked,
                           Map<ResourceLocation, Long> deadlines, List<ResourceLocation> warned,
                           List<ResourceLocation> failed) {
    public static final PlayerQuests EMPTY = new PlayerQuests(Map.of(), List.of(), List.of(), List.of(), Map.of(), List.of(),
            Map.of(), List.of(), List.of());

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
            ResourceLocation.CODEC.listOf().optionalFieldOf("tracked", List.of()).forGetter(PlayerQuests::tracked),
            // And worlds saved before quests had time limits.
            Codec.unboundedMap(ResourceLocation.CODEC, Codec.LONG).optionalFieldOf("deadlines", Map.of())
                    .forGetter(PlayerQuests::deadlines),
            ResourceLocation.CODEC.listOf().optionalFieldOf("warned", List.of()).forGetter(PlayerQuests::warned),
            ResourceLocation.CODEC.listOf().optionalFieldOf("failed", List.of()).forGetter(PlayerQuests::failed)
    ).apply(instance, PlayerQuests::new));

    public static final StreamCodec<ByteBuf, PlayerQuests> STREAM_CODEC = ByteBufCodecs.fromCodec(CODEC);

    public boolean isActive(ResourceLocation quest) {
        return active.containsKey(quest);
    }

    public boolean isCompleted(ResourceLocation quest) {
        return completed.contains(quest);
    }

    /** Failed for good: the time ran out. It can never be accepted again. */
    public boolean isFailed(ResourceLocation quest) {
        return failed.contains(quest);
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

    /** When an accepted quest with a time limit fails (overworld day time); empty for quests without one. */
    public OptionalLong deadline(ResourceLocation quest) {
        Long at = deadlines.get(quest);
        return at == null ? OptionalLong.empty() : OptionalLong.of(at);
    }

    /** Active quests whose time ran out at {@code now}. */
    public List<ResourceLocation> overdue(long now) {
        return deadlines.entrySet().stream().filter(entry -> now >= entry.getValue()).map(Map.Entry::getKey).sorted().toList();
    }

    /** Active quests with less than {@code window} left at {@code now} that haven't been warned yet. */
    public List<ResourceLocation> dueSoon(long now, long window) {
        return deadlines.entrySet().stream()
                .filter(entry -> entry.getValue() - now <= window && !warned.contains(entry.getKey()))
                .map(Map.Entry::getKey).sorted().toList();
    }

    /** Also follows it on the HUD while there is room. */
    public PlayerQuests accept(ResourceLocation quest, int baseline) {
        Map<ResourceLocation, Integer> newActive = new HashMap<>(active);
        newActive.put(quest, baseline);
        List<ResourceLocation> newTracked = tracked.size() < MAX_TRACKED && !isTracked(quest) ? with(tracked, quest) : tracked;
        return new PlayerQuests(Map.copyOf(newActive), completed, discovered, seen, timesCompleted, newTracked, deadlines, warned, failed);
    }

    /** Sets when an active quest fails; unchanged if the quest isn't active. */
    public PlayerQuests withDeadline(ResourceLocation quest, long at) {
        if (!isActive(quest)) {
            return this;
        }
        Map<ResourceLocation, Long> newDeadlines = new HashMap<>(deadlines);
        newDeadlines.put(quest, at);
        return new PlayerQuests(active, completed, discovered, seen, timesCompleted, tracked, Map.copyOf(newDeadlines),
                without(warned, quest), failed);
    }

    /** The last-day warning for {@code quest} has been given. */
    public PlayerQuests warn(ResourceLocation quest) {
        if (warned.contains(quest)) {
            return this;
        }
        return new PlayerQuests(active, completed, discovered, seen, timesCompleted, tracked, deadlines, with(warned, quest), failed);
    }

    /** Adds {@code amount} to what an active deliver quest has received; unchanged if the quest isn't active. */
    public PlayerQuests deliver(ResourceLocation quest, int amount) {
        if (!isActive(quest) || amount <= 0) {
            return this;
        }
        Map<ResourceLocation, Integer> newActive = new HashMap<>(active);
        newActive.merge(quest, amount, Integer::sum);
        return new PlayerQuests(Map.copyOf(newActive), completed, discovered, seen, timesCompleted, tracked, deadlines, warned, failed);
    }

    public PlayerQuests abandon(ResourceLocation quest) {
        return close(quest);
    }

    public PlayerQuests complete(ResourceLocation quest) {
        PlayerQuests closed = close(quest);
        List<ResourceLocation> newCompleted = isCompleted(quest) ? completed : with(completed, quest);
        Map<ResourceLocation, Integer> newTimes = new HashMap<>(timesCompleted);
        newTimes.put(quest, timesCompleted(quest) + 1);
        return new PlayerQuests(closed.active, newCompleted, discovered, seen, Map.copyOf(newTimes), closed.tracked,
                closed.deadlines, closed.warned, failed);
    }

    /** The time ran out: no longer active, and failed for good. */
    public PlayerQuests fail(ResourceLocation quest) {
        return close(quest).withFailed(isFailed(quest) ? failed : with(failed, quest));
    }

    /**
     * Follows an accepted quest on the HUD, or stops following it. Unchanged (the same instance) if the quest isn't
     * active, or if it isn't followed yet and {@link #MAX_TRACKED} already are.
     */
    public PlayerQuests toggleTracked(ResourceLocation quest) {
        if (isTracked(quest)) {
            return new PlayerQuests(active, completed, discovered, seen, timesCompleted, without(tracked, quest), deadlines, warned, failed);
        }
        if (!isActive(quest) || tracked.size() >= MAX_TRACKED) {
            return this;
        }
        return new PlayerQuests(active, completed, discovered, seen, timesCompleted, with(tracked, quest), deadlines, warned, failed);
    }

    public PlayerQuests discover(ResourceLocation quest) {
        if (isDiscovered(quest)) {
            return this;
        }
        return new PlayerQuests(active, completed, with(discovered, quest), seen, timesCompleted, tracked, deadlines, warned, failed);
    }

    /** Every discovery so far counts as seen; returns the same instance if there was nothing new. */
    public PlayerQuests seeAllDiscovered() {
        return hasNew() ? new PlayerQuests(active, completed, discovered, discovered, timesCompleted, tracked, deadlines, warned, failed) : this;
    }

    /** No longer active, followed or timed. */
    private PlayerQuests close(ResourceLocation quest) {
        Map<ResourceLocation, Integer> newActive = new HashMap<>(active);
        newActive.remove(quest);
        Map<ResourceLocation, Long> newDeadlines = new HashMap<>(deadlines);
        newDeadlines.remove(quest);
        return new PlayerQuests(Map.copyOf(newActive), completed, discovered, seen, timesCompleted, without(tracked, quest),
                Map.copyOf(newDeadlines), without(warned, quest), failed);
    }

    private PlayerQuests withFailed(List<ResourceLocation> newFailed) {
        return new PlayerQuests(active, completed, discovered, seen, timesCompleted, tracked, deadlines, warned, newFailed);
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
