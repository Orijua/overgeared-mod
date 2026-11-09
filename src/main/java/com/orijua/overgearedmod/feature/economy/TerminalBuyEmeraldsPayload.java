package com.orijua.overgearedmod.feature.economy;

import com.orijua.overgearedmod.Overgearedmod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Client → server, from the terminal: buy {@code amount} Satisfy emeralds, having seen them cost {@code seenUnitPrice} Won each. The server checks both. */
public record TerminalBuyEmeraldsPayload(int containerId, int amount, int seenUnitPrice) implements CustomPacketPayload {
    public static final Type<TerminalBuyEmeraldsPayload> TYPE = new Type<>(Overgearedmod.id("terminal_buy_emeralds"));
    public static final StreamCodec<RegistryFriendlyByteBuf, TerminalBuyEmeraldsPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, TerminalBuyEmeraldsPayload::containerId,
            ByteBufCodecs.VAR_INT, TerminalBuyEmeraldsPayload::amount,
            ByteBufCodecs.VAR_INT, TerminalBuyEmeraldsPayload::seenUnitPrice,
            TerminalBuyEmeraldsPayload::new);

    @Override
    public Type<TerminalBuyEmeraldsPayload> type() {
        return TYPE;
    }
}
