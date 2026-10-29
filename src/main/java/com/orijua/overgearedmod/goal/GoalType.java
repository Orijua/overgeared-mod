package com.orijua.overgearedmod.goal;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import net.minecraft.util.StringRepresentable;

import java.util.function.Supplier;

public enum GoalType implements StringRepresentable {
    KILL("kill", () -> KillGoal.CODEC),
    FORGE("forge", () -> ForgeGoal.CODEC),
    LEVEL("level", () -> LevelGoal.CODEC);

    public static final Codec<GoalType> CODEC = StringRepresentable.fromEnum(GoalType::values);

    private final String name;
    private final Supplier<MapCodec<? extends Goal>> codec;

    GoalType(String name, Supplier<MapCodec<? extends Goal>> codec) {
        this.name = name;
        this.codec = codec;
    }

    public MapCodec<? extends Goal> codec() {
        return codec.get();
    }

    @Override
    public String getSerializedName() {
        return name;
    }
}
