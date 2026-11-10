package com.orijua.overgearedmod.feature.economy;

import org.junit.jupiter.api.Test;

import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MoneyTest {
    @Test
    void floorAbsorbsFloatingPointNoise() {
        assertEquals(95L, Money.floor(100 * (1.0 - 0.05)));
        assertEquals(94L, Money.floor(94.9));
    }

    @Test
    void formatGroupsThousandsWithADot() {
        assertEquals("0", Money.format(0));
        assertEquals("99", Money.format(99));
        assertEquals("100", Money.format(100));
        assertEquals("999", Money.format(999));
        assertEquals("1.000", Money.format(1000));
        assertEquals("6.400", Money.format(6400));
        assertEquals("999.999", Money.format(999_999));
        assertEquals("1.000.000", Money.format(1_000_000));
        assertEquals("-6.400", Money.format(-6400));
        assertEquals("-999", Money.format(-999));
        assertEquals("9.223.372.036.854.775.807", Money.format(Long.MAX_VALUE));
        assertEquals("-9.223.372.036.854.775.808", Money.format(Long.MIN_VALUE));
    }

    @Test
    void formatIgnoresTheDefaultLocale() {
        Locale before = Locale.getDefault();
        try {
            for (Locale locale : new Locale[]{Locale.US, Locale.GERMANY, Locale.FRANCE, Locale.forLanguageTag("ar-EG")}) {
                Locale.setDefault(locale);
                assertEquals("6.400", Money.format(6400), locale.toString());
                assertEquals("-1.234.567", Money.format(-1_234_567), locale.toString());
            }
        } finally {
            Locale.setDefault(before);
        }
    }
}
