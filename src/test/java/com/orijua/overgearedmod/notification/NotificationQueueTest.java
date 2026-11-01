package com.orijua.overgearedmod.notification;

import net.minecraft.network.chat.Component;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NotificationQueueTest {
    private static final long D = NotificationQueue.DURATION_MS;
    private static final long F = NotificationQueue.FADE_MS;

    @Test
    void grandWindowsStayLonger() {
        NotificationQueue queue = new NotificationQueue();
        queue.push(new SystemNotification(SystemNotification.Kind.HIDDEN_CLASS, List.of(Component.literal("Night Hunter"))));
        queue.current(0);
        assertTrue(queue.current(D + 100).isPresent(), "still there after a normal window's time");
        assertTrue(queue.current(NotificationQueue.GRAND_DURATION_MS).isEmpty(), "gone after its own");
    }

    @Test
    void emptyQueueShowsNothing() {
        assertTrue(new NotificationQueue().current(0).isEmpty());
    }

    @Test
    void fadesInStaysThenFadesOut() {
        NotificationQueue queue = new NotificationQueue();
        queue.push(notification("a"));
        assertEquals(0.0f, queue.current(1000).orElseThrow().alpha(), 1e-6, "the clock starts when it is first drawn");
        assertEquals(0.5f, queue.current(1000 + F / 2).orElseThrow().alpha(), 1e-6);
        assertEquals(1.0f, queue.current(1000 + D / 2).orElseThrow().alpha(), 1e-6);
        assertEquals(0.5f, queue.current(1000 + D - F / 2).orElseThrow().alpha(), 1e-6);
        assertTrue(queue.current(1000 + D).isEmpty(), "gone after the duration");
    }

    @Test
    void showsOneAtATimeInOrder() {
        NotificationQueue queue = new NotificationQueue();
        SystemNotification first = notification("first");
        SystemNotification second = notification("second");
        queue.push(first);
        queue.push(second);
        assertSame(first, queue.current(0).orElseThrow().notification());
        assertSame(first, queue.current(D - 1).orElseThrow().notification());
        assertSame(second, queue.current(D).orElseThrow().notification(), "the next starts when the first ends");
        assertEquals(0.0f, queue.current(D).orElseThrow().alpha(), 1e-6, "and fades in from scratch");
        assertTrue(queue.current(2 * D).isEmpty());
    }

    @Test
    void aLongPauseSkipsEverythingThatExpired() {
        NotificationQueue queue = new NotificationQueue();
        queue.push(notification("a"));
        queue.current(0);
        queue.push(notification("b"));
        NotificationQueue.Shown shown = queue.current(D + 10).orElseThrow();
        assertEquals("b", shown.notification().lines().get(0).getString(), "a expired");
        assertEquals(0.0f, shown.alpha(), 1e-6, "b starts now, not in the past");
    }

    @Test
    void dropsTheNewestWhenTooManyAreWaiting() {
        NotificationQueue queue = new NotificationQueue();
        for (int i = 0; i < NotificationQueue.MAX_WAITING + 3; i++) {
            queue.push(notification("n" + i));
        }
        assertEquals(NotificationQueue.MAX_WAITING, queue.size());
        assertEquals("n0", queue.current(0).orElseThrow().notification().lines().get(0).getString());
    }

    private static SystemNotification notification(String text) {
        return new SystemNotification(SystemNotification.Kind.TITLE, List.of(Component.literal(text)));
    }
}
