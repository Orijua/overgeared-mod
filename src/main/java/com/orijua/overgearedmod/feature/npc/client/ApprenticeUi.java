package com.orijua.overgearedmod.feature.npc.client;

import com.orijua.overgearedmod.core.client.UiStyle;
import com.orijua.overgearedmod.feature.npc.ApprenticeSmith;

/**
 * What an apprentice looks like on screen, shared by his own window and the Workshop tab so the two always agree:
 * add a new rule (a colour, a threshold) here, never in one screen only.
 */
final class ApprenticeUi {
    private ApprenticeUi() {
    }

    /** Green while he forges, grey with no order, red when he is stopped. */
    static int statusColor(ApprenticeSmith.Status status) {
        return switch (status) {
            case WORKING -> UiStyle.GOOD;
            case NO_ORDER -> UiStyle.MUTED;
            default -> UiStyle.BAD;
        };
    }

    /** Red when no day of wage is left. */
    static int wageColor(int daysPaid, int normal) {
        return daysPaid > 0 ? normal : UiStyle.BAD;
    }
}
