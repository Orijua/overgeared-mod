package com.orijua.overgearedmod.feature.equipment.option;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import net.minecraft.util.StringRepresentable;

import java.util.function.Supplier;

public enum OptionEffectType implements StringRepresentable {
    ATTRIBUTE("attribute", () -> AttributeOptionEffect.CODEC),
    LIFE_STEAL("life_steal", () -> LifeStealOptionEffect.CODEC),
    APPLY_EFFECT("apply_effect", () -> ApplyEffectOptionEffect.CODEC),
    STAT("stat", () -> StatOptionEffect.CODEC),
    BLOCK_CHANCE("block_chance", () -> BlockChanceOptionEffect.CODEC),
    DARK_DAMAGE("dark_damage", () -> DarkDamageOptionEffect.CODEC),
    FEAR("fear", () -> FearOptionEffect.CODEC),
    SKILL("skill", () -> SkillOptionEffect.CODEC),
    FORGING_SKILL("forging_skill", () -> ForgingSkillOptionEffect.CODEC),
    SMITHING_XP("smithing_xp", () -> SmithingXpOptionEffect.CODEC);

    public static final Codec<OptionEffectType> CODEC = StringRepresentable.fromEnum(OptionEffectType::values);

    private final String name;
    private final Supplier<MapCodec<? extends OptionEffect>> codec;

    OptionEffectType(String name, Supplier<MapCodec<? extends OptionEffect>> codec) {
        this.name = name;
        this.codec = codec;
    }

    public MapCodec<? extends OptionEffect> codec() {
        return codec.get();
    }

    @Override
    public String getSerializedName() {
        return name;
    }
}
