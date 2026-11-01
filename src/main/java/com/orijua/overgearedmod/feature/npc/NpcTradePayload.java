package com.orijua.overgearedmod.feature.npc;

import com.orijua.overgearedmod.Overgearedmod;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Client → server: the Trade button of a merchant NPC's quest window (Smith's shop). */
public record NpcTradePayload(int npc) implements CustomPacketPayload {
    public static final Type<NpcTradePayload> TYPE = new Type<>(Overgearedmod.id("npc_trade"));
    public static final StreamCodec<ByteBuf, NpcTradePayload> STREAM_CODEC = ByteBufCodecs.VAR_INT.map(NpcTradePayload::new, NpcTradePayload::npc);

    @Override
    public Type<NpcTradePayload> type() {
        return TYPE;
    }
}
