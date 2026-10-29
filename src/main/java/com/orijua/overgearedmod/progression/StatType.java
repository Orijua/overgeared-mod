package com.orijua.overgearedmod.progression;

import com.mojang.serialization.Codec;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.ByIdMap;
import net.minecraft.util.StringRepresentable;

/**
 * The five base stats. Canon: Satisfy has strength, stamina, agility, intelligence and dexterity
 * among many others (dignity, insight, courage...). What each one does here is project design.
 */
public enum StatType implements StringRepresentable {
    STRENGTH("strength"),
    STAMINA("stamina"),
    AGILITY("agility"),
    INTELLIGENCE("intelligence"),
    DEXTERITY("dexterity");

    public static final Codec<StatType> CODEC = StringRepresentable.fromEnum(StatType::values);
    // CLAMP: a bogus id from a client can never index outside the enum.
    public static final StreamCodec<ByteBuf, StatType> STREAM_CODEC = ByteBufCodecs.idMapper(
            ByIdMap.continuous(StatType::ordinal, values(), ByIdMap.OutOfBoundsStrategy.CLAMP),
            StatType::ordinal);

    private final String name;

    StatType(String name) {
        this.name = name;
    }

    @Override
    public String getSerializedName() {
        return name;
    }

    public Component displayName() {
        return Component.translatable("stat.overgearedmod." + name);
    }
}
