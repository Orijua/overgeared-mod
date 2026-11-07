package com.orijua.overgearedmod.feature.world;

/**
 * Reference pixels of the West Continent's source map (4096x3072) shared by the unit tests. The game tests
 * (src/main/.../gametest) cannot see src/test and repeat the ones they use (Lilton 911,754; the Arctic Glacier label
 * 1382,178 is {@code NorthEndCave.MAP_PIXEL_X/Z}); keep them in step with this class and {@code tools/satisfy_map/test_convert.py}.
 */
final class WestMapReference {
    static final int BAIRAN_X = 1772;
    static final int BAIRAN_Z = 458;
    /** A spot in the Travian Sierra: mountain. */
    static final int TRAVIAN_X = 2400;
    static final int TRAVIAN_Z = 1500;
    /** A spot in the Red Sea, east of the continent: deep sea. */
    static final int RED_SEA_X = 3600;
    static final int RED_SEA_Z = 1500;
    /** Lilton Desert, in the west. */
    static final int LILTON_X = 911;
    static final int LILTON_Z = 754;

    private WestMapReference() {
    }
}
