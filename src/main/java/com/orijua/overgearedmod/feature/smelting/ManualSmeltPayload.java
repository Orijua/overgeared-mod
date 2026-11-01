package com.orijua.overgearedmod.feature.smelting;

import com.orijua.overgearedmod.Overgearedmod;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Client → server: the bellows minigame of a manual melt ended with this precision (0-1) in the smelter menu. */
public record ManualSmeltPayload(int containerId, float precision) implements CustomPacketPayload {
    public static final Type<ManualSmeltPayload> TYPE = new Type<>(Overgearedmod.id("manual_smelt"));
    public static final StreamCodec<ByteBuf, ManualSmeltPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, ManualSmeltPayload::containerId,
            ByteBufCodecs.FLOAT, ManualSmeltPayload::precision,
            ManualSmeltPayload::new);

    @Override
    public Type<ManualSmeltPayload> type() {
        return TYPE;
    }
}
