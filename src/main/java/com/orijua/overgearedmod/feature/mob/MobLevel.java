package com.orijua.overgearedmod.feature.mob;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/** A hostile mob's level and whether it is an elite. Level 0 ({@link #NONE}) means the mob was never leveled. */
public record MobLevel(int level, boolean elite) {
    public static final MobLevel NONE = new MobLevel(0, false);

    public static final Codec<MobLevel> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.INT.fieldOf("level").forGetter(MobLevel::level),
            Codec.BOOL.optionalFieldOf("elite", false).forGetter(MobLevel::elite)
    ).apply(instance, MobLevel::new));

    public static final StreamCodec<ByteBuf, MobLevel> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, MobLevel::level,
            ByteBufCodecs.BOOL, MobLevel::elite,
            MobLevel::new);

    public boolean isLeveled() {
        return level > 0;
    }
}
