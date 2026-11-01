package com.orijua.overgearedmod.feature.npc;

/**
 * How far a smith has mastered his trade (canon: Smith is a beginner blacksmith, Khan an advanced one, cap. 768). It
 * follows his smithing level; the thresholds are our own and belong to blacksmithing (other trades may get their own).
 * Mastery is not the Craftsman recognition: being a Master only makes it possible.
 */
public enum MasteryStage {
    BEGINNER(0),
    INTERMEDIATE(20),
    ADVANCED(40),
    MASTER(60);

    private final int fromLevel;

    MasteryStage(int fromLevel) {
        this.fromLevel = fromLevel;
    }

    public int fromLevel() {
        return fromLevel;
    }

    public static MasteryStage of(int smithingLevel) {
        MasteryStage stage = BEGINNER;
        for (MasteryStage candidate : values()) {
            if (smithingLevel >= candidate.fromLevel) {
                stage = candidate;
            }
        }
        return stage;
    }

    public boolean atLeast(MasteryStage other) {
        return ordinal() >= other.ordinal();
    }

    public String key() {
        return name().toLowerCase();
    }
}
