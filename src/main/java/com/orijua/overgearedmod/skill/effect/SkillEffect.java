package com.orijua.overgearedmod.skill.effect;

import com.mojang.serialization.Codec;
import com.orijua.overgearedmod.progression.PlayerStats;
import net.minecraft.server.level.ServerPlayer;

/**
 * What a skill does. The JSON "type" field picks the implementation (see {@link SkillEffectType});
 * add a new kind of skill by writing a record here and listing it in the enum.
 */
public interface SkillEffect {
    Codec<SkillEffect> CODEC = SkillEffectType.CODEC.dispatch("type", SkillEffect::type, SkillEffectType::codec);

    SkillEffectType type();

    void apply(ServerPlayer player, PlayerStats stats);
}
