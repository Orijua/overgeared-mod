package com.orijua.overgearedmod.feature.economy;

import java.util.Optional;

/**
 * The client's side of the terminal: the last state the server sent for the open menu. No client classes, so the
 * network handler can use it on a dedicated server; the screen that draws it is in {@code economy.client}.
 */
public final class ClientTerminal {
    private static volatile int containerId = -1;
    private static volatile TerminalState state;

    private ClientTerminal() {
    }

    public static void update(int id, TerminalState newState) {
        state = newState;
        containerId = id;
    }

    /** The last state if it belongs to the menu {@code id}; empty otherwise. */
    public static Optional<TerminalState> stateFor(int id) {
        TerminalState current = state;
        return current != null && containerId == id ? Optional.of(current) : Optional.empty();
    }

    public static void clear() {
        state = null;
        containerId = -1;
    }
}
