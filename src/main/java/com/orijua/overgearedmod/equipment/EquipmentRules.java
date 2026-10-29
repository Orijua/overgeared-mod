package com.orijua.overgearedmod.equipment;

import java.util.ArrayList;
import java.util.List;
import java.util.function.DoubleSupplier;
import java.util.function.ToIntFunction;

/** Pure equipment formulas (project design): option rolls and enhancement odds. */
public final class EquipmentRules {
    public static final int MAX_ENHANCEMENT = 10;
    public static final double ATTACK_PER_ENHANCEMENT = 0.5;

    private EquipmentRules() {
    }

    /** Normal 0, Rare/Epic 1, Unique 2, Legendary/Myth 3. */
    public static int optionCount(ItemRating rating) {
        return switch (rating) {
            case NORMAL -> 0;
            case RARE, EPIC -> 1;
            case UNIQUE -> 2;
            case LEGENDARY, MYTH -> 3;
        };
    }

    /** Better ratings roll closer to the top of the option's range. */
    public static float rollValue(float min, float max, ItemRating rating, double random01) {
        double floor = rating.ordinal() / (double) (ItemRating.values().length + 1);
        double t = floor + (1 - floor) * random01;
        return (float) (min + (max - min) * t);
    }

    /** Weighted pick of {@code count} distinct entries. */
    public static <T> List<T> pickWeighted(List<T> pool, ToIntFunction<T> weight, int count, DoubleSupplier random01) {
        List<T> remaining = new ArrayList<>(pool);
        List<T> picked = new ArrayList<>();
        while (picked.size() < count && !remaining.isEmpty()) {
            int total = remaining.stream().mapToInt(weight).sum();
            double target = random01.getAsDouble() * total;
            for (int i = 0; i < remaining.size(); i++) {
                target -= weight.applyAsInt(remaining.get(i));
                if (target < 0 || i == remaining.size() - 1) {
                    picked.add(remaining.remove(i));
                    break;
                }
            }
        }
        return picked;
    }

    /** 95% at +0, about 10 points less per level, a little better with DEX; never below 5%. */
    public static double enhancementChance(int currentLevel, int dexterity) {
        return Math.clamp(0.95 - 0.09 * currentLevel + 0.005 * dexterity, 0.05, 0.95);
    }
}
