package com.orijua.overgearedmod.skill;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * Runtime combat resources of a player. {@code cooldownUntil} is an absolute game time (ticks),
 * so it needs no per-tick countdown.
 */
public record SkillState(float mana, long cooldownUntil) {
    public static final SkillState DEFAULT = new SkillState(20.0f, 0L);

    public static final Codec<SkillState> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.FLOAT.fieldOf("mana").forGetter(SkillState::mana),
            Codec.LONG.fieldOf("cooldown_until").forGetter(SkillState::cooldownUntil)
    ).apply(instance, SkillState::new));

    public static final StreamCodec<ByteBuf, SkillState> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.FLOAT, SkillState::mana,
            ByteBufCodecs.VAR_LONG, SkillState::cooldownUntil,
            SkillState::new);

    public SkillState withMana(float mana) {
        return new SkillState(mana, cooldownUntil);
    }

    public long cooldownRemaining(long gameTime) {
        return Math.max(0, cooldownUntil - gameTime);
    }
}
