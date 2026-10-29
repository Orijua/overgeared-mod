package com.orijua.overgearedmod.network;

import com.orijua.overgearedmod.Overgearedmod;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Client → server: "the skill key was pressed". */
public record UseSkillPayload() implements CustomPacketPayload {
    public static final UseSkillPayload INSTANCE = new UseSkillPayload();
    public static final Type<UseSkillPayload> TYPE = new Type<>(Overgearedmod.id("use_skill"));
    public static final StreamCodec<ByteBuf, UseSkillPayload> STREAM_CODEC = StreamCodec.unit(INSTANCE);

    @Override
    public Type<UseSkillPayload> type() {
        return TYPE;
    }
}
