package com.studio.planeshift.common.course;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class CourseProgressTest {

    private static final String COURSE = "w1_grassland_1";

    @Test
    @DisplayName("an untouched course reads as unplayed rather than throwing")
    void unknownCourseIsEmpty() {
        assertFalse(CourseProgress.DEFAULT.cleared(COURSE));
        assertEquals(0, CourseProgress.DEFAULT.starCoins(COURSE));
        assertEquals(0, CourseProgress.DEFAULT.clearedCount());
        assertEquals(0, CourseProgress.DEFAULT.totalStarCoins());
    }

    @Test
    @DisplayName("a clear records the score and the remaining clock")
    void clearRecordsScoreAndTime() {
        CourseProgress progress = CourseProgress.DEFAULT.withClear(COURSE, 12_000, 3_400);

        assertTrue(progress.cleared(COURSE));
        assertEquals(12_000, progress.record(COURSE).bestScore());
        assertEquals(3_400, progress.record(COURSE).bestTimeLeft());
        assertEquals(1, progress.clearedCount());
    }

    /**
     * The point of "best": a player who beats a course badly after beating it well has not lost
     * their record. Storing the latest run instead would punish replaying for star coins.
     */
    @Test
    @DisplayName("a worse repeat run does not overwrite the record")
    void worseRunKeepsTheBest() {
        CourseProgress progress = CourseProgress.DEFAULT
                .withClear(COURSE, 12_000, 3_400)
                .withClear(COURSE, 500, 100);

        assertEquals(12_000, progress.record(COURSE).bestScore());
        assertEquals(3_400, progress.record(COURSE).bestTimeLeft());
    }

    @Test
    @DisplayName("star coins cap per course, so replaying cannot inflate the collection")
    void starCoinsCapPerCourse() {
        CourseProgress progress = CourseProgress.DEFAULT;
        for (int i = 0; i < 10; i++) {
            progress = progress.withStarCoin(COURSE);
        }

        assertEquals(CourseProgress.STAR_COINS_PER_COURSE, progress.starCoins(COURSE));
        assertEquals(CourseProgress.STAR_COINS_PER_COURSE, progress.totalStarCoins());
        assertTrue(progress.record(COURSE).allStarCoins());
    }

    @Test
    @DisplayName("star coins are tracked per course, not as one running total")
    void starCoinsAreScopedToTheirCourse() {
        CourseProgress progress = CourseProgress.DEFAULT
                .withStarCoin("w1_grassland_1")
                .withStarCoin("w1_grassland_1")
                .withStarCoin("w1_grassland_2");

        assertEquals(2, progress.starCoins("w1_grassland_1"));
        assertEquals(1, progress.starCoins("w1_grassland_2"));
        assertEquals(3, progress.totalStarCoins());
    }

    @Test
    @DisplayName("collecting a star coin does not mark a course cleared, and clearing keeps coins")
    void clearAndStarCoinsAreIndependent() {
        CourseProgress coinsOnly = CourseProgress.DEFAULT.withStarCoin(COURSE);
        assertFalse(coinsOnly.cleared(COURSE), "finding a coin is not finishing the course");

        CourseProgress both = coinsOnly.withClear(COURSE, 100, 0);
        assertTrue(both.cleared(COURSE));
        assertEquals(1, both.starCoins(COURSE), "the clear must not wipe the coins already found");
    }

    @Test
    @DisplayName("a change that changes nothing returns the same record")
    void noOpChangeIsIdentity() {
        CourseProgress progress = CourseProgress.DEFAULT.withClear(COURSE, 100, 0);
        assertSame(progress, progress.withRecord(COURSE, r -> r));
    }

    @Test
    @DisplayName("the first course of the first world is open to a brand new save")
    void firstCourseIsAlwaysOpen() {
        WorldDefinition first = WorldRegistry.allWorlds().get(0);
        assertTrue(WorldRegistry.isUnlocked(CourseProgress.DEFAULT, first.courseIds().get(0)));
    }

    @Test
    @DisplayName("a course opens only once the one before it is cleared")
    void coursesUnlockInSequence() {
        WorldDefinition world = WorldRegistry.allWorlds().get(0);
        List<String> courses = world.courseIds();

        assertFalse(WorldRegistry.isUnlocked(CourseProgress.DEFAULT, courses.get(1)));

        CourseProgress afterFirst = CourseProgress.DEFAULT.withClear(courses.get(0), 100, 0);
        assertTrue(WorldRegistry.isUnlocked(afterFirst, courses.get(1)));
        assertFalse(WorldRegistry.isUnlocked(afterFirst, courses.get(2)),
                "clearing one course must not open the whole world");
    }

    @Test
    @DisplayName("a world opens only once the previous world's boss is cleared")
    void worldsUnlockOnBossClear() {
        WorldDefinition first = WorldRegistry.allWorlds().get(0);
        WorldDefinition second = WorldRegistry.allWorlds().get(1);

        assertFalse(WorldRegistry.isWorldUnlocked(CourseProgress.DEFAULT, second));

        // Clearing everything except the boss must not be enough.
        CourseProgress almost = CourseProgress.DEFAULT;
        for (String courseId : first.courseIds()) {
            if (!courseId.equals(first.bossCourseId())) {
                almost = almost.withClear(courseId, 100, 0);
            }
        }
        assertFalse(WorldRegistry.isWorldUnlocked(almost, second),
                "the boss course is the gate; clearing around it must not open the next world");

        CourseProgress beaten = almost.withClear(first.bossCourseId(), 100, 0);
        assertTrue(WorldRegistry.isWorldUnlocked(beaten, second));
    }

    @Test
    @DisplayName("a cleared course stays open so it can be replayed for star coins")
    void clearedCoursesStayOpen() {
        WorldDefinition world = WorldRegistry.allWorlds().get(2);
        String late = world.courseIds().get(5);

        CourseProgress progress = CourseProgress.DEFAULT.withClear(late, 100, 0);
        assertTrue(WorldRegistry.isUnlocked(progress, late),
                "a course already beaten must not lock itself behind its own prerequisites");
    }

    /**
     * The five vertical-slice courses are not in the world table. Gating something the
     * progression system has never heard of would lock it out with no way in.
     */
    @Test
    @DisplayName("a course outside the world list is never gated")
    void unknownCoursesAreNotGated() {
        assertTrue(WorldRegistry.isUnlocked(CourseProgress.DEFAULT, "course_1"));
        assertTrue(WorldRegistry.isUnlocked(CourseProgress.DEFAULT, "course_5"));
        assertTrue(WorldRegistry.isUnlocked(CourseProgress.DEFAULT, "something_a_datapack_added"));
    }

    @Test
    @DisplayName("every registered course id resolves back to exactly one world")
    void worldLookupCoversEveryCourse() {
        for (WorldDefinition world : WorldRegistry.allWorlds()) {
            for (String courseId : world.courseIds()) {
                assertSame(world, WorldRegistry.worldForCourse(courseId), courseId);
            }
        }
        assertEquals(WorldRegistry.worldCount() * WorldDefinition.COURSES_PER_WORLD,
                WorldRegistry.totalCourses());
    }
}
