package com.orijua.overgearedmod.feature.economy;

import com.orijua.overgearedmod.Overgearedmod;
import com.orijua.overgearedmod.feature.capsule.CapsuleTier;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Client → server, from the terminal: buy the license of {@code tier}, having seen it cost {@code seenPrice} Won. The server checks both. */
public record TerminalBuyLicensePayload(int containerId, CapsuleTier tier, long seenPrice) implements CustomPacketPayload {
    public static final Type<TerminalBuyLicensePayload> TYPE = new Type<>(Overgearedmod.id("terminal_buy_license"));
    public static final StreamCodec<RegistryFriendlyByteBuf, TerminalBuyLicensePayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, TerminalBuyLicensePayload::containerId,
            CapsuleTier.STREAM_CODEC, TerminalBuyLicensePayload::tier,
            ByteBufCodecs.VAR_LONG, TerminalBuyLicensePayload::seenPrice,
            TerminalBuyLicensePayload::new);

    @Override
    public Type<TerminalBuyLicensePayload> type() {
        return TYPE;
    }
}
