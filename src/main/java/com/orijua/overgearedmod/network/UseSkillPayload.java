package com.orijua.overgearedmod.network;

import com.orijua.overgearedmod.Overgearedmod;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Client → server: the key of skill slot {@code slot} (0-2) was pressed. */
public record UseSkillPayload(int slot) implements CustomPacketPayload {
    public static final Type<UseSkillPayload> TYPE = new Type<>(Overgearedmod.id("use_skill"));
    public static final StreamCodec<ByteBuf, UseSkillPayload> STREAM_CODEC =
            ByteBufCodecs.VAR_INT.map(UseSkillPayload::new, UseSkillPayload::slot);

    @Override
    public Type<UseSkillPayload> type() {
        return TYPE;
    }
}
