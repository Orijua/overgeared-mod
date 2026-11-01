package com.orijua.overgearedmod.feature.notification;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Optional;

/**
 * Shows system windows one at a time: each stays {@link #DURATION_MS} (a grand one {@link #GRAND_DURATION_MS}),
 * fading in and out, then the next
 * one starts. Pure (time is passed in), so the client HUD and the unit tests drive it the same way.
 */
public final class NotificationQueue {
    public static final long DURATION_MS = 4000;
    /** The big moments stay longer. */
    public static final long GRAND_DURATION_MS = 7000;
    public static final long FADE_MS = 300;
    /** More than this waiting (e.g. a burst of titles) drops the newest instead of piling up for minutes. */
    public static final int MAX_WAITING = 6;

    public record Shown(SystemNotification notification, float alpha) {
    }

    private final Deque<SystemNotification> waiting = new ArrayDeque<>();
    private long shownSince = -1;

    public void push(SystemNotification notification) {
        if (waiting.size() < MAX_WAITING) {
            waiting.addLast(notification);
        }
    }

    /** The window to draw at {@code nowMs} and how opaque it is (0-1); empty when there is nothing to show. */
    public Optional<Shown> current(long nowMs) {
        while (!waiting.isEmpty()) {
            if (shownSince < 0) {
                shownSince = nowMs;
            }
            long elapsed = nowMs - shownSince;
            long duration = duration(waiting.peekFirst());
            if (elapsed < duration) {
                return Optional.of(new Shown(waiting.peekFirst(), alpha(elapsed, duration)));
            }
            waiting.removeFirst();
            shownSince = -1;
        }
        return Optional.empty();
    }

    public int size() {
        return waiting.size();
    }

    /** Drops everything, e.g. when leaving a world so old windows don't show up in the next one. */
    public void clear() {
        waiting.clear();
        shownSince = -1;
    }

    public static long duration(SystemNotification notification) {
        return notification.kind().grand() ? GRAND_DURATION_MS : DURATION_MS;
    }

    private static float alpha(long elapsed, long duration) {
        if (elapsed < FADE_MS) {
            return elapsed / (float) FADE_MS;
        }
        long left = duration - elapsed;
        return left < FADE_MS ? left / (float) FADE_MS : 1.0f;
    }
}
