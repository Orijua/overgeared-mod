package com.orijua.overgearedmod.feature.economy;

import com.orijua.overgearedmod.Overgearedmod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Client → server, from the terminal: do today's shift, having seen the order {@code seen}. The server checks it is still today's. */
public record TerminalWorkPayload(int containerId, WorkOrder seen) implements CustomPacketPayload {
    public static final Type<TerminalWorkPayload> TYPE = new Type<>(Overgearedmod.id("terminal_work"));
    public static final StreamCodec<RegistryFriendlyByteBuf, TerminalWorkPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, TerminalWorkPayload::containerId,
            WorkOrder.STREAM_CODEC, TerminalWorkPayload::seen,
            TerminalWorkPayload::new);

    @Override
    public Type<TerminalWorkPayload> type() {
        return TYPE;
    }
}
