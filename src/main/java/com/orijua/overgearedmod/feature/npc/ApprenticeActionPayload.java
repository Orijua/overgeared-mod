package com.orijua.overgearedmod.feature.npc;

import com.orijua.overgearedmod.Overgearedmod;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.util.ByIdMap;

/** Client → server, from an apprentice's window: one of its buttons. */
public record ApprenticeActionPayload(int apprentice, Action action) implements CustomPacketPayload {
    public enum Action { PAY_DAY, CANCEL_ORDER, ASSIGN_MATERIALS, ASSIGN_OUTPUT, CLEAR_CONTAINERS, DISMISS, GIVE_HAMMER, TAKE_HAMMER }

    private static final StreamCodec<ByteBuf, Action> ACTION_CODEC = ByteBufCodecs.idMapper(
            ByIdMap.continuous(Action::ordinal, Action.values(), ByIdMap.OutOfBoundsStrategy.CLAMP), Action::ordinal);

    public static final Type<ApprenticeActionPayload> TYPE = new Type<>(Overgearedmod.id("apprentice_action"));
    public static final StreamCodec<ByteBuf, ApprenticeActionPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, ApprenticeActionPayload::apprentice,
            ACTION_CODEC, ApprenticeActionPayload::action,
            ApprenticeActionPayload::new);

    @Override
    public Type<ApprenticeActionPayload> type() {
        return TYPE;
    }
}
