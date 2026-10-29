package com.orijua.overgearedmod.equipment;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.orijua.overgearedmod.progression.PlayerStats;
import com.orijua.overgearedmod.progression.StatType;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

import java.util.Map;

/** Minimum level and stats needed to fight with an item, e.g. {"level": 3, "stats": {"strength": 5}}. */
public record EquipRequirements(int level, Map<StatType, Integer> stats) {
    public static final Codec<EquipRequirements> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.intRange(1, 1000).optionalFieldOf("level", 1).forGetter(EquipRequirements::level),
            Codec.unboundedMap(StatType.CODEC, Codec.INT).optionalFieldOf("stats", Map.of()).forGetter(EquipRequirements::stats)
    ).apply(instance, EquipRequirements::new));

    public static final StreamCodec<ByteBuf, EquipRequirements> STREAM_CODEC = ByteBufCodecs.fromCodec(CODEC);

    public boolean metBy(PlayerStats player) {
        return player.level() >= level
                && stats.entrySet().stream().allMatch(e -> player.get(e.getKey()) >= e.getValue());
    }
}
