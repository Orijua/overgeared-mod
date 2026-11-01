package com.orijua.overgearedmod.feature.notification;

import com.orijua.overgearedmod.Overgearedmod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Server → client: show a Satisfy system window (level up, title, quest, class). */
public record SystemNotificationPayload(SystemNotification notification) implements CustomPacketPayload {
    public static final Type<SystemNotificationPayload> TYPE = new Type<>(Overgearedmod.id("system_notification"));
    public static final StreamCodec<RegistryFriendlyByteBuf, SystemNotificationPayload> STREAM_CODEC =
            SystemNotification.streamCodec().map(SystemNotificationPayload::new, SystemNotificationPayload::notification);

    @Override
    public Type<SystemNotificationPayload> type() {
        return TYPE;
    }
}
