package com.orijua.overgearedmod.structure;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BairanTest {
    private static final Map<String, TemplateBuilder> PIECES = Bairan.pieces();

    @Test
    void theSquarePointsEachSideAtItsPool() {
        List<CompoundTag> exits = Jigsaws.of(Bairan.plaza());
        assertEquals(6, exits.size());
        Set<String> pools = exits.stream().map(jigsaw -> jigsaw.getString("pool")).collect(Collectors.toSet());
        assertEquals(Set.of("overgearedmod:bairan/smithy", "overgearedmod:bairan/houses", "overgearedmod:bairan/chief_house",
                "overgearedmod:bairan/mine"), pools);
        assertEquals(1, exits.stream().filter(jigsaw -> jigsaw.getString("pool").endsWith("/smithy")).count(), "one smithy");
        assertEquals(3, exits.stream().filter(jigsaw -> jigsaw.getString("pool").endsWith("/houses")).count(), "three houses");
    }

    @Test
    void everyBuildingJoinsTheSquareOnce() {
        String wanted = Jigsaws.of(Bairan.plaza()).getFirst().getString("target");
        PIECES.forEach((path, piece) -> {
            if (path.equals(Bairan.PLAZA)) {
                return;
            }
            List<CompoundTag> ends = Jigsaws.of(piece);
            assertEquals(1, ends.size(), path + ": one jigsaw, back to the square");
            assertEquals(wanted, ends.getFirst().getString("name"), path);
            assertEquals("minecraft:empty", ends.getFirst().getString("pool"), path + " doesn't grow the village further");
        });
    }

    @Test
    void smithWorksInTheSmithy() {
        TemplateBuilder smithy = Bairan.smithy();
        assertEquals(List.of("overgearedmod:blacksmith"), smithy.entityIds());
        CompoundTag smith = smithy.build(1).getList("entities", Tag.TAG_COMPOUND).getCompound(0).getCompound("nbt");
        assertTrue(smith.getString("CustomName").contains("\"" + Bairan.SMITH_NAME + "\""));
        assertEquals("overgearedmod:smithing_anvil", smithy.blockAt(3, 1, 3));
    }

    @Test
    void theChiefIsInTheChiefsHouse() {
        assertEquals(List.of("overgearedmod:village_chief"), Bairan.chiefHouse().entityIds());
    }

    @Test
    void theMineHasIronToDig() {
        TemplateBuilder mine = Bairan.mine();
        long iron = mine.build(1).getList("palette", Tag.TAG_COMPOUND).stream()
                .filter(state -> ((CompoundTag) state).getString("Name").equals("minecraft:iron_ore")).count();
        assertEquals(1, iron, "iron ore is in the palette");
        assertEquals("minecraft:iron_ore", mine.blockAt(5, 1, 0), "on the back wall of the room");
        assertEquals("minecraft:air", mine.blockAt(5, 2, 5), "and the tunnel to it is open");
    }

    @Test
    void piecesAreListedByTemplatePath() {
        assertEquals(List.of(Bairan.PLAZA, Bairan.SMITHY, Bairan.CHIEF_HOUSE, Bairan.HOUSE, Bairan.GARDEN_HOUSE, Bairan.MINE),
                List.copyOf(PIECES.keySet()));
        assertTrue(StructurePieces.all().keySet().containsAll(PIECES.keySet()), "datagen writes them");
    }
}
