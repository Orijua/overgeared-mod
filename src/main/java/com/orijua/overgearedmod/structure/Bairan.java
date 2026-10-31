package com.orijua.overgearedmod.structure;

import com.orijua.overgearedmod.Overgearedmod;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Bairan, the village where Grid starts as a blacksmith (canon: caps. 10 and 22). One per world, near spawn
 * (structure_set bairan: concentric rings with a single position). The place, Smith the blacksmith and the mountain
 * mine are canon; the layout, the chief and how the buildings look are our own design.
 * <p>
 * A square with a well and the firewood pile, and around it, joined by jigsaws: Smith's smithy (north), houses
 * (north and south), the chief's house (east) and the mine (west). Each side points at its own pool, so the smithy, the
 * chief and the mine always appear once.
 */
public final class Bairan {
    public static final ResourceLocation STRUCTURE = Overgearedmod.id("bairan");
    public static final String PLAZA = "bairan/plaza";
    public static final String SMITHY = "bairan/smithy";
    public static final String CHIEF_HOUSE = "bairan/chief_house";
    public static final String HOUSE = "bairan/house";
    public static final String GARDEN_HOUSE = "bairan/garden_house";
    public static final String MINE = "bairan/mine";
    /** Bairan's blacksmith (canon), the mod's blacksmith NPC under his name. */
    public static final String SMITH_NAME = "Smith";

    private static final String PLAZA_JOINT = Overgearedmod.MODID + ":bairan_plaza";
    /** Every building joins the square through a jigsaw with this name, in the middle of its front. */
    private static final String ENTRY = Overgearedmod.MODID + ":bairan_entry";
    private static final String NO_POOL = "minecraft:empty";
    private static final String PATH = "minecraft:dirt_path";

    private Bairan() {
    }

    /** Every piece by its template path (data/overgearedmod/structure/&lt;path&gt;.nbt). */
    public static Map<String, TemplateBuilder> pieces() {
        Map<String, TemplateBuilder> pieces = new LinkedHashMap<>();
        pieces.put(PLAZA, plaza());
        pieces.put(SMITHY, smithy());
        pieces.put(CHIEF_HOUSE, chiefHouse());
        pieces.put(HOUSE, house());
        pieces.put(GARDEN_HOUSE, gardenHouse());
        pieces.put(MINE, mine());
        return pieces;
    }

    /** 15x15 square: the well in the middle, Smith's firewood pile and a chopping block, four lamp posts. */
    static TemplateBuilder plaza() {
        TemplateBuilder piece = new TemplateBuilder(15, 6, 15)
                .fill(0, 0, 0, 14, 0, 14, PATH)
                .fill(0, 1, 0, 14, 5, 14, "minecraft:air")
                .fill(5, 0, 5, 9, 0, 9, "minecraft:cobblestone")
                // The well.
                .fill(6, 1, 6, 8, 1, 8, "minecraft:cobblestone")
                .set(7, 1, 7, "minecraft:water[level=0]")
                .fill(6, 2, 6, 6, 3, 6, "minecraft:oak_fence")
                .fill(8, 2, 6, 8, 3, 6, "minecraft:oak_fence")
                .fill(6, 2, 8, 6, 3, 8, "minecraft:oak_fence")
                .fill(8, 2, 8, 8, 3, 8, "minecraft:oak_fence")
                .fill(6, 4, 6, 8, 4, 8, "minecraft:oak_slab[type=bottom]")
                // Firewood: in the canon, Smith's first job for Grid is chopping it.
                .fill(1, 1, 9, 2, 2, 11, "minecraft:oak_log[axis=z]")
                .set(4, 1, 12, "minecraft:oak_log[axis=y]");
        lampPost(piece, 4, 4);
        lampPost(piece, 10, 4);
        lampPost(piece, 4, 10);
        lampPost(piece, 10, 10);
        piece.jigsaw(3, 0, 0, "north_up", PLAZA_JOINT, ENTRY, pool("smithy"), PATH);
        piece.jigsaw(11, 0, 0, "north_up", PLAZA_JOINT, ENTRY, pool("houses"), PATH);
        piece.jigsaw(3, 0, 14, "south_up", PLAZA_JOINT, ENTRY, pool("houses"), PATH);
        piece.jigsaw(11, 0, 14, "south_up", PLAZA_JOINT, ENTRY, pool("houses"), PATH);
        piece.jigsaw(14, 0, 7, "east_up", PLAZA_JOINT, ENTRY, pool("chief_house"), PATH);
        piece.jigsaw(0, 0, 7, "west_up", PLAZA_JOINT, ENTRY, pool("mine"), PATH);
        return piece;
    }

