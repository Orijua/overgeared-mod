package com.orijua.overgearedmod.feature.economy;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MoneyTest {
    @Test
    void percentIsReadable() {
        assertEquals("5", Money.percent(0.05));
        assertEquals("2.5", Money.percent(0.025));
        assertEquals("0", Money.percent(0.0));
        assertEquals("100", Money.percent(1.0));
        assertEquals("12.3", Money.percent(0.123));
    }

    @Test
    void floorAbsorbsFloatingPointNoise() {
        assertEquals(95L, Money.floor(100 * (1.0 - 0.05)));
        assertEquals(94L, Money.floor(94.9));
    }

    @Test
    void afterCommissionIsExact() {
        assertEquals(94L, Money.afterCommission(99L, 0.05), "94.05 rounds down");
        assertEquals(1140L, Money.afterCommission(1200L, 0.05));
        assertEquals(0L, Money.afterCommission(1200L, 1.0));
    }
}
