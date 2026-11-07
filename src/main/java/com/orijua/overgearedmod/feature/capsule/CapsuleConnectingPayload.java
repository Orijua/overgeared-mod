package com.orijua.overgearedmod.feature.capsule;

import com.orijua.overgearedmod.Overgearedmod;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Server → client: the "Connecting…" screen starts for this many ticks, or ({@code ticks} = 0) is cancelled. */
public record CapsuleConnectingPayload(int ticks) implements CustomPacketPayload {
    public static final Type<CapsuleConnectingPayload> TYPE = new Type<>(Overgearedmod.id("capsule_connecting"));
    public static final StreamCodec<ByteBuf, CapsuleConnectingPayload> STREAM_CODEC =
            ByteBufCodecs.VAR_INT.map(CapsuleConnectingPayload::new, CapsuleConnectingPayload::ticks);

    @Override
    public Type<CapsuleConnectingPayload> type() {
        return TYPE;
    }
}
