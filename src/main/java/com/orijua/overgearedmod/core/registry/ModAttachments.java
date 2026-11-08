package com.orijua.overgearedmod.core.registry;

import com.mojang.serialization.Codec;
import com.orijua.overgearedmod.Overgearedmod;
import com.orijua.overgearedmod.feature.capsule.CapsuleState;
import com.orijua.overgearedmod.feature.insight.LearnedToday;
import com.orijua.overgearedmod.feature.mob.MobLevel;
import com.orijua.overgearedmod.feature.npc.WorkshopEntry;
import com.orijua.overgearedmod.feature.progression.PlayerIdentity;
import com.orijua.overgearedmod.feature.progression.PlayerStats;
import com.orijua.overgearedmod.feature.quest.PlayerQuests;
import com.orijua.overgearedmod.feature.quest.goal.PlayerCounters;
import com.orijua.overgearedmod.feature.skill.PlayerSkills;
import com.orijua.overgearedmod.feature.winston.ContestState;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import com.orijua.overgearedmod.feature.character.CharacterVault;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.List;
import java.util.UUID;
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

    /**
     * The energy of the player's body, 1 (rested) to 0 (spent): drains in Satisfy, refills in the real world (see
     * fatigue.FatigueService). One value for the player, not swapped with the characters; kept on death.
     */
    public static final Supplier<AttachmentType<Float>> FATIGUE =
            playerData("fatigue", () -> 1.0f, Codec.FLOAT, ByteBufCodecs.FLOAT, true);

    /** Seconds left of the grace period after running out of energy, or -1 when it is not running. Saved (quitting does not stretch it) and synced to its owner, for the countdown on screen. */
    public static final Supplier<AttachmentType<Integer>> FATIGUE_GRACE =
            playerData("fatigue_grace", () -> -1, Codec.INT, ByteBufCodecs.VAR_INT, true);

    /** The last disconnection was forced by the fatigue: the next connection starts with a few seconds of protection. */
    public static final Supplier<AttachmentType<Boolean>> FATIGUE_FORCED_OUT =
            serverData("fatigue_forced_out", () -> false, Codec.BOOL);

    /** Wall-clock milliseconds when the player last left the server, or 0: lets the real world refill the energy while they are away. */
    public static final Supplier<AttachmentType<Long>> FATIGUE_AWAY_SINCE =
            serverData("fatigue_away_since", () -> 0L, Codec.LONG);

    public static final Supplier<AttachmentType<PlayerIdentity>> PLAYER_IDENTITY =
            playerData("player_identity", () -> PlayerIdentity.DEFAULT, PlayerIdentity.CODEC, PlayerIdentity.STREAM_CODEC, true);

    public static final Supplier<AttachmentType<PlayerCounters>> PLAYER_COUNTERS =
            playerData("player_counters", () -> PlayerCounters.EMPTY, PlayerCounters.CODEC, PlayerCounters.STREAM_CODEC, true);

    public static final Supplier<AttachmentType<PlayerQuests>> PLAYER_QUESTS =
            playerData("player_quests", () -> PlayerQuests.EMPTY, PlayerQuests.CODEC, PlayerQuests.STREAM_CODEC, true);

    /** The apprentice blacksmiths a player has hired (their UUIDs), to know how many more they may hire. */
    public static final Supplier<AttachmentType<List<UUID>>> APPRENTICES =
            playerData("apprentices", List::of, UUIDUtil.CODEC.listOf(), ByteBufCodecs.fromCodec(UUIDUtil.CODEC.listOf()), true);

    /** What the Workshop tab shows of each apprentice (see npc.Workshop). */
    public static final Supplier<AttachmentType<List<WorkshopEntry>>> WORKSHOP =
            playerData("workshop", List::of, WorkshopEntry.CODEC.listOf(), ByteBufCodecs.fromCodec(WorkshopEntry.CODEC.listOf()), true);

    /** Insight (Perspicacia): not allocated, it grows by observing (see insight.Observation). */
    public static final Supplier<AttachmentType<Integer>> INSIGHT =
            playerData("insight", () -> 0, Codec.INT, ByteBufCodecs.VAR_INT, true);

    /** Targets the player already learned from today (see insight.Observation). Server-only: the client never reads it. */
    public static final Supplier<AttachmentType<LearnedToday>> INSIGHT_LEARNED =
            serverData("insight_learned", () -> LearnedToday.EMPTY, LearnedToday.CODEC);

    /** The villagers whose talent the player has found with Talent Search, for life (see insight.TalentSearch). Server-only. */
    public static final Supplier<AttachmentType<List<UUID>>> TALENTS_FOUND =
            serverData("talents_found", List::of, UUIDUtil.CODEC.listOf());

    /** The rumors the player has heard from NPCs, oldest first (the Journal's Rumors section; see rumor.Rumors). */
    public static final Supplier<AttachmentType<List<ResourceLocation>>> HEARD_RUMORS =
            playerData("heard_rumors", List::of, ResourceLocation.CODEC.listOf(), ByteBufCodecs.fromCodec(ResourceLocation.CODEC.listOf()), true);

    /** The production contest against Euphemina in Winston (see winston.KhanContest). */
    public static final Supplier<AttachmentType<ContestState>> KHAN_CONTEST =
            playerData("khan_contest", () -> ContestState.NONE, ContestState.CODEC, ContestState.STREAM_CODEC, true);

    /** The VR capsule: where the player's body is while they are in Satisfy and where they last disconnected there. Server-only. */
    public static final Supplier<AttachmentType<CapsuleState>> CAPSULE =
            serverData("capsule", () -> CapsuleState.EMPTY, CapsuleState.CODEC);

    /** Whether the player already got their starter capsule (see capsule.StarterCapsule). Server-only, kept on death. */
    public static final Supplier<AttachmentType<Boolean>> STARTER_CAPSULE_GIVEN =
            serverData("starter_capsule_given", () -> false, Codec.BOOL);

    /**
     * The two characters: which world's things the player carries and the snapshot of the other's (see
     * character.CharacterVault). Server-only, kept on death, and NOT cleared by {@link #resetAll}: they are the
     * player's belongings, not progress.
     */
    public static final Supplier<AttachmentType<CharacterVault>> CHARACTER_VAULT =
            serverData("character_vault", () -> CharacterVault.EMPTY, CharacterVault.CODEC);

    /** On mobs (not players): true if it came from a spawner. Saved with the mob, never synced. */
    public static final Supplier<AttachmentType<Boolean>> FROM_SPAWNER = ATTACHMENT_TYPES.register("from_spawner",
            () -> AttachmentType.builder(() -> false).serialize(Codec.BOOL).build());

    /** On hostile mobs: level and elite flag. Saved with the mob and synced to every client that sees it (for the name tag). */
    public static final Supplier<AttachmentType<MobLevel>> MOB_LEVEL = ATTACHMENT_TYPES.register("mob_level",
            () -> AttachmentType.builder(() -> MobLevel.NONE).serialize(MobLevel.CODEC).sync(MobLevel.STREAM_CODEC).build());

    private ModAttachments() {
    }

    /** Back to defaults for every system (used by /satisfy debug reset all). */
    public static void resetAll(Player player) {
        ATTACHMENT_TYPES.getEntries().stream()
                .filter(type -> type.get() != CHARACTER_VAULT.get())
                .forEach(type -> player.removeData(type.get()));
    }

    /** Saved with the player and kept on death, but never synced: state only the server needs. */
    private static <T> Supplier<AttachmentType<T>> serverData(String name, Supplier<T> defaultValue, Codec<T> codec) {
        return ATTACHMENT_TYPES.register(name, () -> AttachmentType.builder(defaultValue)
                .serialize(codec)
                .copyOnDeath()
                .build());
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
