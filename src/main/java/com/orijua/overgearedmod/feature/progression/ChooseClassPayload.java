package com.orijua.overgearedmod.feature.progression;

import com.orijua.overgearedmod.Overgearedmod;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** Client → server: pick a class (only works while the player has none). */
public record ChooseClassPayload(ResourceLocation playerClass) implements CustomPacketPayload {
    public static final Type<ChooseClassPayload> TYPE = new Type<>(Overgearedmod.id("choose_class"));
    public static final StreamCodec<ByteBuf, ChooseClassPayload> STREAM_CODEC =
            ResourceLocation.STREAM_CODEC.map(ChooseClassPayload::new, ChooseClassPayload::playerClass);

    @Override
    public Type<ChooseClassPayload> type() {
        return TYPE;
    }
}
