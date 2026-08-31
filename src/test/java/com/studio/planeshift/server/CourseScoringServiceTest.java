package com.studio.planeshift.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Covers the stomp combo ladder in {@link CourseScoringService}.
 *
 * <p>The ladder is the part with real behaviour to get wrong: the values, the point at which a
 * chain stops paying points and starts paying lives, and the bounds around that switch. The
 * surrounding bookkeeping needs a live player and is exercised in game rather than here.
 */
class CourseScoringServiceTest {

    @ParameterizedTest(name = "chain depth {0} awards {1}")
    @CsvSource({
            "0, 100",
            "1, 200",
            "2, 400",
            "3, 800",
            "4, 1000",
            "5, 2000",
            "6, 4000",
            "7, 8000"
    })
    @DisplayName("each rung of the ladder awards its documented value")
    void ladderValues(int depth, int expected) {
        assertEquals(expected, CourseScoringService.stompValue(depth));
    }

    @Test
    @DisplayName("the ladder is strictly increasing, so a longer chain is always worth more")
    void ladderIsStrictlyIncreasing() {
        for (int depth = 1; depth < CourseScoringService.ladderLength(); depth++) {
            int previous = CourseScoringService.stompValue(depth - 1);
            int current = CourseScoringService.stompValue(depth);
            assertTrue(current > previous,
                    "rung " + depth + " (" + current + ") must exceed rung " + (depth - 1)
                            + " (" + previous + ")");
        }
    }

    @Test
    @DisplayName("the last rung still pays points, not a life")
    void lastRungPaysPoints() {
        int last = CourseScoringService.ladderLength() - 1;
        assertEquals(8000, CourseScoringService.stompValue(last),
                "the top of the ladder must be the final scoring rung");
    }

    @ParameterizedTest(name = "depth {0} is past the ladder and awards a life")
    @ValueSource(ints = {8, 9, 12, 100, Integer.MAX_VALUE})
    @DisplayName("past the ladder the chain pays a 1-Up instead of points")
    void beyondLadderAwardsLife(int depth) {
        // The boundary matters: paying points here would let a long chain inflate the score
        // without ever granting the extra life the ladder is meant to end on.
        assertEquals(CourseScoringService.ONE_UP_INSTEAD, CourseScoringService.stompValue(depth));
    }

    @Test
    @DisplayName("a negative depth is a programming error, not a silent zero")
    void negativeDepthRejected() {
        assertThrows(IllegalArgumentException.class, () -> CourseScoringService.stompValue(-1));
    }
}
