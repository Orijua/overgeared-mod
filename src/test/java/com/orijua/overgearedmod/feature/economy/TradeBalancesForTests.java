package com.orijua.overgearedmod.feature.economy;

import com.orijua.overgearedmod.core.config.TradeBalance;
import net.minecraft.resources.ResourceLocation;

import java.util.Map;

/** Copies of {@link TradeBalance#DEFAULT} with one number changed; a new field of the record only breaks this class and its gametest twin. */
final class TradeBalancesForTests {
    private static final TradeBalance D = TradeBalance.DEFAULT;

    private TradeBalancesForTests() {
    }

    static TradeBalance of(double otherMaterial, double sellShare, double saturationStep,
                           double saturationFloor, int recoveryPerDay, int pressureCap, long maxSellEmeralds) {
        return new TradeBalance(D.basePrice(), D.materialMultiplier(), otherMaterial, D.enhancementBonus(), sellShare,
                saturationStep, saturationFloor, recoveryPerDay, pressureCap, maxSellEmeralds, D.minDurabilityFactor(), D.itemMultiplier());
    }

    static TradeBalance withRecoveryPerDay(int value) {
        return of(D.otherMaterial(), D.sellShare(), D.saturationStep(), D.saturationFloor(), value, D.pressureCap(), D.maxSellEmeralds());
    }

    static TradeBalance withPressureCap(int value) {
        return of(D.otherMaterial(), D.sellShare(), D.saturationStep(), D.saturationFloor(), D.recoveryPerDay(), value, D.maxSellEmeralds());
    }

    static TradeBalance withSaturationStep(double value) {
        return of(D.otherMaterial(), D.sellShare(), value, D.saturationFloor(), D.recoveryPerDay(), D.pressureCap(), D.maxSellEmeralds());
    }

    static TradeBalance withSaturationFloor(double value) {
        return of(D.otherMaterial(), D.sellShare(), D.saturationStep(), value, D.recoveryPerDay(), D.pressureCap(), D.maxSellEmeralds());
    }

    static TradeBalance withMinDurabilityFactor(double value) {
        return new TradeBalance(D.basePrice(), D.materialMultiplier(), D.otherMaterial(), D.enhancementBonus(), D.sellShare(),
                D.saturationStep(), D.saturationFloor(), D.recoveryPerDay(), D.pressureCap(), D.maxSellEmeralds(), value, D.itemMultiplier());
    }

    static TradeBalance withSellShare(double value) {
        return of(D.otherMaterial(), value, D.saturationStep(), D.saturationFloor(), D.recoveryPerDay(), D.pressureCap(), D.maxSellEmeralds());
    }

    static TradeBalance withItemMultipliers(Map<ResourceLocation, Double> multipliers) {
        return new TradeBalance(D.basePrice(), D.materialMultiplier(), D.otherMaterial(), D.enhancementBonus(), D.sellShare(),
                D.saturationStep(), D.saturationFloor(), D.recoveryPerDay(), D.pressureCap(), D.maxSellEmeralds(), D.minDurabilityFactor(),
                multipliers);
    }
}
