package com.orijua.overgearedmod.feature.economy;

import com.orijua.overgearedmod.Overgearedmod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Client → server, from the terminal: pay {@code amount} Won of the debt, having seen it at {@code seenDebt}. The server checks both. */
public record TerminalPayDebtPayload(int containerId, long amount, long seenDebt) implements CustomPacketPayload {
    public static final Type<TerminalPayDebtPayload> TYPE = new Type<>(Overgearedmod.id("terminal_pay_debt"));
    public static final StreamCodec<RegistryFriendlyByteBuf, TerminalPayDebtPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, TerminalPayDebtPayload::containerId,
            ByteBufCodecs.VAR_LONG, TerminalPayDebtPayload::amount,
            ByteBufCodecs.VAR_LONG, TerminalPayDebtPayload::seenDebt,
            TerminalPayDebtPayload::new);

    @Override
    public Type<TerminalPayDebtPayload> type() {
        return TYPE;
    }
}
