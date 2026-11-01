package com.orijua.overgearedmod.feature.winston;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * One player's contest against Euphemina: when it began (game time; {@link #NOT_STARTED} while not competing), the
 * score she rolled for it (kept hidden until the end), and the game day the player last lost it (negative if never), to
 * allow a rematch only from the next day.
 */
public record ContestState(long startedAt, int rivalScore, long lostDay) {
    public static final long NOT_STARTED = Long.MIN_VALUE;
    public static final ContestState NONE = new ContestState(NOT_STARTED, 0, -1);

    public static final Codec<ContestState> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.LONG.fieldOf("started_at").forGetter(ContestState::startedAt),
            Codec.INT.fieldOf("rival_score").forGetter(ContestState::rivalScore),
            Codec.LONG.fieldOf("lost_day").forGetter(ContestState::lostDay)
    ).apply(instance, ContestState::new));
    public static final StreamCodec<ByteBuf, ContestState> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_LONG, ContestState::startedAt,
            ByteBufCodecs.VAR_INT, ContestState::rivalScore,
            ByteBufCodecs.VAR_LONG, ContestState::lostDay,
            ContestState::new);

    public boolean running() {
        return startedAt != NOT_STARTED;
    }

    public ContestState start(long now, int rival) {
        return new ContestState(now, rival, lostDay);
    }

    /** The contest is over and lost on {@code day}. */
    public ContestState lost(long day) {
        return new ContestState(NOT_STARTED, 0, day);
    }
}
