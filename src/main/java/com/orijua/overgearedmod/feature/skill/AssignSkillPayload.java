package com.orijua.overgearedmod.feature.skill;

import com.orijua.overgearedmod.Overgearedmod;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.Optional;

/** Client → server: put a learned skill in a slot, or clear the slot when empty. */
public record AssignSkillPayload(int slot, Optional<ResourceLocation> skill) implements CustomPacketPayload {
    public static final Type<AssignSkillPayload> TYPE = new Type<>(Overgearedmod.id("assign_skill"));
    public static final StreamCodec<ByteBuf, AssignSkillPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, AssignSkillPayload::slot,
            ByteBufCodecs.optional(ResourceLocation.STREAM_CODEC), AssignSkillPayload::skill,
            AssignSkillPayload::new);

    @Override
    public Type<AssignSkillPayload> type() {
        return TYPE;
    }
}
