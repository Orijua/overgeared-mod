package com.orijua.overgearedmod.feature.capsule;

import java.util.OptionalInt;
import java.util.OptionalLong;

/** The pure rules of {@code /satisfy logout} (own design: 3 seconds of countdown, refused for 3 seconds after any combat). */
public final class LogoutRules {
    /** The countdown lasts 3 seconds. */
    public static final int COUNTDOWN_TICKS = 60;
    /** Damage dealt or taken in the last 3 seconds counts as danger. */
    public static final int DANGER_TICKS = 60;
    public static final int TICKS_PER_SECOND = 20;

    private LogoutRules() {
    }

    /** Whether there was combat in the last {@link #DANGER_TICKS}. A tick in the future (clock went back) counts as danger. */
    public static boolean inDanger(long now, OptionalLong lastCombat) {
        return lastCombat.isPresent() && now - lastCombat.getAsLong() < DANGER_TICKS;
    }

    /** The seconds to announce when {@code ticksLeft} is a whole second (3, 2, 1). */
    public static OptionalInt announcement(int ticksLeft) {
        if (ticksLeft > 0 && ticksLeft % TICKS_PER_SECOND == 0) {
            return OptionalInt.of(ticksLeft / TICKS_PER_SECOND);
        }
        return OptionalInt.empty();
    }
}
