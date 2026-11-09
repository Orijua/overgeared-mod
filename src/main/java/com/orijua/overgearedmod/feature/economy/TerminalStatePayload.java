package com.orijua.overgearedmod.feature.economy;

import com.orijua.overgearedmod.Overgearedmod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Server → client: the terminal's state for the open menu {@code containerId}. */
public record TerminalStatePayload(int containerId, TerminalState state) implements CustomPacketPayload {
    public static final Type<TerminalStatePayload> TYPE = new Type<>(Overgearedmod.id("terminal_state"));
    public static final StreamCodec<RegistryFriendlyByteBuf, TerminalStatePayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, TerminalStatePayload::containerId,
            TerminalState.STREAM_CODEC, TerminalStatePayload::state,
            TerminalStatePayload::new);

    @Override
    public Type<TerminalStatePayload> type() {
        return TYPE;
    }
}
