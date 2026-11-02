package com.orijua.overgearedmod.feature.world;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.levelgen.structure.PoolElementStructurePiece;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.pools.SinglePoolElement;
import net.minecraft.world.level.levelgen.structure.pools.StructurePoolElement;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Which template a jigsaw piece was built from. {@link SinglePoolElement} keeps its template in a protected field with
 * no getter, so the id is read from its {@code toString()} (it prints as {@code Single[Left[<id>]]}). That parsing lives
 * only here, and a game test pins the format, so a Minecraft update that changes it fails loudly in one place.
 */
public final class PoolPieces {
    private static final Pattern TEMPLATE = Pattern.compile("^\\w+\\[Left\\[([^\\]\\[]+)\\]\\]$");

    private PoolPieces() {
    }

    /** The template id of a piece placed from a single pool element, or empty for any other kind of piece. */
    public static Optional<ResourceLocation> templateOf(StructurePiece piece) {
        return piece instanceof PoolElementStructurePiece pool ? templateOf(pool.getElement()) : Optional.empty();
    }

    /** The template id of a single pool element (also the legacy one), or empty for any other kind of element. */
    public static Optional<ResourceLocation> templateOf(StructurePoolElement element) {
        if (!(element instanceof SinglePoolElement)) {
            return Optional.empty();
        }
        Matcher matcher = TEMPLATE.matcher(element.toString());
        return matcher.matches() ? Optional.ofNullable(ResourceLocation.tryParse(matcher.group(1))) : Optional.empty();
    }
}
