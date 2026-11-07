package com.orijua.overgearedmod.feature.progression.client;

import java.util.function.IntUnaryOperator;

/**
 * The HUD's feedback for gaining XP: a "+N XP" that rises and fades, and a bar that fills gradually.
 * Gains in a row add to the same "+N XP" while it is still shown (it stays {@link #HOLD_MILLIS} after the
 * <em>last</em> gain, then fades); once it is gone the next gain starts a new count. The bar position is
 * kept in level units (level + fraction), so a level-up fills the bar, wraps and goes on filling. The part
 * gained in the current count is the glowing part. Losing XP (death, commands) is the same thing mirrored:
 * the bar drains gradually, a red "-N XP" sinks and fades, and the part just lost shows in red above the bar's
 * end. A gain cancels the loss count and the other way round, so only one of the two is shown at a time.
 */
final class XpGainFeedback {
    /** After joining or respawning, values this soon are the new player's data arriving, not gains or losses. */
    static final long SETTLE_MILLIS = 2000;
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
    private boolean losing;
    private long lost;
    private long lossStart;
    private long lastLoss;
    private double lossFrom;
    private long settleUntil;

    /** @param xpToNextLevel the XP a level needs to reach the next one */
    XpGainFeedback(IntUnaryOperator xpToNextLevel) {
        this.xpToNextLevel = xpToNextLevel;
    }

    /** Forget everything, as for a new player: the next values seen are again not a gain. */
    void reset() {
        known = false;
        counting = false;
        amount = 0;
        losing = false;
        lost = 0;
    }

    /**
     * For a new player (joining, respawning): forget everything and, for {@link #SETTLE_MILLIS}, take what is seen
     * as the starting point. The new player's data arrives a moment after the player does, so without this the
     * bar would climb from level 1 to the real level as if it had just been won.
     */
    void settle(long now) {
        reset();
        settleUntil = now + SETTLE_MILLIS;
    }

    void update(long now, int level, int xp) {
        double target = level + xp / (double) Math.max(1, xpToNextLevel.applyAsInt(level));
        if (!known || now < settleUntil) {
            shown = target;
            counting = false;
            amount = 0;
            losing = false;
            lost = 0;
        } else {
            long gained = gained(this.level, this.xp, level, xp);
            if (counting && now - lastGain >= HOLD_MILLIS + FADE_OUT_MILLIS) {
                counting = false;
                amount = 0;
            }
            long drained = gained(level, xp, this.level, this.xp);
            if (losing && now - lastLoss >= HOLD_MILLIS + FADE_OUT_MILLIS) {
                losing = false;
                lost = 0;
            }
            if (gained > 0) {
                losing = false;
                lost = 0;
                if (!counting) {
                    counting = true;
                    countStart = now;
                    glowFrom = shown;
                }
                amount += gained;
                lastGain = now;
            } else if (drained > 0) {
                counting = false;
                amount = 0;
                if (!losing) {
                    losing = true;
                    lossStart = now;
                    lossFrom = shown;
                }
                lost += drained;
                lastLoss = now;
            }
            long elapsed = Math.max(0, now - lastUpdate);
            shown += (target - shown) * (1 - Math.exp(-elapsed / FILL_MILLIS));
            if (Math.abs(target - shown) < SNAP) {
                shown = target;
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

    /** XP lost in the current count, shown as "-N XP"; 0 when there is none. */
    long lostAmount() {
        return losing ? lost : 0;
    }

    /** @return how visible the "-N XP" and the red lost part are, from 0 to 1 */
    float lossAlpha(long now) {
        if (!losing) {
            return 0;
        }
        float in = Math.min(1, (now - lossStart) / (float) FADE_IN_MILLIS);
        float out = 1 - Math.max(0, now - lastLoss - HOLD_MILLIS) / (float) FADE_OUT_MILLIS;
        return Math.clamp(Math.min(in, out), 0, 1);
    }

    /** @return how far the "-N XP" has sunk since the count started, from 0 to 1 */
    float sink(long now) {
        return losing ? Math.clamp((now - lossStart) / (float) RISE_MILLIS, 0, 1) : 0;
    }

    /** Where the red lost part of the bar ends, from {@link #barFill()} to 1: where the bar was before this count. */
    float lossEnd() {
        if (Math.floor(lossFrom) > Math.floor(shown)) {
            return 1;
        }
        return (float) Math.max(lossFrom - Math.floor(lossFrom), barFill());
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
