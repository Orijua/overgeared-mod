package com.orijua.overgearedmod.feature.capsule;

import org.junit.jupiter.api.Test;

import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

class CapsuleTextTest {
    @Test
    void wholeNumbersHaveNoDecimals() {
        assertAll(
                () -> assertEquals("55", CapsuleText.minutes(55.0)),
                () -> assertEquals("0", CapsuleText.minutes(0.0)),
                () -> assertEquals("1440", CapsuleText.minutes(1440.0)));
    }

    @Test
    void anythingElseHasOneDecimal() {
        assertAll(
                () -> assertEquals("7.5", CapsuleText.minutes(7.5)),
                () -> assertEquals("0.1", CapsuleText.minutes(0.1)),
                () -> assertEquals("41.3", CapsuleText.minutes(41.25 + 0.05)),
                () -> assertEquals("8.3", CapsuleText.minutes(8.25 + 0.04)));
    }

    @Test
    void aValueThatRoundsToAWholeNumberIsWrittenWhole() {
        assertAll(
                () -> assertEquals("8", CapsuleText.minutes(7.96)),
                () -> assertEquals("55", CapsuleText.minutes(54.99)));
    }

    @Test
    void theDecimalSeparatorIsAlwaysADot() {
        Locale previous = Locale.getDefault();
        try {
            Locale.setDefault(Locale.GERMANY);
            assertEquals("7.5", CapsuleText.minutes(7.5));
        } finally {
            Locale.setDefault(previous);
        }
    }

    @Test
    void notFiniteValuesAreADash() {
        assertAll(
                () -> assertEquals("-", CapsuleText.minutes(Double.NaN)),
                () -> assertEquals("-", CapsuleText.minutes(Double.POSITIVE_INFINITY)));
    }
}
