package com.studio.planeshift.server.gen;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.studio.planeshift.common.course.WorldDefinition;
import com.studio.planeshift.common.course.WorldRegistry;
import com.studio.planeshift.common.entity.Koopaling;
import com.studio.planeshift.common.registry.ModEntities;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * The tower bosses are actually in the game.
 *
 * <p>The previous commit added eight of them — entity, rig, eight palettes, renderer — and nothing
 * spawned any of them. This exists so that cannot silently become true again: it asserts the towers
 * are placed, that each world's is a different sibling, and that a tower never lands on the same
 * course as a castle.
 */
class KoopalingTowerTest {

    @Test
    void everyWorldWithEnoughCoursesHasATower() {
        List<String> missing = new ArrayList<>();
        for (WorldDefinition world : WorldRegistry.allWorlds()) {
            if (world.courseCount() < 3) {
                continue;
            }
            String tower = KoopalingTower.towerCourseIdOf(world);
            if (tower == null || !KoopalingTower.isTowerCourse(tower)) {
                missing.add(world.worldId());
            }
        }
        assertEquals(List.of(), missing, "these worlds have no tower");
    }

    @Test
    void aTowerIsNeverAlsoACastle() {
        // Both dispatch by replacing the whole course. If one id satisfied both, whichever check
        // ran first would win and the other room would silently never be built.
        List<String> clashes = new ArrayList<>();
        for (WorldDefinition world : WorldRegistry.allWorlds()) {
            String tower = KoopalingTower.towerCourseIdOf(world);
            if (tower != null && tower.equals(world.bossCourseId())) {
                clashes.add(world.worldId() + ": " + tower);
            }
        }
        assertEquals(List.of(), clashes, "tower and castle on the same course");
    }

    @Test
    void anOrdinaryCourseIsNotMistakenForATower() {
        for (WorldDefinition world : WorldRegistry.allWorlds()) {
            assertTrue(!KoopalingTower.isTowerCourse(world.courseIds().get(0)),
                    world.courseIds().get(0) + " is not a tower");
        }
    }

    @Test
    void eachWorldFightsADifferentSibling() {
        // Five worlds all holding the same boss would make eight variants pointless. There are
        // more siblings than worlds, so this should hold comfortably.
        Set<Koopaling> seen = new HashSet<>();
        for (int world = 0; world < WorldRegistry.worldCount(); world++) {
            seen.add(Koopaling.forWorld(world));
        }
        assertEquals(WorldRegistry.worldCount(), seen.size(),
                "two worlds hold the same tower boss: " + seen);
    }

    @Test
    void theBossIsPlacedAndCarriesItsVariant() {
        // The variant is not implied by the entity type -- all eight share one -- so a spawn that
        // forgot to configure it would put the same sibling in every tower in the game.
        for (int world = 0; world < 6; world++) {
            List<CourseCanvas.EntitySpawn> bosses = KoopalingTower.build(world).entities().stream()
                    .filter(spawn -> spawn.type() == ModEntities.KOOPALING.get())
                    .toList();
            assertEquals(1, bosses.size(), "world " + world + ": expected exactly one tower boss");
            assertNotNull(bosses.get(0).configure(),
                    "world " + world + ": boss spawned without setting which sibling it is");
            assertEquals(SegmentLibrary.GENERATED_TAG, bosses.get(0).tag(),
                    "world " + world + ": boss would survive a rebuild and be joined by another");
        }
    }

    @Test
    void theFloorIsUnbroken() {
        // A tower boss is a fight, not a platforming section. A hole in this floor means losing to
        // the room rather than to the boss.
        CourseCanvas c = KoopalingTower.build(0);
        List<Integer> holes = new ArrayList<>();
        for (int x = 0; x <= 40; x++) {
            if (!c.blocks().containsKey(CourseCanvas.key(x, 0, 0))) {
                holes.add(x);
            }
        }
        assertEquals(List.of(), holes, "gaps in the tower floor");
    }

    @Test
    void theFlagpoleTopIsReachableHereToo() {
        // Same rule as every other finish in the game: without its own staircase the tower would
        // be the one place the top band and its 1-Up cannot be taken.
        CourseCanvas c = KoopalingTower.build(0);
        int flagX = -1;
        for (var e : c.blocks().entrySet()) {
            if (e.getValue().is(com.studio.planeshift.common.registry.ModBlocks.FLAG_POLE.get())) {
                flagX = CourseCanvas.unpackX(e.getKey());
                break;
            }
        }
        assertTrue(flagX > 0, "no flagpole in the tower");

        int highest = Integer.MIN_VALUE;
        for (int x = flagX - 12; x < flagX; x++) {
            for (int y = 11; y >= 0; y--) {
                if (c.blocks().containsKey(CourseCanvas.key(x, y, 0))) {
                    highest = Math.max(highest, y);
                    break;
                }
            }
        }
        assertEquals(7, highest, "no staircase reaching jumping range of the top of the pole");
    }
}
