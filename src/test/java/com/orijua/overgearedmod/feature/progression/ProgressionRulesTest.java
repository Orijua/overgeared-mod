package com.orijua.overgearedmod.feature.progression;

import com.orijua.overgearedmod.core.config.Balance;
import net.minecraft.network.chat.Component;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProgressionRulesTest {
    private static final Balance B = Balance.DEFAULT;
    private static final StatSource CLASS = new StatSource(Component.literal("class"));
    private static final StatSource TITLE = new StatSource(Component.literal("title"));

    @Test
    void xpCurveGrowsWithLevel() {
        assertEquals(75, ProgressionRules.xpToNextLevel(B, 1));
        assertEquals(100, ProgressionRules.xpToNextLevel(B, 2));
    }

    @Test
    void addXpResolvesSeveralLevelUps() {
        // Level 1 → 2 costs 75 XP, 2 → 3 costs 100.
        ProgressionRules.XpResult result = ProgressionRules.addXp(B, PlayerStats.DEFAULT, 180);
        assertEquals(3, result.stats().level());
        assertEquals(5, result.stats().xp());
        assertEquals(10, result.stats().freePoints());
        assertEquals(2, result.levelsGained());
    }

    @Test
    void addXpLevelsUpExactlyAtTheThreshold() {
        ProgressionRules.XpResult below = ProgressionRules.addXp(B, PlayerStats.DEFAULT, 74);
        assertEquals(1, below.stats().level());
        assertEquals(74, below.stats().xp());
        assertEquals(0, below.levelsGained());

        ProgressionRules.XpResult exact = ProgressionRules.addXp(B, PlayerStats.DEFAULT, 75);
        assertEquals(2, exact.stats().level());
        assertEquals(0, exact.stats().xp());
        assertEquals(1, exact.levelsGained());
    }

    @Test
    void negativeXpIsIgnored() {
        assertEquals(PlayerStats.DEFAULT, ProgressionRules.addXp(B, PlayerStats.DEFAULT, -50).stats());
    }

    @Test
    void allocateSpendsOnePoint() {
        PlayerStats stats = PlayerStats.DEFAULT.withProgress(2, 0, 5);
        PlayerStats spent = ProgressionRules.allocate(stats, StatType.DEXTERITY);
        assertEquals(1, spent.dexterity());
        assertEquals(4, spent.freePoints());
        assertSame(PlayerStats.DEFAULT, ProgressionRules.allocate(PlayerStats.DEFAULT, StatType.DEXTERITY), "no points, no change");
    }

    @Test
    void allocateManySpendsUpToWhatIsLeft() {
        PlayerStats stats = PlayerStats.DEFAULT.withProgress(30, 0, 25);
        PlayerStats ten = ProgressionRules.allocate(stats, StatType.STRENGTH, 10);
        assertEquals(10, ten.strength());
        assertEquals(15, ten.freePoints());
        PlayerStats hundred = ProgressionRules.allocate(ten, StatType.STRENGTH, 100);
        assertEquals(25, hundred.strength(), "only the 15 left are spent");
        assertEquals(0, hundred.freePoints());
        assertSame(stats, ProgressionRules.allocate(stats, StatType.STRENGTH, 0), "zero changes nothing");
        assertSame(stats, ProgressionRules.allocate(stats, StatType.STRENGTH, -5), "a negative amount from a bad client changes nothing");
    }

    @Test
    void refundAllReturnsAllocatedPoints() {
        PlayerStats stats = PlayerStats.DEFAULT.with(StatType.STRENGTH, 4).with(StatType.DEXTERITY, 3).withProgress(5, 0, 2);
        PlayerStats refunded = ProgressionRules.refundAll(stats);
        assertEquals(9, refunded.freePoints(), "2 free + 7 allocated");
        assertEquals(0, refunded.strength());
        assertEquals(0, refunded.dexterity());
        assertEquals(5, refunded.level());
    }

    @Test
    void effectiveAddsBonusesOnTopOfAllocatedStats() {
        PlayerStats stats = PlayerStats.DEFAULT.with(StatType.STRENGTH, 2).withProgress(4, 10, 1);
        List<StatModifier> modifiers = new ArrayList<>(StatModifier.allocated(stats));
        modifiers.addAll(StatModifier.fromBonuses(CLASS, Map.of(StatType.STRENGTH, 3)));
        modifiers.addAll(StatModifier.fromBonuses(TITLE, Map.of(StatType.DEXTERITY, 1)));
        PlayerStats total = ProgressionRules.effective(stats, modifiers);
        assertEquals(5, total.strength());
        assertEquals(1, total.dexterity());
        assertEquals(0, total.agility());
        assertEquals(4, total.level(), "level, XP and points are kept");
        assertEquals(1, total.freePoints());
    }

    @Test
    void percentagesApplyAfterEveryFlatBonus() {
        List<StatModifier> modifiers = List.of(
                StatModifier.multiply(TITLE, StatType.STRENGTH, 0.5), // listed first on purpose: order must not matter
                StatModifier.add(StatSource.ALLOCATED, StatType.STRENGTH, 6),
                StatModifier.add(CLASS, StatType.STRENGTH, 4));
        assertEquals(15, ProgressionRules.total(StatType.STRENGTH, modifiers), "(6 + 4) * 1.5");
    }

    @Test
    void percentagesAddUpBeforeApplying() {
        List<StatModifier> modifiers = List.of(
                StatModifier.add(StatSource.ALLOCATED, StatType.AGILITY, 10),
                StatModifier.multiply(CLASS, StatType.AGILITY, 0.1),
                StatModifier.multiply(TITLE, StatType.AGILITY, 0.2));
        assertEquals(13, ProgressionRules.total(StatType.AGILITY, modifiers), "10 * (1 + 0.1 + 0.2), not 10 * 1.1 * 1.2");
    }

    @Test
    void totalRoundsDownAndIgnoresOtherStats() {
        List<StatModifier> modifiers = List.of(
                StatModifier.add(StatSource.ALLOCATED, StatType.STAMINA, 7),
                StatModifier.multiply(CLASS, StatType.STAMINA, 0.1),
                StatModifier.add(CLASS, StatType.STRENGTH, 50));
        assertEquals(7, ProgressionRules.total(StatType.STAMINA, modifiers), "7 * 1.1 = 7.7 rounds down");
        assertEquals(0, ProgressionRules.total(StatType.INTELLIGENCE, List.of()), "no modifiers, no points");
    }

    @Test
    void killXpRewardsToughMobsAndPunishesSpawners() {
        int zombie = ProgressionRules.killXp(B, new ProgressionRules.Victim(20, 3, 2, false, false));
        int armoredBrute = ProgressionRules.killXp(B, new ProgressionRules.Victim(20, 8, 10, false, false));
        int farmedZombie = ProgressionRules.killXp(B, new ProgressionRules.Victim(20, 3, 2, true, false));
        int eliteZombie = ProgressionRules.killXp(B, new ProgressionRules.Victim(20, 3, 2, false, true));
        assertEquals(28, zombie);
        assertTrue(armoredBrute > zombie);
        assertEquals(7, farmedZombie, "a quarter");
        assertEquals(84, eliteZombie, "x3");
    }

    @Test
    void killXpIsAtLeastOne() {
        assertEquals(1, ProgressionRules.killXp(B, new ProgressionRules.Victim(0, 0, 0, true, false)));
    }

    @Test
    void dyingCostsNoXpByDefault() {
        PlayerStats stats = PlayerStats.DEFAULT.withProgress(4, 50, 0);
        assertEquals(stats, ProgressionRules.applyDeathPenalty(B, stats), "the canon has no XP penalty for dying");
    }

    @Test
    void deathCostsPartOfTheLevelXpButNeverALevel() {
        Balance penalty = B.withDeathXpLoss(0.10);
        PlayerStats after = ProgressionRules.applyDeathPenalty(penalty, PlayerStats.DEFAULT.withProgress(4, 50, 0));
        assertEquals(45, after.xp());
        assertEquals(4, after.level());
        assertEquals(PlayerStats.DEFAULT, ProgressionRules.applyDeathPenalty(penalty, PlayerStats.DEFAULT));
    }

    @Test
    void deathAtLevelOneKeepsTheLevel() {
        PlayerStats after = ProgressionRules.applyDeathPenalty(B.withDeathXpLoss(0.10), PlayerStats.DEFAULT.withProgress(1, 50, 0));
        assertEquals(1, after.level());
        assertEquals(45, after.xp(), "same share lost as at any other level");
    }

    @Test
    void smithingLevelIsTheSquareRootOfXpOverTen() {
        assertEquals(0, ProgressionRules.smithingLevel(9));
        assertEquals(1, ProgressionRules.smithingLevel(10));
        assertEquals(3, ProgressionRules.smithingLevel(90));
        assertEquals(0, ProgressionRules.smithingLevel(-5));
    }

    @Test
    void smithingXpForALevelIsWhereThatLevelStarts() {
        assertEquals(0, ProgressionRules.smithingXpFor(0));
        for (int level : new int[]{1, 7, 50, 90}) {
            assertEquals(level, ProgressionRules.smithingLevel(ProgressionRules.smithingXpFor(level)));
            assertEquals(level - 1, ProgressionRules.smithingLevel(ProgressionRules.smithingXpFor(level) - 1), "just below it");
        }
        assertEquals(25_000, ProgressionRules.smithingXpFor(50));
    }

    @Test
    void manaScalesWithIntelligence() {
        assertEquals(20.0f, ProgressionRules.maxMana(B, 0));
        assertEquals(70.0f, ProgressionRules.maxMana(B, 10));
        assertEquals(3.0f, ProgressionRules.manaRegenPerSecond(B, 10), 1e-6f);
    }

    @Test
    void aTitleBonusRaisesSmithingXp() {
        assertEquals(110, ProgressionRules.boostedSmithingXp(100, 0.1));
        assertEquals(17, ProgressionRules.boostedSmithingXp(15, 0.1));
        assertEquals(15, ProgressionRules.boostedSmithingXp(15, 0));
        assertEquals(0, ProgressionRules.boostedSmithingXp(0, 0.1));
    }
}
