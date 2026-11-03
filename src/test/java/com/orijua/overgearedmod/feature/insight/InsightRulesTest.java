package com.orijua.overgearedmod.feature.insight;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class InsightRulesTest {
    @Test
    void moreInsightRevealsMore() {
        assertEquals(InsightRules.Detail.LOW, InsightRules.detail(-5));
        assertEquals(InsightRules.Detail.LOW, InsightRules.detail(0));
        assertEquals(InsightRules.Detail.LOW, InsightRules.detail(14));
        assertEquals(InsightRules.Detail.MEDIUM, InsightRules.detail(15));
        assertEquals(InsightRules.Detail.MEDIUM, InsightRules.detail(39));
        assertEquals(InsightRules.Detail.HIGH, InsightRules.detail(40));
    }

    @Test
    void theNextThresholdIsWhatTheWindowAsksFor() {
        assertEquals(15, InsightRules.nextThreshold(0));
        assertEquals(15, InsightRules.nextThreshold(3));
        assertEquals(15, InsightRules.nextThreshold(14));
        assertEquals(40, InsightRules.nextThreshold(15));
        assertEquals(40, InsightRules.nextThreshold(20));
        assertEquals(40, InsightRules.nextThreshold(39));
        assertEquals(-1, InsightRules.nextThreshold(40));
    }

    @Test
    void moreInsightFillsTheBarFaster() {
        assertEquals(200, InsightRules.observeTicks(200, 0), "ten seconds with none");
        assertEquals(100, InsightRules.observeTicks(200, 50), "half at 50");
        assertEquals(50, InsightRules.observeTicks(100, 50), "a sword's time shortens too");
        assertEquals(40, InsightRules.observeTicks(200, 10_000), "never under two seconds");
        assertEquals(30, InsightRules.observeTicks(30, 500), "a base already under the floor stays");
        assertEquals(200, InsightRules.observeTicks(200, -10), "negative Insight counts as none");
        assertEquals(0, InsightRules.observeTicks(0, 0), "a zero base stays zero");
        assertEquals(137, InsightRules.observeTicks(140, 1), "the Commander's sword at 1 Insight");
        assertEquals(98, InsightRules.observeTicks(100, 1), "the Ruler's sword at 1 Insight");
    }

    @Test
    void aDayIsAMinecraftDay() {
        assertEquals(0, InsightRules.day(23_999));
        assertEquals(1, InsightRules.day(24_000));
    }
}
