package com.orijua.overgearedmod.feature.economy;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MoneyTest {
    @Test
    void floorAbsorbsFloatingPointNoise() {
        assertEquals(95L, Money.floor(100 * (1.0 - 0.05)));
        assertEquals(94L, Money.floor(94.9));
    }
}