    /** Smith's smithy: the mod's smithing anvil, a blast furnace under a smoking chimney, his tool chest, and Smith. */
    static TemplateBuilder smithy() {
        TemplateBuilder piece = new TemplateBuilder(7, 7, 9)
                .fill(0, 0, 0, 6, 0, 7, "minecraft:cobblestone")
                .fill(0, 0, 8, 6, 0, 8, PATH)
                .fill(0, 1, 0, 6, 6, 8, "minecraft:air")
                .walls(0, 1, 0, 6, 1, 7, "minecraft:cobblestone")
                .walls(0, 2, 0, 6, 3, 7, "minecraft:spruce_planks")
                .fill(0, 4, 0, 6, 4, 7, "minecraft:spruce_planks")
                .fill(1, 5, 1, 5, 5, 6, "minecraft:spruce_slab[type=bottom]")
                .fill(1, 4, 1, 1, 5, 1, "minecraft:bricks")
                .set(1, 6, 1, "minecraft:campfire[lit=true,signal_fire=false]")
                .set(0, 2, 3, "minecraft:glass_pane")
                .set(6, 2, 3, "minecraft:glass_pane")
                .set(1, 2, 7, "minecraft:glass_pane")
                .set(5, 2, 7, "minecraft:glass_pane")
                .set(1, 1, 1, "minecraft:blast_furnace[facing=south]")
                .set(2, 1, 1, "minecraft:furnace[facing=south]")
                .chest(5, 1, 1, "south", "minecraft:chests/village/village_toolsmith")
                .set(3, 1, 3, Overgearedmod.MODID + ":smithing_anvil")
                .set(5, 1, 3, "minecraft:grindstone[face=floor,facing=north]")
                .set(1, 1, 4, "minecraft:smithing_table")
                .set(3, 3, 4, "minecraft:lantern[hanging=true]");
        pillars(piece, 6, 7, "minecraft:spruce_log");
        door(piece, 3, 7, "minecraft:spruce_door");
        CompoundTag smith = new CompoundTag();
        smith.putString("CustomName", "{\"text\":\"" + SMITH_NAME + "\"}");
        piece.entity(3, 1, 5, Overgearedmod.MODID + ":blacksmith", smith);
        return front(piece, 3, 8);
    }

    /** The village chief's house: bed, bookshelves and a lectern, a table, and the chief. */
    static TemplateBuilder chiefHouse() {
        TemplateBuilder piece = new TemplateBuilder(9, 7, 9)
                .fill(0, 0, 0, 8, 0, 7, "minecraft:oak_planks")
                .fill(0, 0, 8, 8, 0, 8, PATH)
                .fill(0, 1, 0, 8, 6, 8, "minecraft:air")
                .walls(0, 1, 0, 8, 1, 7, "minecraft:stone_bricks")
                .walls(0, 2, 0, 8, 3, 7, "minecraft:oak_planks")
                .fill(0, 4, 0, 8, 4, 7, "minecraft:oak_planks")
                .fill(1, 5, 0, 7, 5, 7, "minecraft:dark_oak_planks")
                .fill(3, 6, 0, 5, 6, 7, "minecraft:dark_oak_slab[type=bottom]")
                .set(2, 2, 7, "minecraft:glass_pane")
                .set(6, 2, 7, "minecraft:glass_pane")
                .set(0, 2, 3, "minecraft:glass_pane")
                .set(0, 2, 5, "minecraft:glass_pane")
                .set(8, 2, 3, "minecraft:glass_pane")
                .set(8, 2, 5, "minecraft:glass_pane")
                .fill(5, 1, 1, 6, 2, 1, "minecraft:bookshelf")
                .set(7, 1, 1, "minecraft:lectern[facing=south]")
                .set(4, 1, 3, "minecraft:oak_fence")
                .set(4, 2, 3, "minecraft:oak_pressure_plate")
                .set(3, 1, 3, "minecraft:oak_stairs[facing=east]")
                .set(5, 1, 3, "minecraft:oak_stairs[facing=west]")
                .set(4, 3, 4, "minecraft:lantern[hanging=true]");
        bed(piece, 1, 1, "minecraft:red_bed");
        pillars(piece, 8, 7, "minecraft:oak_log");
        door(piece, 4, 7, "minecraft:oak_door");
        piece.entity(4, 1, 5, Overgearedmod.MODID + ":village_chief", new CompoundTag());
        return front(piece, 4, 8);
    }

