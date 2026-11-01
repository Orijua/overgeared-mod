package com.orijua.overgearedmod.feature.progression;

import com.orijua.overgearedmod.Overgearedmod;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Client → server: spend {@code amount} free points on {@code stat} (1, or 10 / 100 with Shift / Control). */
public record AllocateStatPayload(StatType stat, int amount) implements CustomPacketPayload {
    public static final Type<AllocateStatPayload> TYPE = new Type<>(Overgearedmod.id("allocate_stat"));
    public static final StreamCodec<ByteBuf, AllocateStatPayload> STREAM_CODEC = StreamCodec.composite(
            StatType.STREAM_CODEC, AllocateStatPayload::stat,
            ByteBufCodecs.VAR_INT, AllocateStatPayload::amount,
            AllocateStatPayload::new);

    @Override
    public Type<AllocateStatPayload> type() {
        return TYPE;
    }
}
