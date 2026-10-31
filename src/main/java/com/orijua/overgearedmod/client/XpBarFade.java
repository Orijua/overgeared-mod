package com.orijua.overgearedmod.client;

/**
 * How visible the HUD's XP numbers and bar are. They fade in when XP is gained, stay {@link #SHOW_MILLIS}
 * after the <em>last</em> gain (a new gain restarts the wait, it does not add to it) and then fade out.
 * The opacity only ever moves gradually towards its target, so a gain during the fade-out turns it back
 * from where it is instead of jumping. The first values seen (joining a world) are not a gain.
 */
final class XpBarFade {
    static final long SHOW_MILLIS = 5000;
    static final long FADE_IN_MILLIS = 200;
    static final long FADE_OUT_MILLIS = 600;

    private boolean known;
    private int level;
    private int xp;
    private boolean gained;
    private long lastGain;
    private long lastUpdate;
    private float alpha;

    /** Forget everything, as for a new player: the next values seen are again not a gain. */
    void reset() {
        known = false;
        gained = false;
        alpha = 0;
    }

    /** @return the opacity for this frame, from 0 (hidden) to 1. */
    float update(long now, int level, int xp) {
        if (known && (level > this.level || level == this.level && xp > this.xp)) {
            gained = true;
            lastGain = now;
        }
        long elapsed = known ? Math.max(0, now - lastUpdate) : 0;
        known = true;
        this.level = level;
        this.xp = xp;
        lastUpdate = now;

        boolean visible = gained && now - lastGain < SHOW_MILLIS;
        alpha = visible
                ? Math.min(1, alpha + elapsed / (float) FADE_IN_MILLIS)
                : Math.max(0, alpha - elapsed / (float) FADE_OUT_MILLIS);
        return alpha;
    }
}
