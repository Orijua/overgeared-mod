package com.orijua.overgearedmod.feature.progression.client;

import java.util.function.IntUnaryOperator;

/**
 * The HUD's feedback for gaining XP: a "+N XP" that rises and fades, and a bar that fills gradually.
 * Gains in a row add to the same "+N XP" while it is still shown (it stays {@link #HOLD_MILLIS} after the
 * <em>last</em> gain, then fades); once it is gone the next gain starts a new count. The bar position is
 * kept in level units (level + fraction), so a level-up fills the bar, wraps and goes on filling. The part
 * gained in the current count is the glowing part. Losing XP (death, commands) moves the bar at once.
 */
final class XpGainFeedback {
    static final long FADE_IN_MILLIS = 100;
    static final long RISE_MILLIS = 400;
    static final long HOLD_MILLIS = 1500;
    static final long FADE_OUT_MILLIS = 500;
    /** How fast the bar catches up: each this many milliseconds it covers about two thirds of what's left. */
    static final double FILL_MILLIS = 250;
    private static final double SNAP = 1e-3;

    private final IntUnaryOperator xpToNextLevel;
    private boolean known;
    private int level;
    private int xp;
    private long lastUpdate;
    private boolean counting;
    private long amount;
    private long countStart;
    private long lastGain;
    private double shown;
    private double glowFrom;

    /** @param xpToNextLevel the XP a level needs to reach the next one */
    XpGainFeedback(IntUnaryOperator xpToNextLevel) {
        this.xpToNextLevel = xpToNextLevel;
    }

    /** Forget everything, as for a new player: the next values seen are again not a gain. */
    void reset() {
        known = false;
        counting = false;
        amount = 0;
    }

    void update(long now, int level, int xp) {
        double target = level + xp / (double) Math.max(1, xpToNextLevel.applyAsInt(level));
        if (!known) {
            shown = target;
        } else {
            long gained = gained(this.level, this.xp, level, xp);
            if (counting && now - lastGain >= HOLD_MILLIS + FADE_OUT_MILLIS) {
                counting = false;
                amount = 0;
            }
            if (gained > 0) {
                if (!counting) {
                    counting = true;
                    countStart = now;
                    glowFrom = shown;
                }
                amount += gained;
                lastGain = now;
            }
            if (target < shown) {
                shown = target;
                glowFrom = Math.min(glowFrom, target);
            } else {
                long elapsed = Math.max(0, now - lastUpdate);
                shown += (target - shown) * (1 - Math.exp(-elapsed / FILL_MILLIS));
                if (target - shown < SNAP) {
                    shown = target;
                }
            }
        }
        known = true;
        this.level = level;
        this.xp = xp;
        lastUpdate = now;
    }

    /** XP gained going from one level and XP to another; 0 when it went down. */
    private long gained(int fromLevel, int fromXp, int toLevel, int toXp) {
        if (toLevel < fromLevel || toLevel == fromLevel && toXp <= fromXp) {
            return 0;
        }
        if (toLevel == fromLevel) {
            return toXp - fromXp;
        }
        long total = xpToNextLevel.applyAsInt(fromLevel) - fromXp;
        for (int l = fromLevel + 1; l < toLevel; l++) {
            total += xpToNextLevel.applyAsInt(l);
        }
        return total + toXp;
    }

    /** XP gained in the current count, shown as "+N XP"; 0 when there is none. */
    long amount() {
        return counting ? amount : 0;
    }

    /** @return how visible the "+N XP" is, from 0 to 1 */
    float popupAlpha(long now) {
        if (!counting) {
            return 0;
        }
        float in = Math.min(1, (now - countStart) / (float) FADE_IN_MILLIS);
        float out = 1 - Math.max(0, now - lastGain - HOLD_MILLIS) / (float) FADE_OUT_MILLIS;
        return Math.clamp(Math.min(in, out), 0, 1);
    }

    /** @return how far the "+N XP" has risen since the count started, from 0 to 1 */
    float rise(long now) {
        return counting ? Math.clamp((now - countStart) / (float) RISE_MILLIS, 0, 1) : 0;
    }

    /** How full the bar is drawn, from 0 to 1. */
    float barFill() {
        return (float) (shown - Math.floor(shown));
    }

    /** Where the glowing part of the bar starts, from 0 to {@link #barFill()}: what was there before this count. */
    float glowStart() {
        if (Math.floor(glowFrom) < Math.floor(shown)) {
            return 0;
        }
        return (float) Math.min(glowFrom - Math.floor(glowFrom), barFill());
    }

    /** @return how strong the glow is, from 0 to 1: full while filling, then fading with the "+N XP" */
    float glow(long now) {
        if (!counting) {
            return 0;
        }
        double target = level + xp / (double) Math.max(1, xpToNextLevel.applyAsInt(level));
        return shown < target ? 1 : popupAlpha(now);
    }
}
