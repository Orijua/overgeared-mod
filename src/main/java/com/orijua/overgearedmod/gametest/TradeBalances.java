package com.orijua.overgearedmod.gametest;

import com.orijua.overgearedmod.core.config.TradeBalance;

/** Copies of {@link TradeBalance#DEFAULT} with one number changed; a new field of the record only breaks this class and its unit-test twin. */
final class TradeBalances {
    private static final TradeBalance D = TradeBalance.DEFAULT;

    private TradeBalances() {
    }

    private static TradeBalance copy(double other, int won, double step, double floor, int recovery, int cap, long maxSell) {
        return new TradeBalance(D.basePrice(), D.materialMultiplier(), other, D.enhancementBonus(), D.sellShare(), won,
                D.exportCommission(), step, floor, recovery, cap, maxSell);
    }

    static TradeBalance withOtherMaterial(double value) {
        return copy(value, D.wonPerReference(), D.saturationStep(), D.saturationFloor(), D.recoveryPerDay(), D.pressureCap(), D.maxSellEmeralds());
    }

    static TradeBalance withWonPerReference(int value) {
        return copy(D.otherMaterial(), value, D.saturationStep(), D.saturationFloor(), D.recoveryPerDay(), D.pressureCap(), D.maxSellEmeralds());
    }

    static TradeBalance withMaxSellEmeralds(long value) {
        return copy(D.otherMaterial(), D.wonPerReference(), D.saturationStep(), D.saturationFloor(), D.recoveryPerDay(), D.pressureCap(), value);
    }
}
