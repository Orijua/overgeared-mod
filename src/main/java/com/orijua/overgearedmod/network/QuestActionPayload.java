package com.orijua.overgearedmod.network;

import com.orijua.overgearedmod.Overgearedmod;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.ByIdMap;

/** Client → server: accept a quest, claim its rewards or abandon it. */
public record QuestActionPayload(ResourceLocation quest, Action action) implements CustomPacketPayload {
    public enum Action { ACCEPT, CLAIM, ABANDON }

    private static final StreamCodec<ByteBuf, Action> ACTION_CODEC = ByteBufCodecs.idMapper(
            ByIdMap.continuous(Action::ordinal, Action.values(), ByIdMap.OutOfBoundsStrategy.CLAMP), Action::ordinal);

    public static final Type<QuestActionPayload> TYPE = new Type<>(Overgearedmod.id("quest_action"));
    public static final StreamCodec<ByteBuf, QuestActionPayload> STREAM_CODEC = StreamCodec.composite(
            ResourceLocation.STREAM_CODEC, QuestActionPayload::quest,
            ACTION_CODEC, QuestActionPayload::action,
            QuestActionPayload::new);

    @Override
    public Type<QuestActionPayload> type() {
        return TYPE;
    }
}
