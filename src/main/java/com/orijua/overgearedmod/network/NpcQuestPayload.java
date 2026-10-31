package com.orijua.overgearedmod.network;

import com.orijua.overgearedmod.Overgearedmod;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.ByIdMap;

/** Client → server, from an NPC's quest window: accept, deliver to or claim one of its quests. */
public record NpcQuestPayload(int npc, ResourceLocation quest, Action action) implements CustomPacketPayload {
    public enum Action { ACCEPT, DELIVER, CLAIM }

    private static final StreamCodec<ByteBuf, Action> ACTION_CODEC = ByteBufCodecs.idMapper(
            ByIdMap.continuous(Action::ordinal, Action.values(), ByIdMap.OutOfBoundsStrategy.CLAMP), Action::ordinal);

    public static final Type<NpcQuestPayload> TYPE = new Type<>(Overgearedmod.id("npc_quest"));
    public static final StreamCodec<ByteBuf, NpcQuestPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, NpcQuestPayload::npc,
            ResourceLocation.STREAM_CODEC, NpcQuestPayload::quest,
            ACTION_CODEC, NpcQuestPayload::action,
            NpcQuestPayload::new);

    @Override
    public Type<NpcQuestPayload> type() {
        return TYPE;
    }
}
