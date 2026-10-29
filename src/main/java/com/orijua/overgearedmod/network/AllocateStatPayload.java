package com.orijua.overgearedmod.network;

import com.orijua.overgearedmod.Overgearedmod;
import com.orijua.overgearedmod.progression.StatType;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Client → server: spend one free point on {@code stat}. */
public record AllocateStatPayload(StatType stat) implements CustomPacketPayload {
    public static final Type<AllocateStatPayload> TYPE = new Type<>(Overgearedmod.id("allocate_stat"));
    public static final StreamCodec<ByteBuf, AllocateStatPayload> STREAM_CODEC =
            StatType.STREAM_CODEC.map(AllocateStatPayload::new, AllocateStatPayload::stat);

    @Override
    public Type<AllocateStatPayload> type() {
        return TYPE;
    }
}
