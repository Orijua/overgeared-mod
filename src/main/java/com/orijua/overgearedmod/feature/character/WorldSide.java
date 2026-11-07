package com.orijua.overgearedmod.feature.character;

import com.mojang.serialization.Codec;
import com.orijua.overgearedmod.feature.world.Satisfy;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.Level;

/** Which of the two characters a level belongs to: the real world (overworld, Nether, End) or Satisfy. */
public enum WorldSide implements StringRepresentable {
    REAL("real"),
    SATISFY("satisfy");

    public static final Codec<WorldSide> CODEC = StringRepresentable.fromEnum(WorldSide::values);

    private final String name;

    WorldSide(String name) {
        this.name = name;
    }

    @Override
    public String getSerializedName() {
        return name;
    }

    /** The single definition of "which world is this level": Satisfy, or else the real world. */
    public static WorldSide of(Level level) {
        return Satisfy.is(level) ? SATISFY : REAL;
    }
}
