package com.studio.planeshift.server.gen;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.studio.planeshift.common.course.CourseTheme;
import com.studio.planeshift.common.registry.ModBlocks;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Scenery is scenery: it fills the world in and never changes the game.
 *
 * <p>The guarantee is structural rather than checked case by case. Decoration writes only at
 * depths {@link CourseDecorator#NEAR_Z} and beyond, the reachability solver reads only the play
 * plane, and the two sets of columns are disjoint — so no amount of scenery can make a course
 * harder. These tests pin that relationship, because it is the entire reason the decorator is
 * allowed to be as free with the canvas as it is.
 */
class CourseDecoratorTest {

    @Test
    void decorationNeverReachesTheLane() {
        // The lane is +/- GenContext.LANE_HALF_WIDTH around z=0, and scenery starts beyond it with
        // a clear column in between: flush against the play plane, scenery reads as level geometry
        // and the player will try to stand on it.
        //
        // Measured as a distance, not as a coordinate. These constants used to be positive and the
        // test compared them directly, which quietly encoded the side as well as the gap -- and the
        // side was wrong. The camera sits on the positive side of the depth axis, so scenery at
        // +2 was between the camera and the course, drawn over the level it was meant to be behind.
        assertTrue(Math.abs(CourseDecorator.NEAR_Z) > GenContext.LANE_HALF_WIDTH,
                "decoration would sit inside the lane");
        assertTrue(Math.abs(CourseDecorator.NEAR_Z) - GenContext.LANE_HALF_WIDTH >= 1,
                "decoration needs a clear column between it and the play plane");
    }

    /**
     * Scenery is behind the player, on the far side from the camera.
     *
     * <p>The bug this was written for: every depth constant here was positive, and
     * {@code CourseService} builds its rail with {@code lookPositive = true}, so positive is where
     * the camera is. Hills, trees, the backdrop wall and half the props were all drawn in front of
     * the course.
     */
    @Test
    void decorationSitsBehindTheCourse() {
        assertTrue(CourseDecorator.NEAR_Z < 0 && CourseDecorator.FAR_Z < 0
                        && CourseDecorator.BACKDROP_Z < 0,
                "scenery is on the camera's side of the lane, so it draws over the level");
    }

    @Test
    void decorationStaysInsideTheClearedVolume() {
        // CourseWriter clears a fixed half-width around the lane. Anything drawn beyond that would
        // be written into terrain that was never cleared, which is how a course ends up with
        // scenery embedded in a hillside.
        assertTrue(Math.abs(CourseDecorator.FAR_Z) <= 3,
                "decoration must stay inside CourseWriter's cleared half-width");
        assertTrue(Math.abs(CourseDecorator.FAR_Z) >= Math.abs(CourseDecorator.NEAR_Z),
                "the far band must be at least as far back as the near one");
    }

    /** Every theme actually decorates: an undecorated theme is the bug this class exists to fix. */
    @Test
    void everyThemePutsSomethingBehindTheLane() {
        for (CourseTheme theme : CourseTheme.values()) {
            Set<Integer> depths = new HashSet<>();
            for (long seed = 0; seed < 12; seed++) {
                CourseComposer.Composition c = CourseComposer.compose(theme, 240, 3, seed);
                c.canvas().blocks().forEach((key, state) -> {
                    int z = CourseCanvas.unpackZ(key);
                    if (Math.abs(z) >= CourseDecorator.NEAR_Z) {
                        depths.add(z);
                    }
                });
            }
            assertTrue(!depths.isEmpty(), theme + " generated no scenery at all");
        }
    }

    /**
     * Detail stands on the playfield, not only behind it.
     *
     * <p>§5.8's complaint was that every prop in this file is placed behind the lane, which makes
     * the walkable surface read as a shelf the level is displayed on rather than as ground. What is
     * checked is the lane column specifically — a test that counted props anywhere would have
     * passed before the change and after it.
     */
    @Test
    @DisplayName("ground cover stands on the lane itself")
    void detailSitsOnThePlayfield() {
        int withCover = 0;
        for (long seed = 0; seed < 12; seed++) {
            CourseCanvas canvas = CourseComposer.compose(
                    CourseTheme.GRASS, 480, 2, seed).canvas();
            for (int x = 0; x < 480; x++) {
                for (int y = -4; y < 16; y++) {
                    var state = canvas.get(x, y, 0);
                    if (state != null && state.is(ModBlocks.COURSE_TUFT.get())) {
                        withCover++;
                        x = 480;
                        break;
                    }
                }
            }
        }
        assertTrue(withCover >= 10,
                "only " + withCover + " of 12 grass courses had anything growing on the floor the "
                        + "player runs along");
    }

    /**
     * The scenery has ground under it.
     *
     * <p>Terrain is three blocks deep — the lane and nothing else — while every prop is placed two
     * or three blocks further back, so bushes, trees, dunes and hills all stood on air. Seen from
     * this camera that reads as furniture hanging in the sky, which is most of what "the background
     * makes no sense" was pointing at.
     *
     * <p>Checked by asking whether the backdrop column repeats the lane's own block at the same
     * height: ground that continues backwards rather than stopping at the edge of the corridor.
     * Counting scenery of any kind would have passed before the fix, since the props were always
     * there — it was the floor beneath them that was missing.
     */
    @Test
    @DisplayName("scenery stands on ground rather than on air")
    void theBackdropHasAFloor() {
        int length = 480;
        for (long seed = 0; seed < 6; seed++) {
            CourseCanvas canvas = CourseComposer.compose(
                    CourseTheme.GRASS, length, 2, seed).canvas();
            int backed = 0;
            int solid = 0;
            for (int x = 0; x < length; x++) {
                boolean laneHasGround = false;
                boolean hasBacking = false;
                for (int y = -6; y < 8; y++) {
                    var lane = canvas.get(x, y, 0);
                    if (lane == null) {
                        continue;
                    }
                    laneHasGround = true;
                    if (lane.equals(canvas.get(x, y, CourseDecorator.NEAR_Z))) {
                        hasBacking = true;
                    }
                }
                if (laneHasGround) {
                    solid++;
                    if (hasBacking) {
                        backed++;
                    }
                }
            }
            assertTrue(backed * 2 >= solid,
                    "seed " + seed + ": only " + backed + " of " + solid
                            + " solid columns had anything behind them to stand on");
        }
    }

    /**
     * A pit is a pit all the way back.
     *
     * <p>The floor above must not be laid across gaps: filling in behind a hole would put a wall
     * across the one thing the level is asking the player to jump over, and the hole would stop
     * reading as a hole. This is the half of the fix that is easy to lose.
     */
    @Test
    @DisplayName("gaps stay open behind the lane too")
    void pitsAreNotFilledInFromBehind() {
        int length = 480;
        int open = 0;
        for (long seed = 0; seed < 6; seed++) {
            CourseCanvas canvas = CourseComposer.compose(
                    CourseTheme.GRASS, length, 2, seed).canvas();
            for (int x = 0; x < length; x++) {
                boolean laneClear = true;
                boolean backClear = true;
                for (int y = -6; y < 4; y++) {
                    if (canvas.get(x, y, 0) != null) {
                        laneClear = false;
                    }
                    if (canvas.get(x, y, CourseDecorator.NEAR_Z) != null) {
                        backClear = false;
                    }
                }
                if (laneClear && backClear) {
                    open++;
                }
            }
        }
        assertTrue(open > 0, "every column had ground behind it, so no gap survived the backdrop");
    }
}
