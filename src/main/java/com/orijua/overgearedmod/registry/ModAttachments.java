package com.orijua.overgearedmod.registry;

import com.mojang.serialization.Codec;
import com.orijua.overgearedmod.Overgearedmod;
import com.orijua.overgearedmod.goal.PlayerCounters;
import com.orijua.overgearedmod.progression.PlayerIdentity;
import com.orijua.overgearedmod.progression.PlayerStats;
import com.orijua.overgearedmod.quest.PlayerQuests;
import com.orijua.overgearedmod.skill.PlayerSkills;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.function.Supplier;

/**
 * Per-player state, one attachment per system. Saved with the player and synced to its owner
 * on every {@code setData}, so client screens just read them.
 */
public final class ModAttachments {
    public static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES =
            DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, Overgearedmod.MODID);

    public static final Supplier<AttachmentType<PlayerStats>> PLAYER_STATS =
            playerData("player_stats", () -> PlayerStats.DEFAULT, PlayerStats.CODEC, PlayerStats.STREAM_CODEC, true);

    public static final Supplier<AttachmentType<PlayerSkills>> PLAYER_SKILLS =
            playerData("player_skills", () -> PlayerSkills.EMPTY, PlayerSkills.CODEC, PlayerSkills.STREAM_CODEC, true);

    public static final Supplier<AttachmentType<Float>> MANA =
            playerData("mana", () -> 20.0f, Codec.FLOAT, ByteBufCodecs.FLOAT, false);

    public static final Supplier<AttachmentType<PlayerIdentity>> PLAYER_IDENTITY =
            playerData("player_identity", () -> PlayerIdentity.DEFAULT, PlayerIdentity.CODEC, PlayerIdentity.STREAM_CODEC, true);

    public static final Supplier<AttachmentType<PlayerCounters>> PLAYER_COUNTERS =
            playerData("player_counters", () -> PlayerCounters.EMPTY, PlayerCounters.CODEC, PlayerCounters.STREAM_CODEC, true);

    public static final Supplier<AttachmentType<PlayerQuests>> PLAYER_QUESTS =
            playerData("player_quests", () -> PlayerQuests.EMPTY, PlayerQuests.CODEC, PlayerQuests.STREAM_CODEC, true);

    private ModAttachments() {
    }

    /** Back to defaults for every system (used by /satisfy reset). */
    public static void resetAll(Player player) {
        ATTACHMENT_TYPES.getEntries().forEach(type -> player.removeData(type.get()));
    }

    private static <T> Supplier<AttachmentType<T>> playerData(String name, Supplier<T> defaultValue, Codec<T> codec,
                                                             StreamCodec<ByteBuf, T> streamCodec, boolean keepOnDeath) {
        return ATTACHMENT_TYPES.register(name, () -> {
            AttachmentType.Builder<T> builder = AttachmentType.builder(defaultValue)
                    .serialize(codec)
                    .sync((holder, to) -> holder == to, streamCodec);
            if (keepOnDeath) {
                builder.copyOnDeath();
            }
            return builder.build();
        });
    }
}
