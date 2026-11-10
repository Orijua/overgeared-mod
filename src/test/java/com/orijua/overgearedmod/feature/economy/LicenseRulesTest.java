package com.orijua.overgearedmod.feature.economy;

import com.orijua.overgearedmod.feature.capsule.CapsuleTier;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class LicenseRulesTest {
    private static LicenseLock lock(CapsuleTier tier, DebtState debt, boolean hasAdvanced, boolean alreadyHas) {
        return LicenseRules.lock(tier, debt, hasAdvanced, alreadyHas);
    }

    @Test
    void anActiveDebtClosesTheAdvancedLicense() {
        assertEquals(LicenseLock.DEBT_ACTIVE, lock(CapsuleTier.ADVANCED, DebtState.ACTIVE, false, false));
    }

    @Test
    void aSettledOrNeverTakenDebtOpensTheAdvancedLicense() {
        assertEquals(LicenseLock.OPEN, lock(CapsuleTier.ADVANCED, DebtState.SETTLED, false, false));
        assertEquals(LicenseLock.OPEN, lock(CapsuleTier.ADVANCED, DebtState.UNASSIGNED, false, false));
    }

    @Test
    void theDiamondLicenseNeedsAnAdvancedCapsuleEvenWithTheDebtSettled() {
        assertEquals(LicenseLock.NEEDS_ADVANCED, lock(CapsuleTier.DIAMOND, DebtState.SETTLED, false, false));
        assertEquals(LicenseLock.NEEDS_ADVANCED, lock(CapsuleTier.DIAMOND, DebtState.ACTIVE, false, false));
    }

    @Test
    void theDiamondLicenseIgnoresTheDebtOnceThereIsAnAdvancedCapsule() {
        assertEquals(LicenseLock.OPEN, lock(CapsuleTier.DIAMOND, DebtState.ACTIVE, true, false));
        assertEquals(LicenseLock.OPEN, lock(CapsuleTier.DIAMOND, DebtState.UNASSIGNED, true, false));
    }

    @Test
    void carryingTheLicenseSaysSo() {
        assertEquals(LicenseLock.ALREADY_HAVE, lock(CapsuleTier.ADVANCED, DebtState.SETTLED, false, true));
        assertEquals(LicenseLock.ALREADY_HAVE, lock(CapsuleTier.DIAMOND, DebtState.SETTLED, true, true));
    }

    @Test
    void aLockWinsOverHavingTheLicense() {
        assertEquals(LicenseLock.DEBT_ACTIVE, lock(CapsuleTier.ADVANCED, DebtState.ACTIVE, false, true));
        assertEquals(LicenseLock.NEEDS_ADVANCED, lock(CapsuleTier.DIAMOND, DebtState.SETTLED, false, true));
    }

    @Test
    void theBasicCapsuleHasNoLicense() {
        assertThrows(IllegalArgumentException.class, () -> lock(CapsuleTier.BASIC, DebtState.SETTLED, true, false));
    }
}
