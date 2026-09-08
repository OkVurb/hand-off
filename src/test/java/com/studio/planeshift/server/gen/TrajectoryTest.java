package com.studio.planeshift.server.gen;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.studio.planeshift.common.course.CourseTheme;
import com.studio.planeshift.common.registry.ModBlocks;
import com.studio.planeshift.common.registry.ModEntities;
import net.minecraft.world.entity.EntityType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Every moving thing shows its own path, and shows it with real geometry.
 *
 * <p>Section 1 of the plan, and the part of it that was got wrong once already: the first
 * implementation drew trajectories as particle rings and glowing lines, which said the right thing
 * in a visual language nothing else in the game uses. The rule is not "the path is visible", it is
 * "the path is really there".
 *
 * <p>So this test asks the only question that distinguishes the two — is there a block. A drawn
 * telegraph would pass any test written about what the player can see; only geometry passes this.
 */
class TrajectoryTest {

    private static final int LENGTH = 720;

    /** Whether some course of this theme puts the given block within a block of the given entity. */
    private static boolean pathIsBuilt(CourseTheme theme, EntityType<?> mover,
                                       net.minecraft.world.level.block.Block track) {
        return pathIsBuilt(theme, mover, track, -2, 8);
    }

    /**
     * As above, restricted to a band of heights relative to the mover.
     *
     * <p>The band matters more than it looks. A first version of the platform test searched -2 to
     * +8 and passed with the horizontal track removed entirely -- it was finding the *lift's*
     * hanging cable, which is a different telegraph on a different platform. The test was true and
     * meaningless. Restricting it to the block directly underneath is what makes it a question
     * about the thing it names.
     */
    private static boolean pathIsBuilt(CourseTheme theme, EntityType<?> mover,
                                       net.minecraft.world.level.block.Block track,
                                       int fromDy, int toDy) {
        for (long seed = 0; seed < 30; seed++) {
            CourseCanvas canvas = CourseComposer.compose(theme, LENGTH, 3, seed).canvas();
            for (CourseCanvas.EntitySpawn spawn : canvas.entities()) {
                if (spawn.type() != mover) {
                    continue;
                }
                int x = (int) Math.floor(spawn.x());
                for (int dy = fromDy; dy <= toDy; dy++) {
                    var state = canvas.get(x, (int) Math.floor(spawn.y()) + dy, 0);
                    if (state != null && state.getBlock() == track) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    @Test
    @DisplayName("a travelling saw has a rail under it")
    void sawsRunOnTrack() {
        assertTrue(pathIsBuilt(CourseTheme.LAVA, ModEntities.SAW.get(),
                        ModBlocks.COURSE_RAIL.get()),
                "no saw in thirty volcano courses had a rail near it");
    }

    @Test
    @DisplayName("a moving platform has one too")
    void platformsRunOnTrack() {
        assertTrue(pathIsBuilt(CourseTheme.GRASS, ModEntities.MOVING_PLATFORM.get(),
                        ModBlocks.COURSE_RAIL.get(), -1, -1),
                "moving platforms travel with nothing to show where they go; section 1 says a "
                        + "path is shown by being really there");
    }

    /**
     * The rail must not read as a wall.
     *
     * <p>It is declared {@code noCollision}, so the world lets the player through it. The
     * reachability proof kept its own list and the rail was not on it, which made the one block
     * the mod draws paths with expensive to put anywhere the player also stands -- the exact
     * opposite of what section 1 needs.
     */
    @Test
    @DisplayName("and the rail is not a wall")
    void railIsPassable() {
        CourseCanvas canvas = new CourseCanvas();
        canvas.set(0, 0, 0, ModBlocks.COURSE_CASTLE_BLOCK.get().defaultBlockState());
        canvas.set(0, 1, 0, ModBlocks.COURSE_RAIL.get().defaultBlockState());
        canvas.set(0, 2, 0, ModBlocks.COURSE_RAIL.get().defaultBlockState());
        CourseReachability reach = new CourseReachability(canvas, 0);
        assertTrue(reach.isStand(0, 1),
                "a player cannot stand on a floor with a rail drawn through the air above it");
    }
}
