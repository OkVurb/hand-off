package com.studio.planeshift.server.gen;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.studio.planeshift.common.course.CourseTheme;
import com.studio.planeshift.common.registry.ModBlocks;
import org.junit.jupiter.api.Test;

/**
 * The high road exists, and it never becomes the only road.
 *
 * <p>The safety argument is structural: this pass writes nothing but semisolid platforms, above an
 * existing floor. CourseReachability treats a semisolid as floor-from-above and invisible to the
 * body, so an upper deck can add routes and cannot remove or block one. These tests pin the two
 * halves of that — that decks are actually produced, and that they are made of the only block for
 * which the argument holds.
 */
class CourseRoutesTest {

    private static final int SEEDS = 40;

    @Test
    void coursesGrowASecondRouteOftenEnoughToMatter() {
        int withDeck = 0;
        int total = 0;
        for (CourseTheme theme : CourseTheme.values()) {
            for (long seed = 0; seed < SEEDS; seed++) {
                total++;
                CourseComposer.Composition c = CourseComposer.compose(theme, 360, 3, seed);
                boolean deck = c.canvas().blocks().values().stream()
                        .anyMatch(state -> state.is(ModBlocks.SEMISOLID_PLATFORM.get()));
                if (deck) {
                    withDeck++;
                }
            }
        }
        // Not every course: a level where every stretch has an attic stops having an attic and
        // just has two floors. But a feature that fires on a handful of seeds is not a feature.
        assertTrue(withDeck * 4 >= total,
                "only " + withDeck + " of " + total + " courses grew an upper route");
    }

    @Test
    void theUpperRouteIsBuiltFromTheOneBlockThatCannotBlockTheLowerOne() {
        // If this ever starts using ordinary blocks, the high road becomes a ceiling over the low
        // road and the whole point inverts: adding an optional route would make the safe one worse.
        assertTrue(CourseReachability.oneWay().contains(ModBlocks.SEMISOLID_PLATFORM.get()),
                "the deck block must be one-way, or an upper route is a ceiling");
    }
}
