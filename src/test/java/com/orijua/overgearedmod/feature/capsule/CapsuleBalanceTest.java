package com.orijua.overgearedmod.feature.capsule;

import com.orijua.overgearedmod.core.config.CapsuleBalance;
import com.orijua.overgearedmod.core.config.FatigueBalance;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CapsuleBalanceTest {
    private static final CapsuleBalance D = CapsuleBalance.DEFAULT;
    private static final FatigueBalance F = FatigueBalance.DEFAULT;

    private static CapsuleBalance with(double aS, double aR, double dS, double dR, long aL, long dL, int rm, int rx, double re) {
        return new CapsuleBalance(aS, aR, dS, dR, aL, dL, rm, rx, re);
    }

    @Test
    void theDefaultIsTheDesignedOne() {
        assertEquals(55.0, D.advancedSessionMinutes());
        assertEquals(8.0, D.advancedRecoveryMinutes());
        assertEquals(75.0, D.diamondSessionMinutes());
        assertEquals(6.0, D.diamondRecoveryMinutes());
        assertEquals(3000L, D.advancedLicenseWon());
        assertEquals(15000L, D.diamondLicenseWon());
        assertEquals(10, D.restedMinutes());
        assertEquals(10, D.restedXpPercent());
        assertEquals(0.90, D.restedMinEnergy());
        assertEquals(D, CapsuleBalance.current());
    }

    @Test
    void minutesPerTierAndTheBasicOneIsTheFatigues() {
        assertEquals(F.sessionMinutes(), D.sessionMinutes(CapsuleTier.BASIC, F));
        assertEquals(F.recoveryMinutes(), D.recoveryMinutes(CapsuleTier.BASIC, F));
        assertEquals(55.0, D.sessionMinutes(CapsuleTier.ADVANCED, F));
        assertEquals(8.0, D.recoveryMinutes(CapsuleTier.ADVANCED, F));
        assertEquals(75.0, D.sessionMinutes(CapsuleTier.DIAMOND, F));
        assertEquals(6.0, D.recoveryMinutes(CapsuleTier.DIAMOND, F));
    }

    @Test
    void licensePricesAndTheBasicHasNone() {
        assertEquals(3000L, D.licenseWon(CapsuleTier.ADVANCED));
        assertEquals(15000L, D.licenseWon(CapsuleTier.DIAMOND));
        assertThrows(IllegalArgumentException.class, () -> D.licenseWon(CapsuleTier.BASIC));
    }

    @Test
    void sessionsAreBetweenOneAndADay() {
        assertThrows(IllegalArgumentException.class, () -> with(0.99, 8, 75, 6, 3000, 15000, 10, 10, 0.9));
        assertThrows(IllegalArgumentException.class, () -> with(55, 8, 1441, 6, 3000, 15000, 10, 10, 0.9));
        assertThrows(IllegalArgumentException.class, () -> with(Double.NaN, 8, 75, 6, 3000, 15000, 10, 10, 0.9));
        assertEquals(1.0, with(1, 8, 75, 6, 3000, 15000, 10, 10, 0.9).advancedSessionMinutes());
        assertEquals(1440.0, with(55, 8, 1440, 6, 3000, 15000, 10, 10, 0.9).diamondSessionMinutes());
    }

    @Test
    void recoveriesAreBetweenATenthAndADay() {
        assertThrows(IllegalArgumentException.class, () -> with(55, 0.09, 75, 6, 3000, 15000, 10, 10, 0.9));
        assertThrows(IllegalArgumentException.class, () -> with(55, 8, 75, 1441, 3000, 15000, 10, 10, 0.9));
        assertEquals(0.1, with(55, 0.1, 75, 6, 3000, 15000, 10, 10, 0.9).advancedRecoveryMinutes());
    }

    @Test
    void licensesCostBetweenOneAndABillion() {
        assertThrows(IllegalArgumentException.class, () -> with(55, 8, 75, 6, 0, 15000, 10, 10, 0.9));
        assertThrows(IllegalArgumentException.class, () -> with(55, 8, 75, 6, 3000, 1_000_000_001L, 10, 10, 0.9));
        assertEquals(1L, with(55, 8, 75, 6, 1, 15000, 10, 10, 0.9).advancedLicenseWon());
    }

    @Test
    void theRestedNumbersHaveTheirRanges() {
        assertThrows(IllegalArgumentException.class, () -> with(55, 8, 75, 6, 3000, 15000, -1, 10, 0.9));
        assertThrows(IllegalArgumentException.class, () -> with(55, 8, 75, 6, 3000, 15000, 1441, 10, 0.9));
        assertThrows(IllegalArgumentException.class, () -> with(55, 8, 75, 6, 3000, 15000, 10, -1, 0.9));
        assertThrows(IllegalArgumentException.class, () -> with(55, 8, 75, 6, 3000, 15000, 10, 1001, 0.9));
        assertThrows(IllegalArgumentException.class, () -> with(55, 8, 75, 6, 3000, 15000, 10, 10, 1.01));
        assertThrows(IllegalArgumentException.class, () -> with(55, 8, 75, 6, 3000, 15000, 10, 10, -0.01));
        assertEquals(0, with(55, 8, 75, 6, 3000, 15000, 0, 0, 0.0).restedMinutes());
    }

    @Test
    void anOverrideLastsOnlyWhileItRunsAndIsRestoredAfterAThrow() {
        CapsuleBalance custom = with(30, 5, 60, 3, 100, 200, 5, 20, 0.5);
        CapsuleBalance.withOverride(custom, () -> assertEquals(custom, CapsuleBalance.current()));
        assertEquals(D, CapsuleBalance.current());
        assertThrows(IllegalStateException.class, () -> CapsuleBalance.withOverride(custom, () -> {
            throw new IllegalStateException("boom");
        }));
        assertEquals(D, CapsuleBalance.current());
    }
}
