package com.studio.planeshift.server.gen;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.studio.planeshift.common.course.WorldDefinition;
import com.studio.planeshift.common.course.WorldRegistry;
import com.studio.planeshift.common.registry.ModBlocks;
import com.studio.planeshift.common.registry.ModEntities;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * The castle at the end of a world has something in it.
 *
 * <p>Bowser was registered, modelled, textured, given a renderer, a spawn egg and a fire
 * projectile, and spawned by nothing anywhere in the game -- while every world's last course was
 * already designated a boss course, gating the next world and earning a send-off no other course
 * gets. Nothing failed, because a boss course with no boss is just a course.
 */
class BossArenaTest {

    @Test
    void everyWorldsLastCourseIsRecognisedAsABossCourse() {
        List<String> missed = new ArrayList<>();
        for (WorldDefinition world : WorldRegistry.allWorlds()) {
            if (!BossArena.isBossCourse(world.bossCourseId())) {
                missed.add(world.bossCourseId());
            }
        }
        assertEquals(List.of(), missed, "these boss courses would build as ordinary levels");
    }

    @Test
    void anOrdinaryCourseIsNotMistakenForABossCourse() {
        // The dispatch replaces the whole course, so a false positive here would silently turn a
        // normal level into an arena.
        for (WorldDefinition world : WorldRegistry.allWorlds()) {
            if (world.courseCount() > 1) {
                assertTrue(!BossArena.isBossCourse(world.courseIds().get(0)),
                        world.courseIds().get(0) + " is not a boss course");
            }
        }
    }

    @Test
    void theBridgeEndsExactlyOneColumnShortOfTheAxe() {
        // AxeBlock walks its collapse westward from the axe and stops at the first tile that is
        // not bridge. A one-column gap here and the axe would be a decoration: the bridge would
        // never fall, and the arena's whole payoff is that it does.
        CourseCanvas c = BossArena.build();
        int axeX = axeColumn(c);
        assertTrue(c.blocks().containsKey(CourseCanvas.key(axeX - 1, 0, 0)),
                "nothing for the collapse to start on, immediately west of the axe");
        assertTrue(c.blocks().get(CourseCanvas.key(axeX - 1, 0, 0))
                        .is(ModBlocks.COURSE_CASTLE_BLOCK.get()),
                "the tile west of the axe is not bridge, so the cascade stops at once");
    }

    @Test
    void thereIsABossAndItCanBeCleanedUp() {
        List<String> problems = new ArrayList<>();
        boolean found = false;
        for (CourseCanvas.EntitySpawn spawn : BossArena.build().entities()) {
            if (spawn.type() == ModEntities.BOWSER.get()) {
                found = true;
                if (!SegmentLibrary.GENERATED_TAG.equals(spawn.tag())) {
                    problems.add("Bowser tagged " + spawn.tag() + ", so a rebuild would add another");
                }
            }
        }
        assertTrue(found, "the boss arena has no boss in it");
        assertEquals(List.of(), problems);
    }

    @Test
    void theApproachAndFarBankAreWalkable() {
        CourseCanvas c = BossArena.build();
        int axeX = axeColumn(c);
        // Every column from the spawn to past the flag needs a floor. A hole in the approach is a
        // boss course the player cannot reach the boss in.
        List<Integer> holes = new ArrayList<>();
        for (int x = 0; x <= axeX + 10; x++) {
            if (!c.blocks().containsKey(CourseCanvas.key(x, 0, 0))) {
                holes.add(x);
            }
        }
        assertEquals(List.of(), holes, "gaps in the arena floor");
    }

    @Test
    void theFlagpoleTopIsReachableHereToo() {
        // The finish staircase is built into every generated course by CourseComposer, which this
        // room bypasses entirely. Without its own, a boss course would be the one place in the
        // game where the top band and its 1-Up cannot be taken.
        CourseCanvas c = BossArena.build();
        int flagX = flagColumn(c);
        int highest = Integer.MIN_VALUE;
        for (int x = flagX - 12; x < flagX; x++) {
            for (int y = 12; y >= 0; y--) {
                if (c.blocks().containsKey(CourseCanvas.key(x, y, 0))) {
                    highest = Math.max(highest, y);
                    break;
                }
            }
        }
        assertEquals(7, highest, "no staircase reaching jumping range of the top of the pole");
    }

    private static int axeColumn(CourseCanvas c) {
        for (var e : c.blocks().entrySet()) {
            if (e.getValue().is(ModBlocks.AXE_BLOCK.get())) {
                return CourseCanvas.unpackX(e.getKey());
            }
        }
        throw new AssertionError("no axe in the boss arena");
    }

    private static int flagColumn(CourseCanvas c) {
        for (var e : c.blocks().entrySet()) {
            if (e.getValue().is(ModBlocks.FLAG_POLE.get())) {
                return CourseCanvas.unpackX(e.getKey());
            }
        }
        throw new AssertionError("no flagpole in the boss arena");
    }
}
