package com.studio.planeshift.common.course;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Star coins have to gate something, and the gate has to be openable.
 *
 * <p>Coins were tracked, counted and displayed for the whole life of the codebase while gating
 * exactly nothing, which is the same shape as every other bug this project keeps finding: work
 * that is finished, correct and unreachable. The risk in fixing it is the opposite failure --
 * setting the requirement somewhere a player cannot actually get to, turning a goal into a wall.
 * So this pins both ends: the gate is real, and there are enough coins in the game to open it with
 * room to spare.
 */
class StarCoinGateTest {

    private static WorldDefinition finalWorld() {
        List<WorldDefinition> worlds = WorldRegistry.allWorlds();
        return worlds.get(worlds.size() - 1);
    }

    @Test
    @DisplayName("the last world is the gated one and no other world is")
    void onlyTheLastWorldIsGated() {
        List<WorldDefinition> worlds = WorldRegistry.allWorlds();
        for (int i = 0; i < worlds.size() - 1; i++) {
            assertFalse(WorldRegistry.isFinalWorld(worlds.get(i)),
                    worlds.get(i).worldId() + " should not be gated");
        }
        assertTrue(WorldRegistry.isFinalWorld(finalWorld()));
    }

    @Test
    @DisplayName("a player with no coins is told exactly how many are missing")
    void reportsTheShortfall() {
        CourseProgress empty = CourseProgress.DEFAULT;
        assertEquals(WorldRegistry.FINAL_WORLD_STAR_COINS,
                WorldRegistry.starCoinsStillNeeded(empty, finalWorld()),
                "a player with nothing should be short the whole requirement");
    }

    @Test
    @DisplayName("the requirement is reachable well before the final world")
    void theGateCanBeOpened() {
        // Coins available in every world before the last one. If the requirement ever exceeds
        // this the game is unfinishable, and it would be unfinishable silently -- the player would
        // simply run out of courses with the gate still shut.
        List<WorldDefinition> worlds = WorldRegistry.allWorlds();
        int availableBefore = 0;
        for (int i = 0; i < worlds.size() - 1; i++) {
            availableBefore += worlds.get(i).courseCount() * CourseProgress.STAR_COINS_PER_COURSE;
        }
        assertTrue(WorldRegistry.FINAL_WORLD_STAR_COINS < availableBefore,
                "the gate needs " + WorldRegistry.FINAL_WORLD_STAR_COINS + " coins but only "
                        + availableBefore + " exist before the final world");
        // And with margin, so it does not demand a perfect run of everything preceding it.
        assertTrue(WorldRegistry.FINAL_WORLD_STAR_COINS <= availableBefore * 2 / 3,
                "the gate should not require most of every coin in the game");
    }
}
