package com.orijua.overgearedmod.feature.economy;

import com.orijua.overgearedmod.feature.capsule.CapsuleTier;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertAll;
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

    /** The expected lock written from the rule in words, not from the code: closed first, then "have it", else open. */
    private static LicenseLock expected(CapsuleTier tier, DebtState debt, boolean hasAdvanced, boolean alreadyHas) {
        boolean closed = tier == CapsuleTier.ADVANCED ? debt == DebtState.ACTIVE : !hasAdvanced;
        if (closed) {
            return tier == CapsuleTier.ADVANCED ? LicenseLock.DEBT_ACTIVE : LicenseLock.NEEDS_ADVANCED;
        }
        return alreadyHas ? LicenseLock.ALREADY_HAVE : LicenseLock.OPEN;
    }

    @Test
    void theWholeTableOfThreeTiersThreeDebtsAndTwoFlags() {
        boolean[] both = {false, true};
        for (CapsuleTier tier : CapsuleTier.values()) {
            for (DebtState debt : DebtState.values()) {
                for (boolean hasAdvanced : both) {
                    for (boolean alreadyHas : both) {
                        String what = tier + " " + debt + " hasAdvanced=" + hasAdvanced + " alreadyHas=" + alreadyHas;
                        if (tier == CapsuleTier.BASIC) {
                            assertThrows(IllegalArgumentException.class, () -> lock(tier, debt, hasAdvanced, alreadyHas), what);
                        } else {
                            assertEquals(expected(tier, debt, hasAdvanced, alreadyHas), lock(tier, debt, hasAdvanced, alreadyHas), what);
                        }
                    }
                }
            }
        }
    }

    @Test
    void theNamedCornersOfTheTable() {
        assertAll(
                () -> assertEquals(LicenseLock.ALREADY_HAVE, lock(CapsuleTier.ADVANCED, DebtState.UNASSIGNED, false, true), "never taken debt, has it"),
                () -> assertEquals(LicenseLock.ALREADY_HAVE, lock(CapsuleTier.DIAMOND, DebtState.ACTIVE, true, true), "diamond ignores an active debt"),
                () -> assertEquals(LicenseLock.NEEDS_ADVANCED, lock(CapsuleTier.DIAMOND, DebtState.ACTIVE, false, true), "closed wins over having it"),
                () -> assertEquals(LicenseLock.OPEN, lock(CapsuleTier.ADVANCED, DebtState.SETTLED, true, false), "an advanced capsule does not matter for the advanced license"));
    }
}
