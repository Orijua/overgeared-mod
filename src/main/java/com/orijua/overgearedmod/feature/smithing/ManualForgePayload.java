package com.orijua.overgearedmod.feature.smithing;

import com.orijua.overgearedmod.Overgearedmod;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Client → server: the manual forging minigame ended with {@code precision} (0-1) in the anvil menu
 * {@code containerId}. The server doesn't trust it: it clamps the value and checks the timing.
 */
public record ManualForgePayload(int containerId, float precision) implements CustomPacketPayload {
    public static final Type<ManualForgePayload> TYPE = new Type<>(Overgearedmod.id("manual_forge"));
    public static final StreamCodec<ByteBuf, ManualForgePayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, ManualForgePayload::containerId,
            ByteBufCodecs.FLOAT, ManualForgePayload::precision,
            ManualForgePayload::new);

    @Override
    public Type<ManualForgePayload> type() {
        return TYPE;
    }
}