    /** A villager's house: bed, crafting table, barrel. */
    static TemplateBuilder house() {
        TemplateBuilder piece = new TemplateBuilder(7, 6, 8)
                .fill(0, 0, 0, 6, 0, 6, "minecraft:oak_planks")
                .fill(0, 0, 7, 6, 0, 7, PATH)
                .fill(0, 1, 0, 6, 5, 7, "minecraft:air")
                .walls(0, 1, 0, 6, 1, 6, "minecraft:cobblestone")
                .walls(0, 2, 0, 6, 3, 6, "minecraft:oak_planks")
                .fill(0, 4, 0, 6, 4, 6, "minecraft:spruce_planks")
                .fill(1, 5, 0, 5, 5, 6, "minecraft:spruce_slab[type=bottom]")
                .set(1, 2, 6, "minecraft:glass_pane")
                .set(5, 2, 6, "minecraft:glass_pane")
                .set(0, 2, 3, "minecraft:glass_pane")
                .set(6, 2, 3, "minecraft:glass_pane")
                .set(5, 1, 1, "minecraft:crafting_table")
                .set(5, 1, 2, "minecraft:barrel[facing=up]")
                .set(4, 1, 1, "minecraft:potted_poppy")
                .set(3, 3, 3, "minecraft:lantern[hanging=true]");
        bed(piece, 1, 1, "minecraft:white_bed");
        pillars(piece, 6, 6, "minecraft:oak_log");
        door(piece, 3, 6, "minecraft:oak_door");
        return front(piece, 3, 7);
    }

    /** A smaller spruce house with a fenced flower garden in front. */
    static TemplateBuilder gardenHouse() {
        TemplateBuilder piece = new TemplateBuilder(7, 6, 10)
                .fill(0, 0, 0, 6, 0, 5, "minecraft:spruce_planks")
                .fill(0, 0, 6, 6, 0, 9, "minecraft:grass_block")
                .fill(3, 0, 6, 3, 0, 9, PATH)
                .fill(0, 1, 0, 6, 5, 9, "minecraft:air")
                .walls(0, 1, 0, 6, 1, 5, "minecraft:cobblestone")
                .walls(0, 2, 0, 6, 3, 5, "minecraft:spruce_planks")
                .fill(0, 4, 0, 6, 4, 5, "minecraft:dark_oak_planks")
                .fill(1, 5, 0, 5, 5, 5, "minecraft:dark_oak_slab[type=bottom]")
                .set(1, 2, 5, "minecraft:glass_pane")
                .set(5, 2, 5, "minecraft:glass_pane")
                .set(0, 2, 2, "minecraft:glass_pane")
                .set(6, 2, 2, "minecraft:glass_pane")
                .set(1, 1, 1, "minecraft:cauldron")
                .set(3, 3, 2, "minecraft:lantern[hanging=true]")
                // The garden.
                .fill(0, 1, 6, 0, 1, 8, "minecraft:oak_fence")
                .fill(6, 1, 6, 6, 1, 8, "minecraft:oak_fence")
                .fill(1, 1, 8, 2, 1, 8, "minecraft:oak_fence")
                .fill(4, 1, 8, 5, 1, 8, "minecraft:oak_fence")
                .set(1, 1, 6, "minecraft:poppy")
                .set(2, 1, 7, "minecraft:dandelion")
                .set(4, 1, 7, "minecraft:cornflower")
                .set(5, 1, 6, "minecraft:composter");
        bed(piece, 5, 1, "minecraft:light_blue_bed");
        pillars(piece, 6, 5, "minecraft:spruce_log");
        door(piece, 3, 5, "minecraft:spruce_door");
        return front(piece, 3, 9);
    }

