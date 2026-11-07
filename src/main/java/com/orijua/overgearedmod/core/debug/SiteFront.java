package com.orijua.overgearedmod.core.debug;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

/**
 * The front of a structure's first piece: its templates open to the south (the cave mouth, Winston's square, the manor),
 * so a piece placed with a {@link Rotation} opens to that rotation of south. Pure geometry for {@link SiteLanding}.
 */
final class SiteFront {
    private SiteFront() {
    }

    /** The side of the piece's box its template's south side ends up on: south, west, north or east. */
    static Direction front(Rotation rotation) {
        return rotation.rotate(Direction.SOUTH);
    }

    /** The block (x, z) {@code gap} blocks outside the front face of {@code box}, in the middle of that face. */
    static BlockPos spot(BoundingBox box, Rotation rotation, int gap) {
        BlockPos middle = box.getCenter();
        return switch (front(rotation)) {
            case NORTH -> new BlockPos(middle.getX(), 0, box.minZ() - gap);
            case EAST -> new BlockPos(box.maxX() + gap, 0, middle.getZ());
            case WEST -> new BlockPos(box.minX() - gap, 0, middle.getZ());
            default -> new BlockPos(middle.getX(), 0, box.maxZ() + gap);
        };
    }

    /** The yaw that looks from the landing back at the piece. */
    static float yawToPiece(Rotation rotation) {
        return front(rotation).getOpposite().toYRot();
    }
}
