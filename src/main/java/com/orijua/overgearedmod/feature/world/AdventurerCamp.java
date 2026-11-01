package com.orijua.overgearedmod.feature.world;

import com.orijua.overgearedmod.Overgearedmod;
import net.minecraft.resources.ResourceLocation;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * A small adventurers' camp, the first structure on the pipeline: a campfire clearing whose four sides each take a tent,
 * a forge corner or nothing (template pool adventurer_camp/sides). Provisional, made from code until the village and the
 * dungeon reuse the pipeline. The rest is plain data in src/main/resources: worldgen/structure, structure_set,
 * template_pool and the biome tag.
 */
public final class AdventurerCamp {
    public static final ResourceLocation STRUCTURE = Overgearedmod.id("adventurer_camp");
    public static final String CENTER = "adventurer_camp/center";
    public static final String TENT = "adventurer_camp/tent";
    public static final String FORGE = "adventurer_camp/forge";

    /** The center's side jigsaws look for this name in the sides pool. */
    private static final String SIDE = Overgearedmod.MODID + ":camp_side";
    private static final String CENTER_JOINT = Overgearedmod.MODID + ":camp_center";
    private static final String SIDES_POOL = Overgearedmod.MODID + ":adventurer_camp/sides";
    private static final String NO_POOL = "minecraft:empty";
    /** Weapons and armor, so the chest also shows rated loot (the chest loot modifier rates them). */
    private static final String TENT_LOOT = "minecraft:chests/village/village_weaponsmith";

    private AdventurerCamp() {
    }

    /** Every piece by its template path (data/overgearedmod/structure/&lt;path&gt;.nbt). */
    public static Map<String, TemplateBuilder> pieces() {
        Map<String, TemplateBuilder> pieces = new LinkedHashMap<>();
        pieces.put(CENTER, center());
        pieces.put(TENT, tent());
        pieces.put(FORGE, forge());
        return pieces;
    }

    /** 7x7 clearing with a campfire and four log seats; a jigsaw in the middle of each edge. */
    static TemplateBuilder center() {
        TemplateBuilder piece = new TemplateBuilder(7, 4, 7)
                .fill(0, 0, 0, 6, 0, 6, "minecraft:dirt_path")
                .fill(0, 1, 0, 6, 3, 6, "minecraft:air")
                .set(3, 1, 3, "minecraft:campfire[facing=north,lit=true]")
                .set(3, 1, 1, "minecraft:spruce_log[axis=x]")
                .set(3, 1, 5, "minecraft:spruce_log[axis=x]")
                .set(1, 1, 3, "minecraft:spruce_log[axis=z]")
                .set(5, 1, 3, "minecraft:spruce_log[axis=z]");
        sideJigsaw(piece, 3, 0, "north_up");
        sideJigsaw(piece, 3, 6, "south_up");
        sideJigsaw(piece, 0, 3, "west_up");
        sideJigsaw(piece, 6, 3, "east_up");
        return piece;
    }

    /** 5x5 A-frame tent open to the south, with a supply chest at the back. */
    static TemplateBuilder tent() {
        TemplateBuilder piece = new TemplateBuilder(5, 4, 5)
                .fill(0, 0, 0, 4, 0, 4, "minecraft:spruce_planks")
                .fill(0, 1, 0, 4, 3, 4, "minecraft:air")
                .fill(0, 1, 0, 0, 1, 3, "minecraft:white_wool")
                .fill(4, 1, 0, 4, 1, 3, "minecraft:white_wool")
                .fill(1, 2, 0, 1, 2, 3, "minecraft:white_wool")
                .fill(3, 2, 0, 3, 2, 3, "minecraft:white_wool")
                .fill(2, 3, 0, 2, 3, 3, "minecraft:white_wool")
                .fill(1, 1, 0, 3, 1, 0, "minecraft:white_wool")
                .set(2, 2, 0, "minecraft:white_wool")
                .chest(2, 1, 1, "south", TENT_LOOT)
                .set(1, 1, 2, "minecraft:white_carpet")
                .set(3, 1, 2, "minecraft:white_carpet");
        return backJigsaw(piece, "minecraft:spruce_planks");
    }

    /** 5x5 open forge corner under a slab roof: the mod's smithing anvil, a furnace and a barrel with a lantern. */
    static TemplateBuilder forge() {
        TemplateBuilder piece = new TemplateBuilder(5, 4, 5)
                .fill(0, 0, 0, 4, 0, 4, "minecraft:cobblestone")
                .fill(0, 1, 0, 4, 3, 4, "minecraft:air")
                .fill(0, 1, 0, 0, 2, 0, "minecraft:spruce_fence")
                .fill(4, 1, 0, 4, 2, 0, "minecraft:spruce_fence")
                .fill(0, 1, 3, 0, 2, 3, "minecraft:spruce_fence")
                .fill(4, 1, 3, 4, 2, 3, "minecraft:spruce_fence")
                .fill(0, 3, 0, 4, 3, 3, "minecraft:spruce_slab[type=bottom]")
                .set(2, 1, 1, Overgearedmod.MODID + ":smithing_anvil")
                .set(1, 1, 0, "minecraft:furnace[facing=south]")
                .set(3, 1, 0, "minecraft:barrel[facing=up]")
                .set(3, 2, 0, "minecraft:lantern");
        return backJigsaw(piece, "minecraft:cobblestone");
    }

    private static void sideJigsaw(TemplateBuilder piece, int x, int z, String orientation) {
        piece.jigsaw(x, 0, z, orientation, CENTER_JOINT, SIDE, SIDES_POOL, "minecraft:dirt_path");
    }

    /** Side pieces join the center through the middle of their south edge, facing it. */
    private static TemplateBuilder backJigsaw(TemplateBuilder piece, String floor) {
        return piece.jigsaw(2, 0, 4, "south_up", SIDE, CENTER_JOINT, NO_POOL, floor);
    }
}
