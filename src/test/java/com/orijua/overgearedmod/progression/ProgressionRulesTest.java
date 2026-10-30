package com.orijua.overgearedmod.progression;

import com.orijua.overgearedmod.Balance;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProgressionRulesTest {
    private static final Balance B = Balance.DEFAULT;

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
    void refundAllReturnsAllocatedPoints() {
        PlayerStats stats = PlayerStats.DEFAULT.with(StatType.STRENGTH, 4).with(StatType.DEXTERITY, 3).withProgress(5, 0, 2);
        PlayerStats refunded = ProgressionRules.refundAll(stats);
        assertEquals(9, refunded.freePoints(), "2 free + 7 allocated");
        assertEquals(0, refunded.strength());
        assertEquals(0, refunded.dexterity());
        assertEquals(5, refunded.level());
    }

    @Test
    void withBonusesAddsOnTopOfAllocatedStats() {
        PlayerStats stats = PlayerStats.DEFAULT.with(StatType.STRENGTH, 2);
        PlayerStats total = ProgressionRules.withBonuses(stats, List.of(Map.of(StatType.STRENGTH, 3), Map.of(StatType.DEXTERITY, 1)));
        assertEquals(5, total.strength());
        assertEquals(1, total.dexterity());
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
    void deathCostsPartOfTheLevelXpButNeverALevel() {
        PlayerStats after = ProgressionRules.applyDeathPenalty(B, PlayerStats.DEFAULT.withProgress(4, 50, 0));
        assertEquals(45, after.xp());
        assertEquals(4, after.level());
        assertEquals(PlayerStats.DEFAULT, ProgressionRules.applyDeathPenalty(B, PlayerStats.DEFAULT));
    }

    @Test
    void smithingLevelIsTheSquareRootOfXpOverTen() {
        assertEquals(0, ProgressionRules.smithingLevel(9));
        assertEquals(1, ProgressionRules.smithingLevel(10));
        assertEquals(3, ProgressionRules.smithingLevel(90));
        assertEquals(0, ProgressionRules.smithingLevel(-5));
    }

    @Test
    void manaScalesWithIntelligence() {
        assertEquals(20.0f, ProgressionRules.maxMana(B, 0));
        assertEquals(70.0f, ProgressionRules.maxMana(B, 10));
        assertEquals(3.0f, ProgressionRules.manaRegenPerSecond(B, 10), 1e-6f);
    }
}
