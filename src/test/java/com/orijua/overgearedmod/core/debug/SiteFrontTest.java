package com.orijua.overgearedmod.core.debug;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SiteFrontTest {
    private static final int GAP = 3;

    @Test
    void theFrontFollowsTheRotation() {
        assertEquals(Direction.SOUTH, SiteFront.front(Rotation.NONE));
        assertEquals(Direction.WEST, SiteFront.front(Rotation.CLOCKWISE_90));
        assertEquals(Direction.NORTH, SiteFront.front(Rotation.CLOCKWISE_180));
        assertEquals(Direction.EAST, SiteFront.front(Rotation.COUNTERCLOCKWISE_90));
    }

    /** The cave mound is 15x12x15 and its mouth is the south end of the passage, at (7, 3, 14): the front is where that ends up. */
    @Test
    void theFrontIsWhereTheTemplateMouthEndsUp() {
        BlockPos mouth = new BlockPos(7, 3, 14);
        for (Rotation rotation : Rotation.values()) {
            StructurePlaceSettings settings = new StructurePlaceSettings().setRotation(rotation).setMirror(Mirror.NONE).setRotationPivot(BlockPos.ZERO);
            BlockPos placedMouth = StructureTemplate.calculateRelativePosition(settings, mouth);
            BlockPos placedCentre = StructureTemplate.calculateRelativePosition(settings, new BlockPos(7, 3, 7));
            BlockPos offset = placedMouth.subtract(placedCentre);
            Direction towards = Direction.getNearest(offset.getX(), 0, offset.getZ());
            assertEquals(SiteFront.front(rotation), towards, "mouth side for " + rotation);
        }
    }

    @Test
    void theLandingIsOutsideTheBoxInFrontOfItAndFacesIt() {
        BoundingBox box = new BoundingBox(100, 60, 200, 114, 71, 214);
        for (Rotation rotation : Rotation.values()) {
            BlockPos spot = SiteFront.spot(box, rotation, GAP);
            assertFalse(box.isInside(spot.getX(), 65, spot.getZ()), "outside the box for " + rotation + ": " + spot);
            Direction front = SiteFront.front(rotation);
            int distance = switch (front) {
                case SOUTH -> spot.getZ() - box.maxZ();
                case NORTH -> box.minZ() - spot.getZ();
                case EAST -> spot.getX() - box.maxX();
                default -> box.minX() - spot.getX();
            };
            assertEquals(GAP, distance, "gap to the face for " + rotation);
            // Facing back at the piece: the opposite of the front.
            float yaw = SiteFront.yawToPiece(rotation);
            assertEquals(front.getOpposite(), Direction.fromYRot(yaw), "looks at the piece for " + rotation);
            assertTrue(spot.getX() >= box.minX() - GAP && spot.getX() <= box.maxX() + GAP);
        }
        assertEquals(new BlockPos(107, 0, 217), SiteFront.spot(box, Rotation.NONE, GAP));
    }
}
