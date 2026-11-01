package com.orijua.overgearedmod.feature.npc;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WorkshopTest {
    @Test
    void hisMasterHearsWhenHeStopsOnHisOwn() {
        assertTrue(Workshop.stopped(ApprenticeSmith.Status.WORKING, ApprenticeSmith.Status.NO_MATERIALS));
        assertTrue(Workshop.stopped(ApprenticeSmith.Status.WORKING, ApprenticeSmith.Status.UNPAID));
        assertFalse(Workshop.stopped(ApprenticeSmith.Status.WORKING, ApprenticeSmith.Status.NO_ORDER), "cancelling is no news");
        assertFalse(Workshop.stopped(ApprenticeSmith.Status.UNPAID, ApprenticeSmith.Status.NO_MATERIALS), "only once, when he stops");
        assertFalse(Workshop.stopped(ApprenticeSmith.Status.WORKING, ApprenticeSmith.Status.WORKING));
    }
}
