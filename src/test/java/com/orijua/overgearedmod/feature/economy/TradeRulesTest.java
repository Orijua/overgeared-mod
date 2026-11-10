package com.orijua.overgearedmod.feature.economy;

import com.orijua.overgearedmod.core.config.TradeBalance;
import com.orijua.overgearedmod.feature.economy.TradeRules.Refusal;
import com.orijua.overgearedmod.feature.equipment.ItemRating;
import com.google.gson.JsonObject;
import com.orijua.overgearedmod.lang.LangFiles;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TradeRulesTest {
    private static final TradeBalance B = TradeBalance.DEFAULT;

    @Test
    void anEpicIronSwordIsWorth40() {
        long ref = TradeRules.reference(ItemRating.EPIC, 1.0, 0, true, B);
        assertEquals(40, ref);
        assertEquals(16, TradeRules.sellPrice(ref, 0, B));
    }

    @Test
    void aDiamondLegendaryPlusFiveIsWorth1800() {
        long ref = TradeRules.reference(ItemRating.LEGENDARY, 2.0, 5, true, B);
        assertEquals(1800, ref);
        assertEquals(720, TradeRules.sellPrice(ref, 0, B));
    }

    @Test
    void aWoodenNormalPieceIsWorthAtLeastOne() {
        long ref = TradeRules.reference(ItemRating.NORMAL, 0.5, 0, true, B);
        assertEquals(1, ref);
        assertEquals(1, TradeRules.sellPrice(ref, 0, B));
    }

    @Test
    void enhancementOnlyCountsForWeapons() {
        assertEquals(60, TradeRules.reference(ItemRating.EPIC, 1.0, 5, true, B));
        assertEquals(40, TradeRules.reference(ItemRating.EPIC, 1.0, 5, false, B));
    }

    @Test
    void everyBasePrice() {
        assertEquals(3, TradeRules.reference(ItemRating.NORMAL, 1.0, 0, false, B));
        assertEquals(12, TradeRules.reference(ItemRating.RARE, 1.0, 0, false, B));
        assertEquals(40, TradeRules.reference(ItemRating.EPIC, 1.0, 0, false, B));
        assertEquals(150, TradeRules.reference(ItemRating.UNIQUE, 1.0, 0, false, B));
        assertEquals(600, TradeRules.reference(ItemRating.LEGENDARY, 1.0, 0, false, B));
        assertEquals(2500, TradeRules.reference(ItemRating.MYTH, 1.0, 0, false, B));
    }

    @Test
    void theSaturationFollowsThePressure() {
        assertEquals(1.0, TradeRules.saturation(0, B));
        assertEquals(0.95, TradeRules.saturation(1, B), 1e-12);
        assertEquals(Math.pow(0.95, 36), TradeRules.saturation(36, B), 1e-12);
        assertEquals(0.15, TradeRules.saturation(37, B));
        assertEquals(0.15, TradeRules.saturation(1000, B));
        assertEquals(1.0, TradeRules.saturation(-3, B));
    }

    @Test
    void oneSaleLowersThePriceOfAnEpicIronSword() {
        assertEquals(15, TradeRules.sellPrice(40, 1, B));
    }

    @Test
    void thePressureRecoversTwoPerDay() {
        assertEquals(3, TradeRules.recovered(5, 0, 1, B));
        assertEquals(0, TradeRules.recovered(5, 0, 3, B));
        assertEquals(0, TradeRules.recovered(5, 0, 100, B));
        assertEquals(5, TradeRules.recovered(5, 4, 4, B));
    }

    @Test
    void daysThatGoBackCountAsNone() {
        assertEquals(5, TradeRules.recovered(5, 10, 3, B));
    }

    @Test
    void aNegativePressureIsNone() {
        assertEquals(0, TradeRules.recovered(-4, 0, 0, B));
    }

    @Test
    void recoveredWithHugeDays() {
        assertEquals(0, TradeRules.recovered(5, Long.MIN_VALUE, Long.MAX_VALUE, B));
        assertEquals(5, TradeRules.recovered(5, Long.MAX_VALUE, Long.MIN_VALUE, B));
        assertEquals(0, TradeRules.recovered(Integer.MAX_VALUE, 0, Long.MAX_VALUE, B));
    }

    @Test
    void noRecoveryWhenPerDayIsZeroOrNegative() {
        assertEquals(5, TradeRules.recovered(5, 0, 10, TradeBalancesForTests.withRecoveryPerDay(0)));
        assertEquals(5, TradeRules.recovered(5, 0, 10, TradeBalancesForTests.withRecoveryPerDay(-3)));
    }

    @Test
    void payableNeedsAtLeastOne() {
        assertFalse(TradeRules.payable(0, B));
        assertFalse(TradeRules.payable(-5, B));
        assertTrue(TradeRules.payable(1, B));
    }

    @Test
    void payableStopsAtTheSellCap() {
        long cap = B.maxSellEmeralds();
        assertTrue(TradeRules.payable(cap, B));
        assertFalse(TradeRules.payable(cap + 1, B));
    }

    @Test
    void paymentsSplitInStacks() {
        long[] sale = TradeRules.payments(720, 64);
        assertEquals(12, sale.length);
        for (int i = 0; i < 11; i++) {
            assertEquals(64, sale[i]);
        }
        assertEquals(16, sale[11]);
        assertArrayEquals(new long[]{64}, TradeRules.payments(64, 64));
        assertArrayEquals(new long[]{1}, TradeRules.payments(1, 64));
        long total = 0;
        for (long part : TradeRules.payments(B.maxSellEmeralds(), 64)) {
            total += part;
        }
        assertEquals(B.maxSellEmeralds(), total);
    }

    private static TradeRules.Piece piece(TradeCategory category, ItemRating rating, long reference) {
        return new TradeRules.Piece(new TradeKey(category, rating), reference);
    }

    @Test
    void threeEpicSwordsOfTheSameKeyPayLessEach() {
        TradeRules.Piece sword = piece(TradeCategory.SWORD, ItemRating.EPIC, 40);
        long[] prices = TradeRules.sellPrices(List.of(sword, sword, sword), TradeSaturation.EMPTY, 0, B);
        assertArrayEquals(new long[]{16, 15, 14}, prices);
        assertEquals(45, TradeRules.total(prices));
    }

    @Test
    void interleavedKeysOnlyPressTheirOwn() {
        TradeRules.Piece sword = piece(TradeCategory.SWORD, ItemRating.EPIC, 40);
        TradeRules.Piece helmet = piece(TradeCategory.HELMET, ItemRating.EPIC, 40);
        assertArrayEquals(new long[]{16, 16, 15}, TradeRules.sellPrices(List.of(sword, helmet, sword), TradeSaturation.EMPTY, 0, B));
    }

    @Test
    void earlierPressureCountsInABatch() {
        TradeSaturation pressed = TradeSaturation.EMPTY;
        TradeKey key = new TradeKey(TradeCategory.SWORD, ItemRating.EPIC);
        for (int i = 0; i < 36; i++) {
            pressed = pressed.recordSale(key, 0, B);
        }
        TradeRules.Piece sword = piece(TradeCategory.SWORD, ItemRating.EPIC, 40);
        assertArrayEquals(new long[]{2, 2}, TradeRules.sellPrices(List.of(sword, sword), pressed, 0, B));
    }

    @Test
    void anEmptyBatchPricesNothingAndKeepsTheSaturation() {
        TradeSaturation before = TradeSaturation.EMPTY.recordSale(new TradeKey(TradeCategory.AXE, ItemRating.RARE), 0, B);
        assertEquals(0, TradeRules.sellPrices(List.of(), before, 0, B).length);
        assertEquals(TradeSaturation.EMPTY.recordSale(new TradeKey(TradeCategory.AXE, ItemRating.RARE), 0, B), before);
        assertEquals(0, TradeRules.total(new long[0]));
    }

    @Test
    void sellPricesEqualApplyingSellPriceAndRecordSaleByHand() {
        TradeKey[] keys = {new TradeKey(TradeCategory.SWORD, ItemRating.EPIC), new TradeKey(TradeCategory.HELMET, ItemRating.RARE),
                new TradeKey(TradeCategory.BOW, ItemRating.LEGENDARY)};
        long[] references = {40, 12, 600};
        List<TradeRules.Piece> pieces = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            pieces.add(new TradeRules.Piece(keys[(i * 7) % 3], references[(i * 7) % 3]));
        }
        TradeSaturation start = TradeSaturation.EMPTY.recordSale(keys[0], 0, B);
        TradeSaturation saturation = start;
        long[] expected = new long[pieces.size()];
        for (int i = 0; i < pieces.size(); i++) {
            TradeRules.Piece piece = pieces.get(i);
            expected[i] = TradeRules.sellPrice(piece.reference(), saturation.pressure(piece.key(), 0, B), B);
            saturation = saturation.recordSale(piece.key(), 0, B);
        }
        assertArrayEquals(expected, TradeRules.sellPrices(pieces, start, 0, B));
    }

    @Test
    void sellableSlotsSkipEmptyAndRefusedOnesInOrder() {
        Refusal[] refusals = new Refusal[36];
        boolean[] empty = new boolean[36];
        java.util.Arrays.fill(empty, true);
        empty[2] = false;
        empty[5] = false;
        refusals[5] = Refusal.LOANED;
        empty[7] = false;
        refusals[7] = Refusal.STACKED;
        empty[9] = false;
        refusals[9] = Refusal.NO_VALUE;
        empty[11] = false;
        refusals[11] = Refusal.UNRATED;
        empty[20] = false;
        empty[35] = false;
        assertEquals(List.of(2, 20, 35), TradeRules.sellableSlots(36, slot -> empty[slot], slot -> Optional.ofNullable(refusals[slot])));
        assertEquals(List.of(), TradeRules.sellableSlots(36, slot -> true, slot -> Optional.empty()));
        assertEquals(List.of(), TradeRules.sellableSlots(36, slot -> false, slot -> Optional.of(Refusal.UNRATED)));
    }

    @Test
    void validBatchChecksSizeRangeAndRepeats() {
        assertFalse(TradeRules.validBatch(List.of(), 36));
        List<Integer> all = new ArrayList<>();
        for (int i = 0; i < 36; i++) {
            all.add(i);
        }
        assertTrue(TradeRules.validBatch(all, 36));
        List<Integer> tooMany = new ArrayList<>(all);
        tooMany.add(0);
        assertFalse(TradeRules.validBatch(tooMany, 36));
        assertFalse(TradeRules.validBatch(List.of(3, 3), 36));
        assertFalse(TradeRules.validBatch(List.of(-1), 36));
        assertFalse(TradeRules.validBatch(List.of(36), 36));
        assertFalse(TradeRules.validBatch(List.of(0, 36), 36));
        assertTrue(TradeRules.validBatch(List.of(0), 36));
        assertTrue(TradeRules.validBatch(List.of(35), 36));
        assertTrue(TradeRules.validBatch(List.of(35, 0), 36));
        assertTrue(TradeRules.validBatch(List.of(40), 41));
        assertFalse(TradeRules.validBatch(List.of(10), 10));
        assertEquals(36, TradeRules.MAX_BATCH);
    }

    @Test
    void saturationPercentRounds() {
        assertEquals(100, TradeRules.saturationPercent(0, B));
        assertEquals(95, TradeRules.saturationPercent(1, B));
        assertEquals(15, TradeRules.saturationPercent(37, B));
        assertEquals(100, TradeRules.saturationPercent(1, TradeBalancesForTests.withSaturationStep(0.999)));
    }

    @Test
    void floorBeatsTheCurveBeforeTheCap() {
        TradeBalance floor = TradeBalancesForTests.withSaturationFloor(0.9);
        assertEquals(0.95, TradeRules.saturation(1, floor), 1e-12);
        assertEquals(0.9, TradeRules.saturation(3, floor), 1e-12);
        assertEquals(0.9, TradeRules.saturation(37, floor), 1e-12);
    }

    @Test
    void priceAtTheCap() {
        assertEquals(2, TradeRules.sellPrice(40, 37, B));
    }

    @Test
    void negativeEnhancementCountsAsZero() {
        assertEquals(TradeRules.reference(ItemRating.EPIC, 1.0, 0, true, B), TradeRules.reference(ItemRating.EPIC, 1.0, -5, true, B));
    }

    @Test
    void parseItemMultiplierReadsAnIdAndANumber() {
        TradeRules.ItemMultiplier fracaso = TradeRules.parseItemMultiplier("overgearedmod:fracaso_sword=0.25").orElseThrow();
        assertEquals("overgearedmod:fracaso_sword", fracaso.id().toString());
        assertEquals(0.25, fracaso.multiplier());
        assertEquals(2.0, TradeRules.parseItemMultiplier("  minecraft:iron_sword = 2  ").orElseThrow().multiplier());
        assertEquals(0.0, TradeRules.parseItemMultiplier("minecraft:iron_sword=0").orElseThrow().multiplier());
        assertEquals(100.0, TradeRules.parseItemMultiplier("minecraft:iron_sword=100").orElseThrow().multiplier());
        assertEquals("overgearedmod:jaffa_sword=2.0", TradeRules.parseItemMultiplier("overgearedmod:jaffa_sword=2").orElseThrow().asString());
    }

    @Test
    void parseItemMultiplierRejectsWhatIsNotAnObjectAndANumber() {
        for (String bad : new String[]{"minecraft:iron_sword", "#minecraft:swords=2", "minecraft:iron_sword=NaN",
                "minecraft:iron_sword=Infinity", "minecraft:iron_sword=-1", "minecraft:iron_sword=100.1", "minecraft:iron_sword=",
                "minecraft:iron_sword=two", "=2", "minecraft:=2", "Not An Id=2", ""}) {
            assertTrue(TradeRules.parseItemMultiplier(bad).isEmpty(), bad);
        }
        assertTrue(TradeRules.parseItemMultiplier(null).isEmpty());
    }

    /** Java's number syntax is what the config accepts: a suffix "d" or an exponent are numbers, hex floats too. */
    @Test
    void parseItemMultiplierAcceptsWhatJavaReadsAsANumber() {
        assertEquals(2.0, TradeRules.parseItemMultiplier("minecraft:iron_sword=2d").orElseThrow().multiplier());
        assertEquals(10.0, TradeRules.parseItemMultiplier("minecraft:iron_sword=1e1").orElseThrow().multiplier());
        assertEquals(2.0, TradeRules.parseItemMultiplier("minecraft:iron_sword=0x1p1").orElseThrow().multiplier());
    }

    @Test
    void parseItemMultiplierTakesTheLastEqualsAndSoRejectsAnEqualsInTheId() {
        assertTrue(TradeRules.parseItemMultiplier("a=b=2").isEmpty(), "the id would be a=b, which is not an id");
        assertTrue(TradeRules.parseItemMultiplier("minecraft:iron_sword==2").isEmpty(), "the id would end in =");
        assertTrue(TradeRules.parseItemMultiplier("minecraft:iron_sword=2=").isEmpty(), "nothing after the last =");
    }

    @Test
    void parseItemMultiplierReadsMinusZeroAsPlainZero() {
        TradeRules.ItemMultiplier zero = TradeRules.parseItemMultiplier("minecraft:iron_sword=-0").orElseThrow();
        assertEquals(0.0, zero.multiplier(), "assertEquals on doubles tells -0.0 from 0.0");
        assertEquals("minecraft:iron_sword=0.0", zero.asString());
        assertEquals(0.0, TradeRules.parseItemMultiplier("minecraft:iron_sword=-0.0").orElseThrow().multiplier());
    }

    @Test
    void parseItemMultiplierDoesNotAcceptUppercaseIds() {
        assertTrue(TradeRules.parseItemMultiplier("Minecraft:Iron_Sword=2").isEmpty());
        assertTrue(TradeRules.parseItemMultiplier("minecraft:Iron_Sword=2").isEmpty());
    }

    @Test
    void asStringAndParseGoRoundTripEvenForTinyAndHugeNumbers() {
        for (double value : new double[]{0.0, 1.0E-7, 0.25, 1.0, 2.5, 99.999999, 100.0}) {
            TradeRules.ItemMultiplier original = new TradeRules.ItemMultiplier(net.minecraft.resources.ResourceLocation.withDefaultNamespace("iron_sword"), value);
            assertEquals(original, TradeRules.parseItemMultiplier(original.asString()).orElseThrow(), original.asString());
        }
        assertEquals("minecraft:iron_sword=1.0E-7", new TradeRules.ItemMultiplier(
                net.minecraft.resources.ResourceLocation.withDefaultNamespace("iron_sword"), 1.0E-7).asString());
    }

    @Test
    void itemMultipliersToleratesNullElementsAndKeepsTheFirstOfEachId() {
        List<String> lines = new ArrayList<>();
        lines.add(null);
        lines.add("minecraft:iron_sword=2");
        lines.add(null);
        lines.add("minecraft:iron_sword=3");
        lines.add("minecraft:iron_sword=2");
        lines.add("minecraft:bow=0");
        lines.add("minecraft:bow=7");
        Map<net.minecraft.resources.ResourceLocation, Double> m = TradeRules.itemMultipliers(lines);
        assertEquals(2, m.size());
        assertEquals(2.0, m.get(net.minecraft.resources.ResourceLocation.withDefaultNamespace("iron_sword")));
        assertEquals(0.0, m.get(net.minecraft.resources.ResourceLocation.withDefaultNamespace("bow")));
        assertTrue(TradeRules.itemMultipliers(java.util.Collections.singletonList(null)).isEmpty());
    }

    @Test
    void itemMultipliersSkipTheInvalidAndTheFirstRepeatedWins() {
        Map<net.minecraft.resources.ResourceLocation, Double> m = TradeRules.itemMultipliers(
                List.of("minecraft:iron_sword=2", "broken", "minecraft:iron_sword=5", "minecraft:bow=0.5", "minecraft:axe=NaN"));
        assertEquals(2, m.size());
        assertEquals(2.0, m.get(net.minecraft.resources.ResourceLocation.withDefaultNamespace("iron_sword")));
        assertEquals(0.5, m.get(net.minecraft.resources.ResourceLocation.withDefaultNamespace("bow")));
        assertTrue(TradeRules.itemMultipliers(List.of()).isEmpty());
    }

    @Test
    void durabilityPercentIsTheRoundedFactor() {
        assertEquals(100, TradeRules.durabilityPercent(1.0));
        assertEquals(63, TradeRules.durabilityPercent(0.625));
        assertEquals(25, TradeRules.durabilityPercent(0.25));
        assertEquals(0, TradeRules.durabilityPercent(0.0));
    }

    @Test
    void durabilityFactorIsLinearBetweenTheMinimumAndOne() {
        assertEquals(1.0, TradeRules.durabilityFactor(0, 100, 0.25));
        assertEquals(0.25, TradeRules.durabilityFactor(100, 100, 0.25));
        assertEquals(0.625, TradeRules.durabilityFactor(50, 100, 0.25));
        assertEquals(0.0, TradeRules.durabilityFactor(100, 100, 0.0));
    }

    @Test
    void durabilityFactorEdgeCases() {
        assertEquals(1.0, TradeRules.durabilityFactor(5, 0, 0.25), "no durability counts as new");
        assertEquals(1.0, TradeRules.durabilityFactor(5, -3, 0.25));
        assertEquals(1.0, TradeRules.durabilityFactor(-1, 100, 0.25), "negative damage counts as new");
        assertEquals(0.25, TradeRules.durabilityFactor(500, 100, 0.25), "more damage than the maximum is broken");
        assertEquals(1.0, TradeRules.durabilityFactor(77, 100, 1.0), "a minimum of 1 means wear never matters");
    }

    /** The rules do not clamp the minimum: TradeBalance is what keeps it in [0, 1]. Here only what comes out is pinned down. */
    @Test
    void durabilityFactorDoesNotClampAnOutOfRangeMinimum() {
        assertTrue(Double.isNaN(TradeRules.durabilityFactor(50, 100, Double.NaN)));
        assertTrue(Double.isNaN(TradeRules.durabilityFactor(0, 100, Double.NaN)), "even an untouched piece: the minimum takes part in the formula");
        assertEquals(1.0, TradeRules.durabilityFactor(0, 0, Double.NaN), "only a piece with no durability skips it");
        assertEquals(0.0, TradeRules.durabilityFactor(50, 100, -1.0), 1e-12, "-1 + 2 x 0,5");
        assertEquals(1.5, TradeRules.durabilityFactor(50, 100, 2.0), 1e-12, "2 - 1 x 0,5");
        assertEquals(-1.0, TradeRules.durabilityFactor(100, 100, -1.0), "broken gives the minimum as it comes");
    }

    @Test
    void durabilityFactorWithTheBiggestMaxDamage() {
        assertEquals(1.0, TradeRules.durabilityFactor(0, Integer.MAX_VALUE, 0.25));
        assertEquals(0.25, TradeRules.durabilityFactor(Integer.MAX_VALUE, Integer.MAX_VALUE, 0.25));
        assertEquals(0.625, TradeRules.durabilityFactor(Integer.MAX_VALUE / 2, Integer.MAX_VALUE, 0.25), 1e-9);
        assertEquals(1.0, TradeRules.durabilityFactor(Integer.MIN_VALUE, Integer.MAX_VALUE, 0.25), "negative damage is new");
        double almostNew = TradeRules.durabilityFactor(1, Integer.MAX_VALUE, 0.25);
        assertTrue(almostNew < 1.0 && almostNew > 0.999999, "one point of damage: " + almostNew);
    }

    @Test
    void theBalanceRefusesAMinimumOutOfRange() {
        for (double bad : new double[]{Double.NaN, -0.1, 1.1, Double.POSITIVE_INFINITY}) {
            assertThrows(IllegalArgumentException.class, () -> TradeBalancesForTests.withMinDurabilityFactor(bad), String.valueOf(bad));
        }
    }

    /** The wear is a whole percent, rounded to the nearest (half up): 0,995 reads 100 %, so a piece that lost only 0,5 % shows no wear tooltip. */
    @Test
    void durabilityPercentRoundsToTheNearestAndTakesWhatItIsGiven() {
        assertEquals(150, TradeRules.durabilityPercent(1.5), "a factor above 1 is not clamped here");
        assertEquals(-50, TradeRules.durabilityPercent(-0.5), "nor a negative one");
        assertEquals(0, TradeRules.durabilityPercent(Double.NaN), "NaN reads 0");
        assertEquals(100, TradeRules.durabilityPercent(0.995), "decision: 0,995 shows as 100 %, so no wear is announced");
        assertEquals(99, TradeRules.durabilityPercent(0.994));
        assertEquals(99, TradeRules.durabilityPercent(0.99));
        assertEquals(1, TradeRules.durabilityPercent(0.005), "0,005 rounds up to 1 %");
        assertEquals(0, TradeRules.durabilityPercent(0.004));
    }

    /** Damage 0 to 250 on a 250-point piece: the reference never goes up with the damage, never below 1 nor above the new piece's. */
    @Test
    void theReferenceFallsWithTheDamageButNeverBelowOneNorAboveTheNewPiece() {
        long newPiece = TradeRules.reference(ItemRating.EPIC, 1.0, 0, false, TradeRules.durabilityFactor(0, 250, B.minDurabilityFactor()), B);
        assertEquals(40, newPiece);
        long previous = newPiece;
        for (int damage = 0; damage <= 250; damage++) {
            double factor = TradeRules.durabilityFactor(damage, 250, B.minDurabilityFactor());
            long reference = TradeRules.reference(ItemRating.EPIC, 1.0, 0, false, factor, B);
            assertTrue(reference <= previous, "damage " + damage + ": " + reference + " after " + previous);
            assertTrue(reference >= 1, "damage " + damage);
            assertTrue(reference <= newPiece, "damage " + damage);
            previous = reference;
        }
        assertEquals(10, previous, "a broken epic piece is worth 40 x 0,25");
        long sellPrevious = TradeRules.sellPrice(newPiece, 0, B);
        for (int damage = 0; damage <= 250; damage++) {
            long reference = TradeRules.reference(ItemRating.EPIC, 1.0, 0, false, TradeRules.durabilityFactor(damage, 250, B.minDurabilityFactor()), B);
            long price = TradeRules.sellPrice(reference, 0, B);
            assertTrue(price <= sellPrevious && price >= 1, "sale price at damage " + damage + ": " + price);
            sellPrevious = price;
        }
    }

    @Test
    void durabilityEntersTheReferenceBeforeTheSingleRounding() {
        assertEquals(12, TradeRules.reference(ItemRating.RARE, 1.0, 0, false, 1.0, B));
        assertEquals(7, TradeRules.reference(ItemRating.RARE, 1.0, 0, false, 0.625, B));
        assertEquals(3, TradeRules.reference(ItemRating.RARE, 1.0, 0, false, 0.25, B));
        assertEquals(1, TradeRules.reference(ItemRating.NORMAL, 0.5, 0, false, 0.0, B), "a broken piece still pays at least 1");
        assertEquals(TradeRules.reference(ItemRating.EPIC, 2.0, 3, true, B), TradeRules.reference(ItemRating.EPIC, 2.0, 3, true, 1.0, B));
    }

    @Test
    void referenceMatchesExactArithmetic() {
        for (ItemRating rating : ItemRating.values()) {
            for (Map.Entry<String, Double> material : B.materialMultiplier().entrySet()) {
                for (int level = 0; level <= 10; level++) {
                    BigDecimal value = BigDecimal.valueOf(B.basePrice().get(rating))
                            .multiply(BigDecimal.valueOf(material.getValue()))
                            .multiply(BigDecimal.ONE.add(BigDecimal.valueOf(B.enhancementBonus()).multiply(BigDecimal.valueOf(level))));
                    long expected = Math.max(1L, value.setScale(0, RoundingMode.FLOOR).longValueExact());
                    assertEquals(expected, TradeRules.reference(rating, material.getValue(), level, true, B),
                            rating + " " + material.getKey() + " +" + level);
                }
            }
        }
    }

    @Test
    void messagesExistForEveryResult() {
        for (TradeService.Result result : TradeService.Result.values()) {
            Optional<net.minecraft.network.chat.Component> message = TradeService.message(new TradeService.Outcome(
                    result, result == TradeService.Result.REFUSED ? Optional.of(Refusal.LOANED) : Optional.empty(), 0L));
            assertEquals(result == TradeService.Result.SOLD, message.isEmpty(), result.toString());
        }
        assertEquals("message.overgearedmod.trade_stale", key(TradeService.Result.STALE));
        assertEquals("message.overgearedmod.trade_stale", key(TradeService.Result.EMPTY));
        assertEquals("message.overgearedmod.trade_stale", key(TradeService.Result.INVALID_SLOT));
        assertEquals("message.overgearedmod.trade_failed", key(TradeService.Result.FAILED));
        assertEquals("message.overgearedmod.trade_outside", key(TradeService.Result.OUTSIDE_SATISFY));
        assertEquals("message.overgearedmod.trade_refused", key(TradeService.Result.REFUSED));
    }

    private static String key(TradeService.Result result) {
        var message = TradeService.message(new TradeService.Outcome(
                result, result == TradeService.Result.REFUSED ? Optional.of(Refusal.LOANED) : Optional.empty(), 0L)).orElseThrow();
        return ((net.minecraft.network.chat.contents.TranslatableContents) message.getContents()).getKey();
    }

    @Test
    void aDayIs24000Ticks() {
        assertEquals(0, TradeRules.day(23999));
        assertEquals(1, TradeRules.day(24000));
        assertEquals(5, TradeRules.day(5 * 24000L + 7));
    }

    @Test
    void refusalsComeInOrder() {
        assertEquals(Optional.of(Refusal.LOANED), TradeRules.refusal(5, false, false, true));
        assertEquals(Optional.of(Refusal.STACKED), TradeRules.refusal(2, false, false, false));
        assertEquals(Optional.of(Refusal.NO_VALUE), TradeRules.refusal(1, false, false, false));
        assertEquals(Optional.of(Refusal.UNRATED), TradeRules.refusal(1, true, false, false));
        assertTrue(TradeRules.refusal(1, true, true, false).isEmpty());
        assertEquals(Optional.of(Refusal.LOANED), TradeRules.refusal(5, true, true, true));
        assertEquals(Optional.of(Refusal.STACKED), TradeRules.refusal(2, true, false, false));
        assertEquals(Optional.of(Refusal.NO_VALUE), TradeRules.refusal(1, false, true, false));
        // The service cuts before with EMPTY: counts of 0 or less are not a refusal here.
        assertTrue(TradeRules.refusal(0, true, true, false).isEmpty());
        assertTrue(TradeRules.refusal(-1, true, true, false).isEmpty());
    }

    private static String batchKey(TradeService.Batch batch) {
        var message = TradeService.message(batch).orElseThrow();
        return ((net.minecraft.network.chat.contents.TranslatableContents) message.getContents()).getKey();
    }

    @Test
    void messagesForABatch() {
        assertEquals("message.overgearedmod.trade_outside", batchKey(new TradeService.Batch(0, 0, 2, 0, TradeService.Result.OUTSIDE_SATISFY)));
        assertEquals("message.overgearedmod.trade_stale", batchKey(new TradeService.Batch(0, 0, 2, 0, TradeService.Result.INVALID_SLOT)));
        assertEquals("message.overgearedmod.trade_skipped", batchKey(new TradeService.Batch(2, 30, 1, 0, TradeService.Result.STALE)));
        assertEquals("message.overgearedmod.trade_failed", batchKey(new TradeService.Batch(0, 0, 0, 3, TradeService.Result.FAILED)));
        assertEquals("message.overgearedmod.trade_failed", batchKey(new TradeService.Batch(1, 5, 2, 1, TradeService.Result.FAILED)));
        assertEquals("message.overgearedmod.trade_failed", batchKey(new TradeService.Batch(1, 64, 0, 1, TradeService.Result.FAILED)));
        assertTrue(TradeService.message(new TradeService.Batch(3, 45, 0, 0, TradeService.Result.SOLD)).isEmpty());
    }

    private static int placeholders(String text) {
        return text.split("%s", -1).length - 1;
    }

    @Test
    void everyTradeKeyExistsInBothLangFiles() {
        List<String> keys = new ArrayList<>();
        for (Refusal refusal : Refusal.values()) {
            assertTrue(refusal.langKey().startsWith("trade.overgearedmod.refusal."));
            keys.add(refusal.langKey());
        }
        keys.addAll(List.of("notification.overgearedmod.trade.sold", "notification.overgearedmod.trade.sold_many",
                "screen.overgearedmod.trade.confirm_sell", "screen.overgearedmod.trade.confirm_title",
                "screen.overgearedmod.trade.select_all", "screen.overgearedmod.trade.select_none",
                "screen.overgearedmod.trade.sell_selected", "screen.overgearedmod.trade.sell_price", "screen.overgearedmod.trade.empty",
                "message.overgearedmod.trade_refused", "message.overgearedmod.trade_outside",
                "message.overgearedmod.trade_stale", "message.overgearedmod.trade_failed", "message.overgearedmod.trade_skipped",
                "screen.overgearedmod.trade.tip.durability", "screen.overgearedmod.status.rested_cooldown",
                "message.overgearedmod.debug_capsule_cooldown", "message.overgearedmod.debug_capsule"));
        List<String> gone = List.of("screen.overgearedmod.trade.confirm_export", "notification.overgearedmod.trade.exported",
                "screen.overgearedmod.trade.export", "screen.overgearedmod.trade.export_price");
        for (String file : List.of("en_us.json", "es_es.json")) {
            JsonObject lang = LangFiles.load(file);
            for (String key : keys) {
                assertTrue(lang.has(key), key + " missing in " + file);
            }
            for (String key : gone) {
                assertFalse(lang.has(key), key + " should be gone from " + file);
            }
            assertEquals(2, placeholders(lang.get("screen.overgearedmod.trade.confirm_sell").getAsString()), file + ": confirm_sell");
            assertEquals(2, placeholders(lang.get("screen.overgearedmod.trade.sell_selected").getAsString()), file + ": sell_selected");
            assertEquals(0, placeholders(lang.get("screen.overgearedmod.trade.confirm_title").getAsString()), file + ": confirm_title");
            assertEquals(2, placeholders(lang.get("notification.overgearedmod.trade.sold_many").getAsString()), file + ": sold_many");
            assertEquals(1, placeholders(lang.get("message.overgearedmod.trade_skipped").getAsString()), file + ": trade_skipped");
            assertEquals(1, placeholders(lang.get("screen.overgearedmod.trade.tip.durability").getAsString()), file + ": tip.durability");
            assertEquals(1, placeholders(lang.get("screen.overgearedmod.status.rested_cooldown").getAsString()), file + ": rested_cooldown");
            assertEquals(1, placeholders(lang.get("message.overgearedmod.debug_capsule_cooldown").getAsString()), file + ": debug_capsule_cooldown");
            assertEquals(6, placeholders(lang.get("message.overgearedmod.debug_capsule").getAsString()), file + ": debug_capsule");
        }
    }
}