    /**
     * The mountain mine (canon: where Grid digs Smith's iron). A rocky mound with a propped tunnel, rails and a minecart,
     * and iron and coal in the walls of the room at the back. It stays above ground: villages adapt the terrain around
     * their pieces (beard_thin), which would dig a crater over a piece that went underground.
     */
    static TemplateBuilder mine() {
        TemplateBuilder piece = new TemplateBuilder(11, 8, 12);
        rock(piece, 0, 0, 0, 10, 3, 9);
        rock(piece, 1, 4, 0, 9, 4, 8);
        rock(piece, 2, 5, 0, 8, 5, 7);
        rock(piece, 3, 6, 1, 7, 6, 6);
        rock(piece, 4, 7, 2, 6, 7, 5);
        piece.fill(0, 0, 10, 10, 0, 11, "minecraft:coarse_dirt")
                .fill(4, 0, 10, 6, 0, 11, PATH)
                .fill(0, 1, 10, 10, 7, 11, "minecraft:air")
                // Tunnel and the room at the back.
                .fill(4, 1, 2, 6, 3, 9, "minecraft:air")
                .fill(2, 1, 1, 8, 3, 4, "minecraft:air")
                .fill(5, 1, 2, 5, 1, 9, "minecraft:rail[shape=north_south]")
                // Props: at the mouth and half way in.
                .fill(3, 1, 9, 3, 3, 9, "minecraft:oak_log")
                .fill(7, 1, 9, 7, 3, 9, "minecraft:oak_log")
                .fill(3, 4, 9, 7, 4, 9, "minecraft:oak_planks")
                .fill(3, 1, 6, 3, 3, 6, "minecraft:oak_log")
                .fill(7, 1, 6, 7, 3, 6, "minecraft:oak_log")
                .fill(3, 4, 6, 7, 4, 6, "minecraft:oak_planks")
                .set(5, 3, 7, "minecraft:lantern[hanging=true]")
                .set(5, 3, 3, "minecraft:lantern[hanging=true]");
        int[][] iron = {{1, 2, 2}, {1, 1, 3}, {9, 1, 2}, {9, 2, 3}, {3, 2, 0}, {5, 1, 0}, {7, 2, 0}, {4, 4, 2}, {6, 4, 3},
                {3, 2, 7}, {7, 1, 8}};
        for (int[] ore : iron) {
            piece.set(ore[0], ore[1], ore[2], "minecraft:iron_ore");
        }
        int[][] coal = {{1, 3, 1}, {9, 3, 4}, {2, 4, 3}, {7, 4, 1}};
        for (int[] ore : coal) {
            piece.set(ore[0], ore[1], ore[2], "minecraft:coal_ore");
        }
        piece.entity(5, 1, 2, "minecraft:minecart", new CompoundTag());
        return front(piece, 5, 11);
    }

    private static String pool(String name) {
        return Overgearedmod.MODID + ":bairan/" + name;
    }

    private static void lampPost(TemplateBuilder piece, int x, int z) {
        piece.fill(x, 1, z, x, 2, z, "minecraft:oak_fence").set(x, 3, z, "minecraft:lantern");
    }

    /** Log pillars on the four corners of a building whose walls run from (0, 0) to (maxX, maxZ), floors 1 to 3. */
    private static void pillars(TemplateBuilder piece, int maxX, int maxZ, String log) {
        for (int[] corner : new int[][]{{0, 0}, {maxX, 0}, {0, maxZ}, {maxX, maxZ}}) {
            piece.fill(corner[0], 1, corner[1], corner[0], 3, corner[1], log);
        }
    }

    /** A two-block door in the south wall, opening outwards to the front. */
    private static void door(TemplateBuilder piece, int x, int z, String door) {
        piece.set(x, 1, z, door + "[facing=south,half=lower,hinge=left,open=false]")
                .set(x, 2, z, door + "[facing=south,half=upper,hinge=left,open=false]");
    }

    /** A bed with its head at (x, 1, z) and its foot one block south. */
    private static void bed(TemplateBuilder piece, int x, int z, String bed) {
        piece.set(x, 1, z, bed + "[facing=north,part=head]").set(x, 1, z + 1, bed + "[facing=north,part=foot]");
    }

    /** Mixed stone, andesite and cobblestone, always the same for the same position. */
    private static void rock(TemplateBuilder piece, int x1, int y1, int z1, int x2, int y2, int z2) {
        for (int y = y1; y <= y2; y++) {
            for (int z = z1; z <= z2; z++) {
                for (int x = x1; x <= x2; x++) {
                    int mix = Math.floorMod(x * 31 + y * 17 + z * 7, 5);
                    piece.set(x, y, z, mix == 3 ? "minecraft:andesite" : mix == 4 ? "minecraft:cobblestone" : "minecraft:stone");
                }
            }
        }
    }

    /** The jigsaw that joins a building to the square: in the middle of its front row, facing out. */
    private static TemplateBuilder front(TemplateBuilder piece, int x, int z) {
        return piece.jigsaw(x, 0, z, "south_up", ENTRY, PLAZA_JOINT, NO_POOL, PATH);
    }
}
