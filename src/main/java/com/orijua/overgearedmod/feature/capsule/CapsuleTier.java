package com.orijua.overgearedmod.feature.capsule;

import com.mojang.serialization.Codec;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.ByIdMap;
import net.minecraft.util.StringRepresentable;

import java.util.Optional;

/** The tier of a VR capsule: basic, advanced or diamond. Each one is bought on top of the previous one. */
public enum CapsuleTier implements StringRepresentable {
    BASIC("basic"),
    ADVANCED("advanced"),
    DIAMOND("diamond");

    public static final Codec<CapsuleTier> CODEC = StringRepresentable.fromEnum(CapsuleTier::values);
    public static final StreamCodec<ByteBuf, CapsuleTier> STREAM_CODEC = ByteBufCodecs.idMapper(
            ByIdMap.continuous(CapsuleTier::ordinal, values(), ByIdMap.OutOfBoundsStrategy.ZERO), CapsuleTier::ordinal);

    private final String id;

    CapsuleTier(String id) {
        this.id = id;
    }

    @Override
    public String getSerializedName() {
        return id;
    }

    /** The tier this one is built from; empty for the basic one. */
    public Optional<CapsuleTier> previous() {
        return ordinal() == 0 ? Optional.empty() : Optional.of(values()[ordinal() - 1]);
    }

    /** Whether this tier is {@code other} or a better one. */
    public boolean atLeast(CapsuleTier other) {
        return compareTo(other) >= 0;
    }

    /** The name of the tier, translated. */
    public Component tierName() {
        return Component.translatable("capsule_tier.overgearedmod." + id);
    }
}
