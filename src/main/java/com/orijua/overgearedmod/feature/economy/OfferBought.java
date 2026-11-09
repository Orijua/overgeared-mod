package com.orijua.overgearedmod.feature.economy;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.HashSet;
import java.util.Set;

/**
 * The positions of the daily offer a player already bought on one game day (one unit per piece, player and day).
 * Immutable. It only keeps the current day, so it never grows beyond {@link #MAX_POSITIONS}: asking about another day
 * just finds nothing.
 */
public record OfferBought(long day, Set<Integer> positions) {
    /** 12 pieces per rating at most, 6 ratings. */
    public static final int MAX_POSITIONS = 72;
    /** Never: no day has this value. */
    public static final OfferBought EMPTY = new OfferBought(Long.MIN_VALUE, Set.of());

    public static final Codec<OfferBought> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.LONG.fieldOf("day").forGetter(OfferBought::day),
            Codec.INT.listOf().xmap(Set::copyOf, java.util.List::copyOf).fieldOf("positions").forGetter(OfferBought::positions)
    ).apply(instance, OfferBought::new));

    public OfferBought {
        positions = Set.copyOf(positions);
    }

    /** Whether {@code position} was already bought on day {@code today}. */
    public boolean has(int position, long today) {
        return day == today && positions.contains(position);
    }

    /** This, plus {@code position} on day {@code today}; the positions of another day are dropped. */
    public OfferBought with(int position, long today) {
        Set<Integer> next = new HashSet<>();
        if (day == today) {
            next.addAll(positions);
        }
        if (next.size() < MAX_POSITIONS) {
            next.add(position);
        }
        return new OfferBought(today, next);
    }

    /** The positions bought on day {@code today} (empty for another day). */
    public Set<Integer> positionsOn(long today) {
        return day == today ? positions : Set.of();
    }
}
