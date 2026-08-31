package com.studio.planeshift.common.course;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Covers the course clock and score fields on {@link CourseState}.
 *
 * <p>{@code timeLeft} carries a sentinel ({@link CourseState#NO_TIME_LIMIT}) alongside a clamped
 * range, which is exactly the shape that goes wrong quietly: an untimed course must never read
 * as expired, and a clamp that swallows the sentinel would silently start a clock on every
 * course in the game.
 */
class CourseStateTest {

    @Test
    @DisplayName("the default state is untimed and unscored")
    void defaultIsUntimed() {
        CourseState state = CourseState.DEFAULT;

        assertFalse(state.timed(), "the hub must not run a clock");
        assertFalse(state.timeExpired(), "an untimed state must never read as expired");
        assertFalse(state.timeCritical(), "an untimed state must never read as critical");
        assertEquals(0, state.score());
        assertFalse(state.autoScroll());
    }

    @Test
    @DisplayName("an untimed course never expires, however long it runs")
    void untimedNeverExpires() {
        CourseState state = CourseState.DEFAULT.withTimeLeft(CourseState.NO_TIME_LIMIT);

        assertFalse(state.timed());
        assertFalse(state.timeExpired());
    }

    @ParameterizedTest(name = "timeLeft {0} is expired")
    @ValueSource(ints = {0})
    @DisplayName("a timed course at zero has expired")
    void zeroIsExpired(int ticks) {
        CourseState state = CourseState.DEFAULT.withTimeLeft(ticks);

        assertTrue(state.timed(), "zero is still a clock, just a finished one");
        assertTrue(state.timeExpired());
    }

    @Test
    @DisplayName("a running clock is neither expired nor critical until it is low")
    void runningClock() {
        CourseState state = CourseState.DEFAULT.withTimeLeft(400 * 20);

        assertTrue(state.timed());
        assertFalse(state.timeExpired());
        assertFalse(state.timeCritical(), "400 s is well above the warning band");
    }

    @Test
    @DisplayName("the warning band is inclusive at its boundary")
    void criticalBoundary() {
        CourseState atBoundary = CourseState.DEFAULT.withTimeLeft(CourseState.TIME_WARNING_TICKS);
        CourseState justAbove = CourseState.DEFAULT.withTimeLeft(CourseState.TIME_WARNING_TICKS + 1);

        assertTrue(atBoundary.timeCritical(), "exactly at the threshold must already warn");
        assertFalse(justAbove.timeCritical(), "one tick above must not warn yet");
    }

    @ParameterizedTest(name = "withTimeLeft({0}) collapses to NO_TIME_LIMIT")
    @ValueSource(ints = {-1, -2, -50, Integer.MIN_VALUE})
    @DisplayName("any negative clock collapses to the untimed sentinel rather than clamping to zero")
    void negativeCollapsesToSentinel(int ticks) {
        // Clamping these to 0 would make every untimed course instantly "expired" and kill the
        // player on the first tick.
        CourseState state = CourseState.DEFAULT.withTimeLeft(ticks);

        assertEquals(CourseState.NO_TIME_LIMIT, state.timeLeft());
        assertFalse(state.timed());
        assertFalse(state.timeExpired());
    }

    @Test
    @DisplayName("score clamps to the shared value ceiling")
    void scoreClamps() {
        assertEquals(0, CourseState.DEFAULT.withScore(-500).score());
        assertEquals(CourseState.MAX_VALUE,
                CourseState.DEFAULT.withScore(CourseState.MAX_VALUE + 1_000).score());
    }

    @Test
    @DisplayName("course rules set the clock and auto-scroll together")
    void courseRulesApplyTogether() {
        CourseState state = CourseState.DEFAULT.withCourseRules(300 * 20, true);

        assertTrue(state.timed());
        assertEquals(300 * 20, state.timeLeft());
        assertTrue(state.autoScroll());

        CourseState cleared = state.withCourseRules(CourseState.NO_TIME_LIMIT, false);

        assertFalse(cleared.timed());
        assertFalse(cleared.autoScroll());
    }

    @Test
    @DisplayName("changing an unrelated field leaves the clock, score and auto-scroll alone")
    void withersPreserveNewFields() {
        // The withers rebuild the whole record by hand, so a dropped argument is easy to miss.
        CourseState state = CourseState.DEFAULT
                .withCourseRules(120 * 20, true)
                .withScore(4200);

        CourseState afterCoins = state.withCoins(7);

        assertEquals(120 * 20, afterCoins.timeLeft(), "coins must not disturb the clock");
        assertEquals(4200, afterCoins.score(), "coins must not disturb the score");
        assertTrue(afterCoins.autoScroll(), "coins must not disturb the auto-scroll rule");
        assertEquals(7, afterCoins.coins());
    }
}
