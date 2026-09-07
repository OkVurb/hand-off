package com.studio.planeshift.common.course;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * "This clear is what opened the final world" has to be true exactly once.
 *
 * <p>The banner is computed by subtracting the run's own contribution from current progress rather
 * than by remembering the previous state, which is what lets it need nothing synced and nothing
 * persisted. The cost of that trick is that the arithmetic is easy to get subtly wrong in a way
 * that shows up as a banner firing on every subsequent course clear -- a celebration that repeats
 * is worse than none, because it teaches the player to ignore it.
 */
class UnlockMomentTest {

    private static WorldDefinition finalWorld() {
        List<WorldDefinition> worlds = WorldRegistry.allWorlds();
        return worlds.get(worlds.size() - 1);
    }

    private static WorldDefinition penultimate() {
        List<WorldDefinition> worlds = WorldRegistry.allWorlds();
        return worlds.get(worlds.size() - 2);
    }

    /** Progress with the given coin total and optionally the gating boss cleared. */
    private static CourseProgress progress(int totalStarCoins, boolean bossCleared) {
        CourseProgress p = CourseProgress.DEFAULT;
        // Spread the coins across real course ids so totalStarCoins() adds up.
        int remaining = totalStarCoins;
        for (WorldDefinition world : WorldRegistry.allWorlds()) {
            for (String id : world.courseIds()) {
                if (remaining <= 0) {
                    break;
                }
                int here = Math.min(CourseProgress.STAR_COINS_PER_COURSE, remaining);
                for (int i = 0; i < here; i++) {
                    p = p.withStarCoin(id);
                }
                remaining -= here;
            }
        }
        if (bossCleared) {
            p = p.withClear(penultimate().bossCourseId(), 0, 0);
        }
        return p;
    }

    @Test
    @DisplayName("fires on the clear that crosses the coin threshold")
    void firesOnTheCrossing() {
        int gate = WorldRegistry.FINAL_WORLD_STAR_COINS;
        CourseProgress after = progress(gate, true);
        assertTrue(WorldRegistry.justOpenedFinalWorld(after, "some_course", 3),
                "crossing the threshold on this run should announce");
    }

    @Test
    @DisplayName("does not fire again on the next clear")
    void doesNotRepeat() {
        int gate = WorldRegistry.FINAL_WORLD_STAR_COINS;
        // Already well past the gate, and this run contributed a normal amount.
        CourseProgress after = progress(gate + 9, true);
        assertFalse(WorldRegistry.justOpenedFinalWorld(after, "some_course", 3),
                "the world was already open before this run; announcing again trains the player "
                        + "to ignore the banner");
    }

    @Test
    @DisplayName("does not fire while the world is still locked")
    void silentWhenLocked() {
        CourseProgress after = progress(2, true);
        assertFalse(WorldRegistry.justOpenedFinalWorld(after, "some_course", 2));
    }

    @Test
    @DisplayName("fires on the boss clear when the coins were already banked")
    void firesOnTheBossClear() {
        int gate = WorldRegistry.FINAL_WORLD_STAR_COINS;
        CourseProgress after = progress(gate, true);
        assertTrue(
                WorldRegistry.justOpenedFinalWorld(after, penultimate().bossCourseId(), 0),
                "clearing the gating castle with the coins already found should announce");
    }
}
