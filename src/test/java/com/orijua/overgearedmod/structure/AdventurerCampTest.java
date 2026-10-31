package com.orijua.overgearedmod.structure;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AdventurerCampTest {
    /** The block entity data of every jigsaw in a piece. */
    private static List<CompoundTag> jigsaws(TemplateBuilder piece) {
        List<CompoundTag> found = new ArrayList<>();
        ListTag blocks = piece.build(1).getList("blocks", Tag.TAG_COMPOUND);
        for (int i = 0; i < blocks.size(); i++) {
            CompoundTag nbt = blocks.getCompound(i).getCompound("nbt");
            if ("minecraft:jigsaw".equals(nbt.getString("id"))) {
                found.add(nbt);
            }
        }
        return found;
    }

    @Test
    void theCenterOpensOnAllFourSides() {
        assertEquals("minecraft:jigsaw[orientation=north_up]", AdventurerCamp.center().blockAt(3, 0, 0));
        assertEquals("minecraft:jigsaw[orientation=south_up]", AdventurerCamp.center().blockAt(3, 0, 6));
        assertEquals("minecraft:jigsaw[orientation=west_up]", AdventurerCamp.center().blockAt(0, 0, 3));
        assertEquals("minecraft:jigsaw[orientation=east_up]", AdventurerCamp.center().blockAt(6, 0, 3));
        assertEquals(4, jigsaws(AdventurerCamp.center()).size());
    }

    @Test
    void everySidePieceFitsTheCentersJigsaws() {
        String wanted = jigsaws(AdventurerCamp.center()).getFirst().getString("target");
        for (TemplateBuilder side : List.of(AdventurerCamp.tent(), AdventurerCamp.forge())) {
            List<CompoundTag> ends = jigsaws(side);
            assertEquals(1, ends.size(), "one jigsaw, back to the center, so the camp stops growing");
            assertEquals(wanted, ends.getFirst().getString("name"));
            assertEquals("minecraft:empty", ends.getFirst().getString("pool"));
            assertEquals("minecraft:jigsaw[orientation=south_up]", side.blockAt(2, 0, 4), "on the south edge, facing out");
        }
    }

    @Test
    void theTentChestRollsRatedLoot() {
        assertTrue(AdventurerCamp.tent().blockAt(2, 1, 1).startsWith("minecraft:chest["));
        assertTrue(AdventurerCamp.forge().blockAt(2, 1, 1).endsWith(":smithing_anvil"), "the forge has the mod's anvil");
    }

    @Test
    void piecesAreListedByTemplatePath() {
        assertEquals(List.of(AdventurerCamp.CENTER, AdventurerCamp.TENT, AdventurerCamp.FORGE),
                List.copyOf(AdventurerCamp.pieces().keySet()));
    }
}
