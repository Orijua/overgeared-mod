package com.orijua.overgearedmod.feature.notification;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.ByIdMap;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.Optional;

/**
 * One Satisfy system window: a heading chosen by {@code kind} and a few lines of text under it. A grand kind (the big
 * moments: a hidden class, a Legendary piece, a rare title) is drawn larger and longer, and {@code icon} (if any) rises
 * on the screen like a totem.
 */
public record SystemNotification(Kind kind, List<Component> lines, Optional<ItemStack> icon) {
    public enum Kind {
        LEVEL_UP("level_up", false),
        TITLE("title", false),
        QUEST("quest", false),
        CLASS("class", false),
        QUEST_DISCOVERED("quest_discovered", false),
        ITEM("item", false),
        APPRENTICE("apprentice", false),
        INSIGHT("insight", false),
        GREAT_SUCCESS("great_success", false),
        SKILL("skill", false),
        QUEST_WARNING("quest_warning", false),
        QUEST_FAILED("quest_failed", false),
        CONTEST_RESULT("contest_result", false),
        HIDDEN_CLASS("hidden_class", true),
        LEGENDARY_ITEM("legendary_item", true),
        FATIGUE_WARNING("fatigue_warning", false),
        FATIGUE_EXHAUSTED("fatigue_exhausted", false),
        RARE_TITLE("rare_title", true),
        // Always at the end: the stream codec goes by ordinal.
        TRADE("trade", false);

        // CLAMP: a bogus id can never index outside the enum.
        public static final StreamCodec<ByteBuf, Kind> STREAM_CODEC = ByteBufCodecs.idMapper(
                ByIdMap.continuous(Kind::ordinal, values(), ByIdMap.OutOfBoundsStrategy.CLAMP), Kind::ordinal);

        private final String name;
        private final boolean grand;

        Kind(String name, boolean grand) {
            this.name = name;
            this.grand = grand;
        }

        public Component heading() {
            return Component.translatable("notification.overgearedmod." + name);
        }

        /** A big moment: its own larger, longer window. */
        public boolean grand() {
            return grand;
        }
    }

    // No ItemStack is touched unless there is an icon: the queue is unit-tested without a running game.
    public SystemNotification(Kind kind, List<Component> lines) {
        this(kind, lines, Optional.empty());
    }

    /** ItemStack has no value equality, so the icon is compared by item, count and components. */
    @Override
    public boolean equals(Object other) {
        return other instanceof SystemNotification that && kind == that.kind && lines.equals(that.lines)
                && icon.isPresent() == that.icon.isPresent() && (icon.isEmpty() || ItemStack.matches(icon.get(), that.icon.get()));
    }

    @Override
    public int hashCode() {
        return 31 * kind.hashCode() + lines.hashCode();
    }

    /** Built on first use, for the same reason. */
    public static StreamCodec<RegistryFriendlyByteBuf, SystemNotification> streamCodec() {
        return StreamCodec.composite(
                Kind.STREAM_CODEC, SystemNotification::kind,
                ComponentSerialization.TRUSTED_STREAM_CODEC.apply(ByteBufCodecs.list(4)), SystemNotification::lines,
                ByteBufCodecs.optional(ItemStack.STREAM_CODEC), SystemNotification::icon,
                SystemNotification::new);
    }
}
