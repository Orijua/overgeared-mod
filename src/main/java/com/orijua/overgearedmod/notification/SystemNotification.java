package com.orijua.overgearedmod.notification;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.ByIdMap;

import java.util.List;

/** One Satisfy system window: a heading chosen by {@code kind} and a few lines of text under it. */
public record SystemNotification(Kind kind, List<Component> lines) {
    public enum Kind {
        LEVEL_UP("level_up"),
        TITLE("title"),
        QUEST("quest"),
        CLASS("class"),
        QUEST_DISCOVERED("quest_discovered");

        // CLAMP: a bogus id can never index outside the enum.
        public static final StreamCodec<ByteBuf, Kind> STREAM_CODEC = ByteBufCodecs.idMapper(
                ByIdMap.continuous(Kind::ordinal, values(), ByIdMap.OutOfBoundsStrategy.CLAMP), Kind::ordinal);

        private final String name;

        Kind(String name) {
            this.name = name;
        }

        public Component heading() {
            return Component.translatable("notification.overgearedmod." + name);
        }
    }

    public static final StreamCodec<RegistryFriendlyByteBuf, SystemNotification> STREAM_CODEC = StreamCodec.composite(
            Kind.STREAM_CODEC, SystemNotification::kind,
            ComponentSerialization.TRUSTED_STREAM_CODEC.apply(ByteBufCodecs.list(4)), SystemNotification::lines,
            SystemNotification::new);
}
