package com.orijua.overgearedmod.feature.npc;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
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

    @Test
    void progressIsReportedInFivePercentSteps() {
        assertEquals(0, WorkshopEntry.quantize(4));
        assertEquals(5, WorkshopEntry.quantize(9));
        assertEquals(40, WorkshopEntry.quantize(44));
        assertEquals(100, WorkshopEntry.quantize(100));
        assertEquals(100, WorkshopEntry.quantize(250), "never past the end");
        assertEquals(0, WorkshopEntry.quantize(-3), "never below the start");
    }

    @Test
    void theHeaderCountsWhoForgesAndWhoStopped() {
        List<WorkshopEntry> entries = List.of(entry(ApprenticeSmith.Status.WORKING), entry(ApprenticeSmith.Status.WORKING),
                entry(ApprenticeSmith.Status.NO_MATERIALS), entry(ApprenticeSmith.Status.NO_ORDER), entry(ApprenticeSmith.Status.UNPAID));
        assertEquals(2, Workshop.working(entries));
        assertEquals(2, Workshop.stoppedCount(entries), "an apprentice without an order is idle, not stopped");
        assertEquals(0, Workshop.working(List.of()));
    }

    private static WorkshopEntry entry(ApprenticeSmith.Status status) {
        return new WorkshopEntry(UUID.randomUUID(), "Tom", false, false, 50, status.name(), Optional.empty(), 1, 0, Optional.empty(), 0);
    }
}
