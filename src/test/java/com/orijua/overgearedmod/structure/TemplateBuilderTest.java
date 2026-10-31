package com.orijua.overgearedmod.structure;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TemplateBuilderTest {
    @Test
    void writesTheStructureBlockFormat() {
        CompoundTag tag = new TemplateBuilder(2, 1, 3).set(1, 0, 2, "minecraft:stone").build(3955);
        assertEquals(3955, tag.getInt("DataVersion"));
        ListTag size = tag.getList("size", Tag.TAG_INT);
        assertEquals(2, size.getInt(0));
        assertEquals(1, size.getInt(1));
        assertEquals(3, size.getInt(2));
        assertTrue(tag.getList("entities", Tag.TAG_COMPOUND).isEmpty());

        CompoundTag block = tag.getList("blocks", Tag.TAG_COMPOUND).getCompound(0);
        ListTag pos = block.getList("pos", Tag.TAG_INT);
        assertEquals(1, pos.getInt(0));
        assertEquals(0, pos.getInt(1));
        assertEquals(2, pos.getInt(2));
        assertEquals("minecraft:stone", tag.getList("palette", Tag.TAG_COMPOUND).getCompound(block.getInt("state")).getString("Name"));
    }

    @Test
    void equalStatesShareOnePaletteEntry() {
        CompoundTag tag = new TemplateBuilder(3, 1, 3)
                .fill(0, 0, 0, 2, 0, 2, "minecraft:oak_log[axis=x]")
                .set(1, 0, 1, "minecraft:oak_log[axis=z]")
                .build(1);
        assertEquals(9, tag.getList("blocks", Tag.TAG_COMPOUND).size(), "the later set replaces the filled block");
        ListTag palette = tag.getList("palette", Tag.TAG_COMPOUND);
        assertEquals(2, palette.size(), "x and z logs, once each");
        assertEquals("x", palette.getCompound(0).getCompound("Properties").getString("axis"));
    }

    @Test
    void propertiesAreReadInAnyOrder() {
        TemplateBuilder piece = new TemplateBuilder(1, 1, 1).set(0, 0, 0, "minecraft:furnace[lit=false, facing=south]");
        assertEquals("minecraft:furnace[facing=south,lit=false]", piece.blockAt(0, 0, 0));
        assertThrows(IllegalArgumentException.class, () -> piece.set(0, 0, 0, "minecraft:furnace[facing]"));
        assertThrows(IllegalArgumentException.class, () -> piece.set(0, 0, 0, "minecraft:furnace[facing=south"));
    }

    @Test
    void unsetPositionsAreStructureVoid() {
        TemplateBuilder piece = new TemplateBuilder(2, 2, 2).set(0, 0, 0, "minecraft:air");
        assertEquals("minecraft:air", piece.blockAt(0, 0, 0), "air clears the terrain");
        assertNull(piece.blockAt(1, 1, 1), "unset keeps it");
        assertEquals(1, piece.build(1).getList("blocks", Tag.TAG_COMPOUND).size());
    }

    @Test
    void blocksMustFitInTheTemplate() {
        TemplateBuilder piece = new TemplateBuilder(2, 2, 2);
        assertThrows(IllegalArgumentException.class, () -> piece.set(2, 0, 0, "minecraft:stone"));
        assertThrows(IllegalArgumentException.class, () -> piece.fill(0, 0, 0, 0, -1, 0, "minecraft:stone"));
        assertThrows(IllegalArgumentException.class, () -> new TemplateBuilder(0, 1, 1));
    }

    @Test
    void jigsawsAndChestsCarryTheirBlockEntity() {
        CompoundTag tag = new TemplateBuilder(2, 1, 1)
                .jigsaw(0, 0, 0, "north_up", "a:name", "a:target", "a:pool", "minecraft:dirt")
                .chest(1, 0, 0, "south", "minecraft:chests/simple_dungeon")
                .build(1);
        ListTag blocks = tag.getList("blocks", Tag.TAG_COMPOUND);
        CompoundTag jigsaw = blocks.getCompound(0).getCompound("nbt");
        assertEquals("minecraft:jigsaw", jigsaw.getString("id"));
        assertEquals("a:target", jigsaw.getString("target"));
        assertEquals("a:pool", jigsaw.getString("pool"));
        assertEquals("minecraft:dirt", jigsaw.getString("final_state"));
        assertEquals(TemplateBuilder.ROLLABLE, jigsaw.getString("joint"));
        CompoundTag chest = blocks.getCompound(1).getCompound("nbt");
        assertEquals("minecraft:chests/simple_dungeon", chest.getString("LootTable"));
        assertFalse(jigsaw.contains("x"), "no world position: the template places it");
    }
}
