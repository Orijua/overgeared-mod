package com.orijua.overgearedmod.equipment.option;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

/** One option on an item: which definition and the value it rolled. Stored by id, so removed options are just ignored. */
public record RolledOption(ResourceLocation option, float value) {
    public static final Codec<RolledOption> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ResourceLocation.CODEC.fieldOf("option").forGetter(RolledOption::option),
            Codec.FLOAT.fieldOf("value").forGetter(RolledOption::value)
    ).apply(instance, RolledOption::new));

    public static final Codec<List<RolledOption>> LIST_CODEC = CODEC.listOf();

    public static final StreamCodec<ByteBuf, List<RolledOption>> LIST_STREAM_CODEC = StreamCodec.composite(
            ResourceLocation.STREAM_CODEC, RolledOption::option,
            ByteBufCodecs.FLOAT, RolledOption::value,
            RolledOption::new).apply(ByteBufCodecs.list());
}
