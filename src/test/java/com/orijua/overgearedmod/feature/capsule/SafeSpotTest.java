package com.orijua.overgearedmod.feature.capsule;

import net.minecraft.core.BlockPos;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SafeSpotTest {
    private static final BlockPos ORIGIN = new BlockPos(100, 64, 100);

    /** Solid blocks, lava blocks (not open, not a safe floor) and air everywhere else. */
    private static final class World implements SafeSpot.Probe {
        final Set<BlockPos> solid = new HashSet<>();
        final Set<BlockPos> lava = new HashSet<>();

        World solid(BlockPos... positions) {
            solid.addAll(List.of(positions));
            return this;
        }

        World lava(BlockPos... positions) {
            lava.addAll(List.of(positions));
            return this;
        }

        @Override
        public boolean isOpen(BlockPos pos) {
            return !solid.contains(pos) && !lava.contains(pos);
        }

        @Override
        public boolean isSafeFloor(BlockPos pos) {
            return solid.contains(pos);
        }
    }

    /** Makes the spot at {@code feet} valid: floor below, air at feet and head. */
    private static void withSpot(World world, BlockPos feet) {
        world.solid.add(feet.below());
        world.solid.remove(feet);
        world.solid.remove(feet.above());
    }

    /** A world solid everywhere inside the search volume, so only the spots a test carves are valid. */
    private static World full() {
        World world = new World();
        for (int dx = -SafeSpot.RADIUS - 1; dx <= SafeSpot.RADIUS + 1; dx++) {
            for (int dz = -SafeSpot.RADIUS - 1; dz <= SafeSpot.RADIUS + 1; dz++) {
                for (int dy = -SafeSpot.DOWN - 3; dy <= SafeSpot.UP + 3; dy++) {
                    world.solid.add(ORIGIN.offset(dx, dy, dz));
                }
            }
        }
        return world;
    }

    private static Optional<BlockPos> find(World world) {
        return SafeSpot.find(world, ORIGIN);
    }

    @Test
    void solidGroundUnderTheOriginGivesTheOrigin() {
        World world = new World().solid(ORIGIN.below());
        assertEquals(Optional.of(ORIGIN), find(world));
    }

    @Test
    void allAirHasNoFloorAnywhere() {
        assertEquals(Optional.empty(), find(new World()));
    }

    @Test
    void feetInsideStoneClimbToTheFirstGapWithFloor() {
        World world = new World().solid(ORIGIN.below(), ORIGIN, ORIGIN.above());
        // The pile is origin-1..origin+1: the first valid feet are on top of it.
        assertEquals(Optional.of(ORIGIN.above(2)), find(world));
    }

    @Test
    void aSolidColumnMovesToTheNeighbour() {
        World world = full();
        withSpot(world, ORIGIN.east());
        assertEquals(Optional.of(ORIGIN.east()), find(world));
    }

    @Test
    void anUnsafeFloorIsRejectedAndTheNeighbourWins() {
        // The floor under the origin is "lava" for this probe: not a safe floor, so the origin is out.
        World world = new World().lava(ORIGIN.below()).solid(ORIGIN.east().below());
        assertEquals(Optional.of(ORIGIN.east()), find(world));
    }

    @Test
    void lavaAtTheFeetIsRejected() {
        World world = new World().solid(ORIGIN.below(), ORIGIN.east().below()).lava(ORIGIN);
        assertEquals(Optional.of(ORIGIN.east()), find(world));
    }

    @Test
    void aBlockedHeadIsRejected() {
        // Solid floor, free feet, solid head: the only free feet in the whole volume, and they do not fit a player.
        World world = full();
        world.solid.remove(ORIGIN);
        assertEquals(Optional.empty(), find(world));
    }

    @Test
    void aWorldSolidEverywhereHasNoSpot() {
        assertEquals(Optional.empty(), find(full()));
    }

    @Test
    void theNearestGapInTheColumnWinsAndUpWinsATie() {
        World farUp = full();
        withSpot(farUp, ORIGIN.above(2));
        withSpot(farUp, ORIGIN.below(1));
        assertEquals(Optional.of(ORIGIN.below(1)), find(farUp), "-1 is nearer than +2");

        World tie = full();
        withSpot(tie, ORIGIN.above(2));
        withSpot(tie, ORIGIN.below(2));
        assertEquals(Optional.of(ORIGIN.above(2)), find(tie), "+2 and -2 are as near: up first");

        World farDown = full();
        withSpot(farDown, ORIGIN.above(3));
        withSpot(farDown, ORIGIN.below(2));
        assertEquals(Optional.of(ORIGIN.below(2)), find(farDown), "-2 is nearer than +3");
    }

    @Test
    void theFullColumnComesBeforeTheFirstRing() {
        World world = full();
        withSpot(world, ORIGIN.above(SafeSpot.UP));
        withSpot(world, ORIGIN.east());
        assertEquals(Optional.of(ORIGIN.above(SafeSpot.UP)), find(world));

        World down = full();
        withSpot(down, ORIGIN.below(SafeSpot.DOWN));
        withSpot(down, ORIGIN.east());
        assertEquals(Optional.of(ORIGIN.below(SafeSpot.DOWN)), find(down));
    }

    @Test
    void beyondTheColumnRangeTheRingWins() {
        World world = full();
        withSpot(world, ORIGIN.above(SafeSpot.UP + 1));
        withSpot(world, ORIGIN.east());
        assertEquals(Optional.of(ORIGIN.east()), find(world));
    }

    @Test
    void theLowestSpotIsFoundAndOneBelowIsNot() {
        World low = full();
        withSpot(low, ORIGIN.below(SafeSpot.DOWN));
        assertEquals(Optional.of(ORIGIN.below(SafeSpot.DOWN)), find(low));
        World tooLow = full();
        withSpot(tooLow, ORIGIN.below(SafeSpot.DOWN + 1));
        assertEquals(Optional.empty(), find(tooLow));
    }

    @Test
    void neighboursAreTriedInADeterministicOrder() {
        World world = full();
        withSpot(world, ORIGIN.offset(1, 0, 0));
        withSpot(world, ORIGIN.offset(1, 0, 1));
        assertEquals(Optional.of(ORIGIN.offset(1, 0, 0)), find(world));
    }

    @Test
    void theRingTieBreakIsDistanceThenDxThenDz() {
        // Same distance (dx²+dz² = 1): the smaller dx wins, then the smaller dz.
        World dx = full();
        withSpot(dx, ORIGIN.offset(1, 0, 0));
        withSpot(dx, ORIGIN.offset(-1, 0, 0));
        assertEquals(Optional.of(ORIGIN.offset(-1, 0, 0)), find(dx), "dx -1 before dx +1");
        World dz = full();
        withSpot(dz, ORIGIN.offset(0, 0, 1));
        withSpot(dz, ORIGIN.offset(0, 0, -1));
        assertEquals(Optional.of(ORIGIN.offset(0, 0, -1)), find(dz), "dz -1 before dz +1");
        World mixed = full();
        withSpot(mixed, ORIGIN.offset(1, 0, 0));
        withSpot(mixed, ORIGIN.offset(0, 0, 1));
        assertEquals(Optional.of(ORIGIN.offset(0, 0, 1)), find(mixed), "dx 0 before dx 1");
        // A nearer diagonal beats a farther straight column in the same ring.
        World ring2 = full();
        withSpot(ring2, ORIGIN.offset(2, 0, 2));
        withSpot(ring2, ORIGIN.offset(2, 0, 0));
        assertEquals(Optional.of(ORIGIN.offset(2, 0, 0)), find(ring2), "(2,0) is nearer than (2,2)");
    }

    @Test
    void ringOneIsOrderedFromTheNearestColumns() {
        List<List<Integer>> order = SafeSpot.ring(1).stream().map(c -> List.of(c[0], c[1])).toList();
        assertEquals(List.of(List.of(-1, 0), List.of(0, -1), List.of(0, 1), List.of(1, 0),
                List.of(-1, -1), List.of(-1, 1), List.of(1, -1), List.of(1, 1)), order);
    }

    @Test
    void ringOneComesBeforeRingTwo() {
        World world = full();
        withSpot(world, ORIGIN.offset(2, 0, 0));
        withSpot(world, ORIGIN.offset(1, 1, 1));
        assertEquals(Optional.of(ORIGIN.offset(1, 1, 1)), find(world));
    }

    @Test
    void theSearchStopsAtTheRadius() {
        World far = full();
        withSpot(far, ORIGIN.offset(SafeSpot.RADIUS + 1, 0, 0));
        assertEquals(Optional.empty(), find(far));
        World near = full();
        withSpot(near, ORIGIN.offset(SafeSpot.RADIUS, 0, 0));
        assertEquals(Optional.of(ORIGIN.offset(SafeSpot.RADIUS, 0, 0)), find(near));
    }

    @Test
    void ringsHaveTheRightColumns() {
        assertEquals(1, SafeSpot.ring(0).size());
        for (int r = 1; r <= SafeSpot.RADIUS; r++) {
            List<int[]> ring = SafeSpot.ring(r);
            assertEquals(8 * r, ring.size(), "ring " + r);
            Set<List<Integer>> distinct = new HashSet<>();
            for (int[] c : ring) {
                assertEquals(r, Math.max(Math.abs(c[0]), Math.abs(c[1])));
                distinct.add(List.of(c[0], c[1]));
            }
            assertEquals(8 * r, distinct.size(), "no repeated column in ring " + r);
        }
    }
}
