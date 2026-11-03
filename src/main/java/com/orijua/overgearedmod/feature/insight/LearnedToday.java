package com.orijua.overgearedmod.feature.insight;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.UUIDUtil;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * The targets a player already learned from on one Minecraft day (see {@link Observation}). Immutable. It only keeps the
 * current day, so it never grows beyond {@link #MAX_TARGETS}: asking about another day just finds nothing.
 */
public record LearnedToday(long day, Set<UUID> targets) {
    public static final int MAX_TARGETS = 256;
    public static final LearnedToday EMPTY = new LearnedToday(0, Set.of());

    public static final Codec<LearnedToday> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.LONG.fieldOf("day").forGetter(LearnedToday::day),
            UUIDUtil.CODEC.listOf().xmap(Set::copyOf, List::copyOf).fieldOf("targets").forGetter(LearnedToday::targets)
    ).apply(instance, LearnedToday::new));

    /** Whether {@code target} was already learned from on day {@code today}. */
    public boolean has(UUID target, long today) {
        return day == today && targets.contains(target);
    }

    /** This, plus {@code target} on day {@code today}; the targets of another day are dropped. */
    public LearnedToday with(UUID target, long today) {
        Set<UUID> next = new HashSet<>();
        if (day == today) {
            next.addAll(targets);
        }
        if (next.size() < MAX_TARGETS) {
            next.add(target);
        }
        return new LearnedToday(today, Set.copyOf(next));
    }
}
