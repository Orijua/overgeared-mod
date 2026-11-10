package com.orijua.overgearedmod.feature.economy;

import com.orijua.overgearedmod.feature.capsule.CapsuleTier;

/** The pure rules of the capsule licenses sold at the terminal. */
public final class LicenseRules {
    private LicenseRules() {
    }

    /**
     * What stops the license of {@code tier} from being bought, if anything. The first lock that applies wins, so a closed
     * license never reads "you already have it": the advanced one is closed while the debt is active, the diamond one
     * until an advanced capsule is owned (the debt does not matter for it).
     *
     * <p>Unlike {@link AccountRules#isDebtSettled}, a debt that was never taken ({@code UNASSIGNED}) also opens the
     * advanced license: only an active debt closes it.
     *
     * @throws IllegalArgumentException for the basic tier, which has no license
     */
    public static LicenseLock lock(CapsuleTier tier, DebtState debt, boolean hasAdvanced, boolean alreadyHas) {
        switch (tier) {
            case BASIC -> throw new IllegalArgumentException("The basic capsule has no license");
            case ADVANCED -> {
                if (debt == DebtState.ACTIVE) {
                    return LicenseLock.DEBT_ACTIVE;
                }
            }
            case DIAMOND -> {
                if (!hasAdvanced) {
                    return LicenseLock.NEEDS_ADVANCED;
                }
            }
        }
        return alreadyHas ? LicenseLock.ALREADY_HAVE : LicenseLock.OPEN;
    }
}
