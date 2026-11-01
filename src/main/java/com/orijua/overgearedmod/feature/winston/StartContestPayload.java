package com.orijua.overgearedmod.feature.winston;

import com.orijua.overgearedmod.Overgearedmod;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Client → server: the player confirmed starting the contest against Euphemina (entity id {@code euphemina}). */
public record StartContestPayload(int euphemina) implements CustomPacketPayload {
    public static final Type<StartContestPayload> TYPE = new Type<>(Overgearedmod.id("start_contest"));
    public static final StreamCodec<ByteBuf, StartContestPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, StartContestPayload::euphemina,
            StartContestPayload::new);

    @Override
    public Type<StartContestPayload> type() {
        return TYPE;
    }
}
