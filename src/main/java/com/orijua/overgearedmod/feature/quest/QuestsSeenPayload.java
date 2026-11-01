package com.orijua.overgearedmod.feature.quest;

import com.orijua.overgearedmod.Overgearedmod;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Client → server: the player opened the Quests tab, so every discovered quest has now been seen. */
public record QuestsSeenPayload() implements CustomPacketPayload {
    public static final QuestsSeenPayload INSTANCE = new QuestsSeenPayload();
    public static final Type<QuestsSeenPayload> TYPE = new Type<>(Overgearedmod.id("quests_seen"));
    public static final StreamCodec<ByteBuf, QuestsSeenPayload> STREAM_CODEC = StreamCodec.unit(INSTANCE);

    @Override
    public Type<QuestsSeenPayload> type() {
        return TYPE;
    }
}
