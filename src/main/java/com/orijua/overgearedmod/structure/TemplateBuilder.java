package com.orijua.overgearedmod.structure;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntTag;
import net.minecraft.nbt.ListTag;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;

/**
 * Builds a structure template (the .nbt a structure block saves) block by block, from code. Pure: blocks are written as
 * text ("minecraft:oak_log[axis=x]"), so it needs no registries and is unit tested. Datagen writes the result
 * (ModStructurePieceProvider); a piece built in game and saved with a structure block can replace that file later.
 * Positions left unset are structure void: the terrain there is kept. Set "minecraft:air" to clear it.
 */
public final class TemplateBuilder {
    /** Jigsaw connection kinds: rollable pieces may spin around a vertical connection, aligned ones may not. */
    public static final String ROLLABLE = "rollable";

    private static final Comparator<Pos> ORDER = Comparator.comparingInt(Pos::y).thenComparingInt(Pos::z).thenComparingInt(Pos::x);

    private final int sizeX;
    private final int sizeY;
    private final int sizeZ;
    private final Map<Pos, Placed> blocks = new TreeMap<>(ORDER);

    public TemplateBuilder(int sizeX, int sizeY, int sizeZ) {
        if (sizeX <= 0 || sizeY <= 0 || sizeZ <= 0) {
            throw new IllegalArgumentException("template size must be positive: " + sizeX + "x" + sizeY + "x" + sizeZ);
        }
        this.sizeX = sizeX;
        this.sizeY = sizeY;
        this.sizeZ = sizeZ;
    }

    public TemplateBuilder set(int x, int y, int z, String block) {
        return put(x, y, z, BlockSpec.parse(block), null);
    }

    /** Fills the box between two corners (both included); later calls overwrite earlier ones. */
    public TemplateBuilder fill(int x1, int y1, int z1, int x2, int y2, int z2, String block) {
        BlockSpec spec = BlockSpec.parse(block);
        for (int y = Math.min(y1, y2); y <= Math.max(y1, y2); y++) {
            for (int z = Math.min(z1, z2); z <= Math.max(z1, z2); z++) {
                for (int x = Math.min(x1, x2); x <= Math.max(x1, x2); x++) {
                    put(x, y, z, spec, null);
                }
            }
        }
        return this;
    }

    /** A chest that rolls {@code lootTable} when first opened. */
    public TemplateBuilder chest(int x, int y, int z, String facing, String lootTable) {
        CompoundTag nbt = new CompoundTag();
        nbt.putString("id", "minecraft:chest");
        nbt.putString("LootTable", lootTable);
        return put(x, y, z, BlockSpec.parse("minecraft:chest[facing=" + facing + ",type=single,waterlogged=false]"), nbt);
    }

    /**
     * A jigsaw block: where this piece joins another. A jigsaw looks in {@code pool} for a piece with a jigsaw named
     * {@code target} and fastens it in front of itself, facing back; it then turns into {@code finalState}.
     *
     * @param orientation where it faces, as the block's state: "north_up", "east_up"... for side connections
     */
    public TemplateBuilder jigsaw(int x, int y, int z, String orientation, String name, String target, String pool, String finalState) {
        CompoundTag nbt = new CompoundTag();
        nbt.putString("id", "minecraft:jigsaw");
        nbt.putString("name", name);
        nbt.putString("target", target);
        nbt.putString("pool", pool);
        nbt.putString("final_state", finalState);
        nbt.putString("joint", ROLLABLE);
        nbt.putInt("placement_priority", 0);
        nbt.putInt("selection_priority", 0);
        return put(x, y, z, BlockSpec.parse("minecraft:jigsaw[orientation=" + orientation + "]"), nbt);
    }

    /** The block set at a position, or null where it is structure void. */
    public String blockAt(int x, int y, int z) {
        Placed placed = blocks.get(new Pos(x, y, z));
        return placed == null ? null : placed.spec().toString();
    }

    /** The template in the structure-block NBT format, stamped with the game's data version so it isn't upgraded. */
    public CompoundTag build(int dataVersion) {
        Map<BlockSpec, Integer> palette = new LinkedHashMap<>();
        ListTag blockList = new ListTag();
        blocks.forEach((pos, placed) -> {
            CompoundTag entry = new CompoundTag();
            entry.put("pos", ints(pos.x(), pos.y(), pos.z()));
            entry.putInt("state", palette.computeIfAbsent(placed.spec(), spec -> palette.size()));
            if (placed.nbt() != null) {
                entry.put("nbt", placed.nbt().copy());
            }
            blockList.add(entry);
        });
        ListTag paletteList = new ListTag();
        palette.keySet().forEach(spec -> paletteList.add(spec.toTag()));

        CompoundTag tag = new CompoundTag();
        tag.put("size", ints(sizeX, sizeY, sizeZ));
        tag.put("palette", paletteList);
        tag.put("blocks", blockList);
        tag.put("entities", new ListTag());
        tag.putInt("DataVersion", dataVersion);
        return tag;
    }

    private TemplateBuilder put(int x, int y, int z, BlockSpec spec, CompoundTag nbt) {
        if (x < 0 || y < 0 || z < 0 || x >= sizeX || y >= sizeY || z >= sizeZ) {
            throw new IllegalArgumentException("(" + x + ", " + y + ", " + z + ") is outside the "
                    + sizeX + "x" + sizeY + "x" + sizeZ + " template");
        }
        blocks.put(new Pos(x, y, z), new Placed(spec, nbt));
        return this;
    }

    private static ListTag ints(int... values) {
        ListTag list = new ListTag();
        for (int value : values) {
            list.add(IntTag.valueOf(value));
        }
        return list;
    }

    private record Pos(int x, int y, int z) {
    }

    private record Placed(BlockSpec spec, CompoundTag nbt) {
    }

    /** A block state as text: an id plus its properties, sorted so equal states share one palette entry. */
    record BlockSpec(String id, Map<String, String> properties) {
        static BlockSpec parse(String text) {
            int open = text.indexOf('[');
            if (open < 0) {
                return new BlockSpec(text, Map.of());
            }
            if (!text.endsWith("]")) {
                throw new IllegalArgumentException("unclosed properties in " + text);
            }
            Map<String, String> properties = new TreeMap<>();
            for (String pair : text.substring(open + 1, text.length() - 1).split(",")) {
                String[] keyValue = pair.split("=");
                if (keyValue.length != 2) {
                    throw new IllegalArgumentException("bad property '" + pair + "' in " + text);
                }
                properties.put(keyValue[0].trim(), keyValue[1].trim());
            }
            return new BlockSpec(text.substring(0, open), Map.copyOf(properties));
        }

        CompoundTag toTag() {
            CompoundTag tag = new CompoundTag();
            tag.putString("Name", id);
            if (!properties.isEmpty()) {
                CompoundTag props = new CompoundTag();
                new TreeMap<>(properties).forEach(props::putString);
                tag.put("Properties", props);
            }
            return tag;
        }

        @Override
        public String toString() {
            if (properties.isEmpty()) {
                return id;
            }
            StringBuilder text = new StringBuilder(id).append('[');
            new TreeMap<>(properties).forEach((key, value) -> text.append(key).append('=').append(value).append(','));
            text.setLength(text.length() - 1);
            return text.append(']').toString();
        }
    }
}
