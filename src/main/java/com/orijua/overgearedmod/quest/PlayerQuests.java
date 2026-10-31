package com.orijua.overgearedmod.quest;

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
 * the ids of quests already completed, the hidden quests the player has discovered, and which of those
 * discoveries they have already seen in the Quests tab (the rest are marked "New!").
 */
public record PlayerQuests(Map<ResourceLocation, Integer> active, List<ResourceLocation> completed,
                           List<ResourceLocation> discovered, List<ResourceLocation> seen) {
    public static final PlayerQuests EMPTY = new PlayerQuests(Map.of(), List.of(), List.of(), List.of());

    public static final Codec<PlayerQuests> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.unboundedMap(ResourceLocation.CODEC, Codec.INT).optionalFieldOf("active", Map.of()).forGetter(PlayerQuests::active),
            ResourceLocation.CODEC.listOf().optionalFieldOf("completed", List.of()).forGetter(PlayerQuests::completed),
            // Optional so worlds saved before hidden quests (and their "New!" mark) existed still load.
            ResourceLocation.CODEC.listOf().optionalFieldOf("discovered", List.of()).forGetter(PlayerQuests::discovered),
            ResourceLocation.CODEC.listOf().optionalFieldOf("seen", List.of()).forGetter(PlayerQuests::seen)
    ).apply(instance, PlayerQuests::new));

    public static final StreamCodec<ByteBuf, PlayerQuests> STREAM_CODEC = ByteBufCodecs.fromCodec(CODEC);

    public boolean isActive(ResourceLocation quest) {
        return active.containsKey(quest);
    }

    public boolean isCompleted(ResourceLocation quest) {
        return completed.contains(quest);
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

    public PlayerQuests accept(ResourceLocation quest, int baseline) {
        Map<ResourceLocation, Integer> newActive = new HashMap<>(active);
        newActive.put(quest, baseline);
        return new PlayerQuests(Map.copyOf(newActive), completed, discovered, seen);
    }

    /** Adds {@code amount} to what an active deliver quest has received; unchanged if the quest isn't active. */
    public PlayerQuests deliver(ResourceLocation quest, int amount) {
        if (!isActive(quest) || amount <= 0) {
            return this;
        }
        Map<ResourceLocation, Integer> newActive = new HashMap<>(active);
        newActive.merge(quest, amount, Integer::sum);
        return new PlayerQuests(Map.copyOf(newActive), completed, discovered, seen);
    }

    public PlayerQuests abandon(ResourceLocation quest) {
        Map<ResourceLocation, Integer> newActive = new HashMap<>(active);
        newActive.remove(quest);
        return new PlayerQuests(Map.copyOf(newActive), completed, discovered, seen);
    }

    public PlayerQuests complete(ResourceLocation quest) {
        Map<ResourceLocation, Integer> newActive = new HashMap<>(active);
        newActive.remove(quest);
        List<ResourceLocation> newCompleted = new ArrayList<>(completed);
        if (!newCompleted.contains(quest)) {
            newCompleted.add(quest);
        }
        return new PlayerQuests(Map.copyOf(newActive), List.copyOf(newCompleted), discovered, seen);
    }

    public PlayerQuests discover(ResourceLocation quest) {
        if (isDiscovered(quest)) {
            return this;
        }
        List<ResourceLocation> newDiscovered = new ArrayList<>(discovered);
        newDiscovered.add(quest);
        return new PlayerQuests(active, completed, List.copyOf(newDiscovered), seen);
    }

    /** Every discovery so far counts as seen; returns the same instance if there was nothing new. */
    public PlayerQuests seeAllDiscovered() {
        return hasNew() ? new PlayerQuests(active, completed, discovered, discovered) : this;
    }
}
