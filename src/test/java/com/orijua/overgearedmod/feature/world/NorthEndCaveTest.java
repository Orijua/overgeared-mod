package com.orijua.overgearedmod.feature.world;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NorthEndCaveTest {
    private static final Map<String, TemplateBuilder> PIECES = NorthEndCave.pieces();

    @Test
    void eachPieceLeadsToTheNextAndTheSanctumEndsIt() {
        assertEquals(List.of(NorthEndCave.ENTRANCE, NorthEndCave.DESCENT, NorthEndCave.HALL, NorthEndCave.SANCTUM), List.copyOf(PIECES.keySet()));
        assertEquals(List.of("overgearedmod:north_end_cave/descent"), pools(NorthEndCave.ENTRANCE));
        assertEquals(List.of("overgearedmod:north_end_cave/hall", "minecraft:empty"), pools(NorthEndCave.DESCENT));
        assertEquals(List.of("overgearedmod:north_end_cave/sanctum", "minecraft:empty"), pools(NorthEndCave.HALL));
        assertEquals(List.of("minecraft:empty"), pools(NorthEndCave.SANCTUM));
        assertTrue(StructurePieces.all().keySet().containsAll(PIECES.keySet()), "datagen writes them");
    }

    @Test
    void theDescentGoesDownOneStepPerBlock() {
        assertEquals(1, NorthEndCave.stepFloor(0), "flat at the bottom");
        assertEquals(1, NorthEndCave.stepFloor(4));
        assertEquals(2, NorthEndCave.stepFloor(5));
        assertEquals(NorthEndCave.DROP + 1, NorthEndCave.stepFloor(4 + NorthEndCave.DROP), "and flat at the top");
        assertEquals(NorthEndCave.DROP + 1, NorthEndCave.stepFloor(99));
        TemplateBuilder descent = NorthEndCave.descent();
        for (int z = 1; z < 4 + NorthEndCave.DROP + 2; z++) {
            int floor = NorthEndCave.stepFloor(z);
            for (int y = floor + 1; y <= floor + 3; y++) {
                String block = descent.blockAt(2, y, z);
                assertTrue(block.equals("minecraft:air") || block.startsWith("minecraft:soul_lantern"), "headroom at z " + z + ", y " + y + ": " + block);
            }
        }
        int pit = 4 + NorthEndCave.DROP / 2;
        assertEquals("minecraft:powder_snow", descent.blockAt(2, NorthEndCave.stepFloor(pit), pit), "the trap");
        assertTrue(descent.blockAt(2, NorthEndCave.stepFloor(pit) - 1, pit).startsWith("minecraft:"), "only one block deep");
        assertEquals("minecraft:spawner", descent.blockAt(0, 2, 2));
    }

    @Test
    void theHallIsFullOfLockedArmoredStands() {
        TemplateBuilder hall = NorthEndCave.hall();
        List<String> ids = hall.entityIds();
        assertEquals(10, ids.size(), "one on each of the four piles and six along the aisle");
        assertTrue(ids.stream().allMatch("minecraft:armor_stand"::equals));
        ListTag entities = hall.build(1).getList("entities", Tag.TAG_COMPOUND);
        for (int i = 0; i < entities.size(); i++) {
            CompoundTag stand = entities.getCompound(i).getCompound("nbt");
            assertTrue(stand.getBoolean("Invulnerable"), "unbreakable");
            assertEquals(NorthEndCave.ALL_SLOTS_DISABLED, stand.getInt("DisabledSlots"), "nothing can be taken or swapped");
            assertEquals(NorthEndCave.RELIC_TAG, stand.getList("Tags", Tag.TAG_STRING).getString(0));
            assertEquals(4, stand.getList("ArmorItems", Tag.TAG_COMPOUND).size());
            assertTrue(stand.getList("HandItems", Tag.TAG_COMPOUND).getCompound(0).getString("id").endsWith("_sword"));
        }
        assertEquals("minecraft:air", hall.blockAt(11, 3, 11), "the aisle is clear");
    }

    @Test
    void theBookWaitsOnTheGoldenTable() {
        assertEquals("overgearedmod:golden_table[book=true]", NorthEndCave.sanctum().blockAt(6, 2, 6));
    }

    private static List<String> pools(String piece) {
        return Jigsaws.of(PIECES.get(piece)).stream().map(jigsaw -> jigsaw.getString("pool")).toList();
    }
}
