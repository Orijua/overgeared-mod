package com.orijua.overgearedmod.structure;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;

import java.util.ArrayList;
import java.util.List;

/** Reads the jigsaws out of a built piece, for the structure tests. */
final class Jigsaws {
    private Jigsaws() {
    }

    /** The block entity data of every jigsaw in a piece. */
    static List<CompoundTag> of(TemplateBuilder piece) {
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
}
