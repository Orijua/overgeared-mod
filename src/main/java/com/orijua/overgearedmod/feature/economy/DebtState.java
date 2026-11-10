package com.orijua.overgearedmod.feature.economy;

import com.mojang.serialization.Codec;
import net.minecraft.util.StringRepresentable;

/** Where the starter debt of the capsule is: not taken yet, being paid, or paid off. */
public enum DebtState implements StringRepresentable {
    UNASSIGNED("unassigned"),
    ACTIVE("active"),
    SETTLED("settled");

    public static final Codec<DebtState> CODEC = StringRepresentable.fromEnum(DebtState::values);

    private final String name;

    DebtState(String name) {
        this.name = name;
    }

    @Override
    public String getSerializedName() {
        return name;
    }
}
