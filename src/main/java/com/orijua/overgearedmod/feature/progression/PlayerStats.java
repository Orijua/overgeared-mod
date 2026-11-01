package com.orijua.overgearedmod.feature.progression;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * Persistent Satisfy progression of one player. Immutable: every change returns a new copy,
 * which is then stored with {@code player.setData(...)} (that call also syncs it to the client).
 */
public record PlayerStats(
        int level,
        int xp,
        int freePoints,
        int strength,
        int stamina,
        int agility,
        int intelligence,
        int dexterity,
        int smithingXp) {

    public static final PlayerStats DEFAULT = new PlayerStats(1, 0, 0, 0, 0, 0, 0, 0, 0);

    public static final Codec<PlayerStats> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.INT.fieldOf("level").forGetter(PlayerStats::level),
            Codec.INT.fieldOf("xp").forGetter(PlayerStats::xp),
            Codec.INT.fieldOf("free_points").forGetter(PlayerStats::freePoints),
            Codec.INT.fieldOf("strength").forGetter(PlayerStats::strength),
            Codec.INT.fieldOf("stamina").forGetter(PlayerStats::stamina),
            Codec.INT.fieldOf("agility").forGetter(PlayerStats::agility),
            Codec.INT.fieldOf("intelligence").forGetter(PlayerStats::intelligence),
            Codec.INT.fieldOf("dexterity").forGetter(PlayerStats::dexterity),
            Codec.INT.optionalFieldOf("smithing_xp", 0).forGetter(PlayerStats::smithingXp)
    ).apply(instance, PlayerStats::new));

    // Simple approach for a template: reuse the disk codec for the network.
    public static final StreamCodec<ByteBuf, PlayerStats> STREAM_CODEC = ByteBufCodecs.fromCodec(CODEC);

    public int get(StatType stat) {
        return switch (stat) {
            case STRENGTH -> strength;
            case STAMINA -> stamina;
            case AGILITY -> agility;
            case INTELLIGENCE -> intelligence;
            case DEXTERITY -> dexterity;
        };
    }

    public PlayerStats with(StatType stat, int value) {
        return new PlayerStats(level, xp, freePoints,
                stat == StatType.STRENGTH ? value : strength,
                stat == StatType.STAMINA ? value : stamina,
                stat == StatType.AGILITY ? value : agility,
                stat == StatType.INTELLIGENCE ? value : intelligence,
                stat == StatType.DEXTERITY ? value : dexterity,
                smithingXp);
    }

    public PlayerStats withProgress(int level, int xp, int freePoints) {
        return new PlayerStats(level, xp, freePoints, strength, stamina, agility, intelligence, dexterity, smithingXp);
    }

    public PlayerStats withSmithingXp(int smithingXp) {
        return new PlayerStats(level, xp, freePoints, strength, stamina, agility, intelligence, dexterity, smithingXp);
    }

    public int smithingLevel() {
        return ProgressionRules.smithingLevel(smithingXp);
    }
}
