package com.orijua.overgearedmod.goal;

import com.mojang.serialization.Codec;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

import java.util.HashMap;
import java.util.Map;

/**
 * Lifetime tallies used by goals, keyed by strings like "kill", "kill/minecraft:zombie",
 * "forge" or "forge/epic". See {@link GoalTracker} for what increments them.
 */
public record PlayerCounters(Map<String, Integer> values) {
    public static final PlayerCounters EMPTY = new PlayerCounters(Map.of());

    public static final Codec<PlayerCounters> CODEC = Codec.unboundedMap(Codec.STRING, Codec.INT)
            .xmap(PlayerCounters::new, PlayerCounters::values);
    public static final StreamCodec<ByteBuf, PlayerCounters> STREAM_CODEC = ByteBufCodecs.fromCodec(CODEC);

    public int get(String key) {
        return values.getOrDefault(key, 0);
    }

    public PlayerCounters increment(String key) {
        Map<String, Integer> copy = new HashMap<>(values);
        copy.merge(key, 1, Integer::sum);
        return new PlayerCounters(Map.copyOf(copy));
    }
}
