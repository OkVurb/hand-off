package com.studio.planeshift.server.gen;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.studio.planeshift.common.block.OnOffSwitchBlock;
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

    @Test
    void theBridgeIsUnbrokenAtEveryDifficulty() {
        // The one rule the escalation must never break. AxeBlock walks its collapse westward and
        // stops at the first tile that is not bridge, so a gap anywhere in the span would leave
        // everything beyond it standing -- the castle would end with half a bridge hanging in the
        // air over the lava. Tempting to add gaps as a difficulty knob; this is why not.
        for (int world = 0; world < 8; world++) {
            CourseCanvas c = BossArena.build(world);
            int axeX = axeColumn(c);
            for (int x = 15; x < axeX; x++) {
                assertTrue(c.blocks().containsKey(CourseCanvas.key(x, 0, 0)),
                        "world " + world + ": gap in the bridge at x=" + x);
            }
        }
    }

    @Test
    void laterCastlesAskMoreThanEarlierOnes() {
        // Five worlds all building the same room makes the fifth castle the first castle with a
        // longer walk in front of it.
        int first = BossArena.build(0).entities().size();
        int last = BossArena.build(4).entities().size();
        assertTrue(last > first,
                "the last castle has no more in it than the first (" + first + " vs " + last + ")");
    }

    @Test
    void theFirstCastleIsJustTheEncounter() {
        // Whatever else escalates, the first boss a player ever meets should be the fight and
        // nothing else, so the fight itself is what they learn.
        assertEquals(1, BossArena.build(0).entities().size(),
                "the first castle has hazards in it as well as Bowser");
    }

    @Test
    void anExtraWorldWouldGetTheHardestArenaRatherThanAnEmptyOne() {
        // The table is indexed by world and clamped. A sixth world added later must not fall off
        // the end into a castle containing nothing.
        assertTrue(BossArena.build(99).entities().size() > 1,
                "a world past the end of the escalation table builds an empty castle");
    }

    @Test
    void everythingInTheArenaCanBeCleanedUp() {
        List<String> problems = new ArrayList<>();
        for (int world = 0; world < 6; world++) {
            for (CourseCanvas.EntitySpawn spawn : BossArena.build(world).entities()) {
                if (!SegmentLibrary.GENERATED_TAG.equals(spawn.tag())) {
                    problems.add("world " + world + ": " + spawn.type().getDescriptionId()
                            + " tagged " + spawn.tag());
                }
            }
        }
        assertEquals(List.of(), problems,
                "these would survive a rebuild and accumulate on every attempt");
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

    /**
     * The enormous Bowser has something to stand on.
     *
     * <p>He is held five blocks behind the play plane, and for a long time the arena laid floor
     * across the three-block lane and nothing else -- so a background boss stood over a hole, fell,
     * and was killed by his own out-of-world check before the player got near him. The fight the
     * whole progression points at was ending before it started, and nothing failed, because an
     * arena with no boss left in it is still an arena.
     *
     * <p>Asserted along the whole room rather than at one column, because he tracks the player
     * from end to end and a ledge with a gap in it is the same bug in a smaller place.
     */
    @Test
    void theLastCastleHasFloorWhereTheBackgroundBossStands() {
        CourseCanvas c = BossArena.build(WorldRegistry.allWorlds().size() - 1);
        List<String> holes = new ArrayList<>();
        for (int x = -4; x <= 50; x++) {
            for (int z = 2; z <= 7; z++) {
                if (!c.blocks().containsKey(CourseCanvas.key(x, 0, z))) {
                    holes.add(x + "," + z);
                }
            }
        }
        assertEquals(List.of(), holes, "the background boss would fall through these");
    }

    /** The other four castles are unchanged: no ledge, and the wall still close in. */
    @Test
    void ordinaryCastlesAreNotWidened() {
        CourseCanvas c = BossArena.build(0);
        assertTrue(c.blocks().containsKey(CourseCanvas.key(0, 1, 2)),
                "an ordinary castle keeps its wall one block off the lane");
        assertTrue(!c.blocks().containsKey(CourseCanvas.key(0, 0, 5)),
                "an ordinary castle has no backdrop ledge, because it has no background boss");
    }

    /**
     * The last castle's climb falls away under the player; its supports do not.
     *
     * <p>Both halves matter. If the treads were stone there is no phase two, only a staircase; if
     * the whole column were donut a single missed step would take the staircase with it and leave
     * the player waiting at the bottom for blocks to time back in, which is a soft-lock wearing a
     * respawn timer.
     */
    @Test
    void theLastCastleClimbIsMadeOfBlocksThatFall() {
        CourseCanvas c = BossArena.build(WorldRegistry.allWorlds().size() - 1);
        for (int i = 0; i < 7; i++) {
            int x = 33 + i;
            assertEquals(ModBlocks.DONUT_BLOCK.get(),
                    c.blocks().get(CourseCanvas.key(x, i + 1, 0)).getBlock(),
                    "the tread at x=" + x + " should fall away");
            if (i > 0) {
                assertEquals(ModBlocks.COURSE_CASTLE_BLOCK.get(),
                        c.blocks().get(CourseCanvas.key(x, i, 0)).getBlock(),
                        "the support under x=" + x + " should not");
            }
        }
    }

    /**
     * The ending is reachable, and only from the top of the climb.
     *
     * <p>The switch turns the ledge off and drops Super Bowser out of the world. If it were
     * placed anywhere the player could reach before climbing, the last castle would end by walking
     * up to a button -- which is how this project's bugs usually look from the outside: correct,
     * tested, and skipping the part that was the point.
     */
    @Test
    void theLastCastleEndsWithASwitchAtTheTopOfTheClimb() {
        CourseCanvas c = BossArena.build(WorldRegistry.allWorlds().size() - 1);
        assertEquals(ModBlocks.ON_OFF_SWITCH.get(),
                c.blocks().get(CourseCanvas.key(40, 8, 0)).getBlock(),
                "the switch should sit beside the top tread");
        assertEquals(ModBlocks.ON_OFF_BLOCK.get(),
                c.blocks().get(CourseCanvas.key(39, 0, 5)).getBlock(),
                "the ledge the switch turns off should be what the boss stands on");
        // The one that actually matters, and it reads both heights back out of the canvas rather
        // than restating them: a check written as two literals passes whatever the arena does. The
        // first version of this placement sat nine blocks up, one outside the switch's own vertical
        // reach -- it would have been hit, it would have made its noise, and the boss would have
        // stood on a floor that never went away.
        int switchY = Integer.MIN_VALUE;
        int ledgeY = Integer.MIN_VALUE;
        for (int y = -8; y <= 16; y++) {
            for (int x = -4; x <= 50; x++) {
                if (ModBlocks.ON_OFF_SWITCH.get().equals(block(c, x, y, 0))) {
                    switchY = y;
                }
                if (ModBlocks.ON_OFF_BLOCK.get().equals(block(c, x, y, 5))) {
                    ledgeY = y;
                }
            }
        }
        assertTrue(switchY != Integer.MIN_VALUE && ledgeY != Integer.MIN_VALUE,
                "expected both a switch and a ledge in the last castle");
        assertTrue(Math.abs(switchY - ledgeY) <= OnOffSwitchBlock.RANGE_Y,
                "the switch at y=" + switchY + " cannot reach the ledge at y=" + ledgeY);
    }

    /** The block at a position, or null where the canvas laid nothing. */
    private static net.minecraft.world.level.block.Block block(CourseCanvas c, int x, int y, int z) {
        var state = c.blocks().get(CourseCanvas.key(x, y, z));
        return state == null ? null : state.getBlock();
    }
}
