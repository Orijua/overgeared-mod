package com.orijua.overgearedmod.skill.effect;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import net.minecraft.util.StringRepresentable;

import java.util.function.Supplier;

public enum SkillEffectType implements StringRepresentable {
    AREA_DAMAGE("area_damage", () -> AreaDamageEffect.CODEC),
    SELF_BUFF("self_buff", () -> SelfBuffEffect.CODEC),
    PROJECTILE("projectile", () -> ProjectileEffect.CODEC);

    public static final Codec<SkillEffectType> CODEC = StringRepresentable.fromEnum(SkillEffectType::values);

    private final String name;
    private final Supplier<MapCodec<? extends SkillEffect>> codec;

    SkillEffectType(String name, Supplier<MapCodec<? extends SkillEffect>> codec) {
        this.name = name;
        this.codec = codec;
    }

    public MapCodec<? extends SkillEffect> codec() {
        return codec.get();
    }

    @Override
    public String getSerializedName() {
        return name;
    }
}
