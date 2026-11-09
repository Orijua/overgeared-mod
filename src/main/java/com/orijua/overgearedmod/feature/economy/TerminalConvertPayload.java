package com.orijua.overgearedmod.feature.economy;

import com.orijua.overgearedmod.Overgearedmod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Client → server, from the terminal: convert {@code amount} stored emeralds, having seen {@code seenStored} stored. The server checks both. */
public record TerminalConvertPayload(int containerId, int amount, int seenStored) implements CustomPacketPayload {
    public static final Type<TerminalConvertPayload> TYPE = new Type<>(Overgearedmod.id("terminal_convert"));
    public static final StreamCodec<RegistryFriendlyByteBuf, TerminalConvertPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, TerminalConvertPayload::containerId,
            ByteBufCodecs.VAR_INT, TerminalConvertPayload::amount,
            ByteBufCodecs.VAR_INT, TerminalConvertPayload::seenStored,
            TerminalConvertPayload::new);

    @Override
    public Type<TerminalConvertPayload> type() {
        return TYPE;
    }
}
