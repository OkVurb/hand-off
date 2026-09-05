package com.studio.planeshift.server.gen;

import com.studio.planeshift.common.registry.ModBlocks;
import com.studio.planeshift.common.registry.ModEntities;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The Toad House: a room, not a course.
 *
 * <p>The Toad House used to load through the ordinary generator, which meant it was a 64-block
 * grass course with a flagpole on the end -- the shortest level in the game rather than a place.
 * Nothing about it said "you have arrived somewhere", and the flagpole actively said the opposite,
 * because reaching a flagpole is how every other course ends.
 *
 * <p>Hand-built rather than composed, and that is the point of it. The segment library exists to
 * make levels out of parts that can be recombined and proved traversable; a room has one shape, is
 * entered once, and has nothing to prove about reachability beyond the boxes being jumpable. Asking
 * the composer for it would mean teaching it a concept it has no other use for.
 *
 * <p>Open at the camera side and walled everywhere else, which is the 2.5D convention the rest of
 * the mod already follows: {@code CourseDecorator} puts its scenery at positive Z, so positive Z is
 * behind the lane and negative Z is where the player is looking from.
 */
public final class ToadHouseRoom {

    /** The course id this room is built for. */
    public static final String ID = "toad_house";

    /** Distance from the lane centre to the side walls. */
    private static final int HALF = GenContext.LANE_HALF_WIDTH;

    /** Interior span along the lane, from the west wall to the east wall. */
    private static final int FROM = -4;
    private static final int TO = 26;

    /** Interior height. Tall enough that the boxes are overhead rather than at eye level. */
    private static final int CEILING = 8;

    /**
     * How high the boxes hang.
     *
     * <p>The floor fills y=0 so the player stands at y=1; four puts the boxes three clear blocks
     * overhead, which is the ordinary jump-and-headbutt height the rest of the mod uses for
     * question blocks. Any higher and the room would need furniture to climb.
     */
    private static final int BOX_Y = 4;

    /** Where the three boxes stand. Spread so they read as a row of choices, not a cluster. */
    private static final int[] BOX_X = {8, 13, 18};

    private ToadHouseRoom() {
    }

    /** Builds the room into a canvas, ready for {@link CourseWriter}. */
    public static CourseCanvas build() {
        CourseCanvas c = new CourseCanvas();
        BlockState floor = ModBlocks.COURSE_TILE.get().defaultBlockState();
        BlockState wall = ModBlocks.COURSE_WOOD_BLOCK.get().defaultBlockState();
        BlockState trim = ModBlocks.COURSE_HARD_BLOCK.get().defaultBlockState();

        for (int x = FROM; x <= TO; x++) {
            for (int z = -HALF; z <= HALF; z++) {
                c.set(x, 0, z, floor);
                c.set(x, CEILING, z, wall);
            }
            // The back wall. Only the back: the camera looks in from negative Z, so walling that
            // side would hide the room from the only angle it is ever seen from.
            for (int y = 1; y < CEILING; y++) {
                c.set(x, y, HALF + 1, wall);
            }
        }

        // End walls, so the room is closed rather than a corridor that stops.
        for (int y = 1; y < CEILING; y++) {
            for (int z = -HALF; z <= HALF + 1; z++) {
                c.set(FROM, y, z, trim);
                c.set(TO, y, z, trim);
            }
        }

        // Lamps in the ceiling. A sealed room with no light source is a dark room, and the boxes
        // are the one thing in here the player has to be able to see.
        for (int x = FROM + 5; x < TO; x += 6) {
            c.set(x, CEILING - 1, HALF, ModBlocks.COURSE_LAMP.get().defaultBlockState());
        }

        BlockState box = ModBlocks.TOAD_BOX.get().defaultBlockState();
        for (int x : BOX_X) {
            c.set(x, BOX_Y, 0, box);
            // A banner over each one, so the row reads as three of the same thing rather than as
            // three blocks that happen to be in a line.
            c.set(x, BOX_Y + 2, 0, ModBlocks.COURSE_BANNER.get().defaultBlockState());
        }

        // Toad, at the far end, facing back down the room toward the player.
        c.spawn(ModEntities.TOAD.get(), TO - 3 + 0.5D, 1.0D, 0.5D, 90.0F, "toad_house_host");
        c.marker("toad_house", BOX_X[1], BOX_Y, 0);
        return c;
    }
}
