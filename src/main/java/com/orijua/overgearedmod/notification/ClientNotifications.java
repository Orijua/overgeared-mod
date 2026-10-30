package com.orijua.overgearedmod.notification;

/**
 * The windows waiting on this client. Filled by the network handler, drawn by the HUD layer; both run
 * on the client main thread. Holds no client classes, so common code can reference it safely.
 */
public final class ClientNotifications {
    public static final NotificationQueue QUEUE = new NotificationQueue();

    private ClientNotifications() {
    }
}
