package com.studio.planeshift.server.gen;

import com.studio.planeshift.common.block.FlagPoleBlock;
import com.studio.planeshift.common.registry.ModBlocks;
import com.studio.planeshift.common.registry.ModEntities;
import com.studio.planeshift.common.registry.ModItems;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The castle at the end of a world, with something in it.
 *
 * <p>Every world's last course is its boss course: {@code WorldDefinition.bossCourseId} names it,
 * clearing it is what unlocks the next world, and {@code CourseCompletionService} gives it a
 * send-off that no other course gets. It was an ordinary generated level with a flagpole. Bowser
 * was registered, modelled, textured, given a renderer, a spawn egg and a fire projectile, and was
 * spawned by nothing anywhere in the game.
 *
 * <p>So the progression already told the player they were fighting their way to a castle, and the
 * castle was a corridor. This is the room it was always describing.
 *
 * <p>Hand-built rather than composed, for the same reason {@link ToadHouseRoom} is: an arena has
 * one shape and is about a single encounter, and the segment library exists to recombine parts
 * across hundreds of courses. There is nothing here for a composer to vary.
 */
public final class BossArena {

    /** Distance from the lane centre to the walls. */
    private static final int HALF = GenContext.LANE_HALF_WIDTH;

    /** The approach, before the bridge. Long enough to see what is waiting. */
    private static final int BRIDGE_FROM = 15;

    /**
     * The last tile of the bridge, immediately west of the axe.
     *
     * <p>{@code AxeBlock} walks its collapse westward from the axe and stops at the first tile that
     * is not bridge, so the bridge has to end exactly one column short of the axe or the cascade
     * never starts. It also gives up after MAX_BRIDGE_LENGTH tiles, which is why the span is
     * fifteen and not longer.
     */
    private static final int BRIDGE_TO = 30;

    /** Where the axe sits, on the far bank. */
    private static final int AXE_X = 31;

    /**
     * The flagpole, past the axe.
     *
     * <p>Far enough past it that the finish staircase clears the axe entirely. At 38 the staircase
     * ran from x=28 to x=34 and overwrote the axe at x=31 -- it is written after it -- so the
     * arena had no axe in it at all and the bridge could never be dropped.
     */
    private static final int FLAG_X = 43;

    /** How far the room runs. */
    private static final int END = 50;

    /**
     * Where the pillars stand, along the bridge.
     *
     * <p>Spaced four apart, which is wider than a jump is long, so crossing between two of them is
     * a decision with a cost rather than something that happens by accident. They sit inside the
     * bridge span so they are cover during the fight and go down with it when the axe is taken.
     */
    private static final int[] PILLARS = {17, 21, 25, 29};

    /**
     * Pillars on the walk-in, which is where the clown car is.
     *
     * <p>The bridge pillars are cover during the Bowser fight; these are cover on the approach,
     * and the approach is the only place the flash can reach anyone. Without them the set-piece
     * would be a hazard with no answer, which is the mod's own characteristic bug wearing a
     * different hat: correct, tested, and impossible to get past.
     */
    private static final int[] APPROACH_PILLARS = {1, 5, 9, 13};

    /**
     * Which world gets the clown car.
     *
     * <p>One castle, not five. The wiki is specific that this happens in the final castle, and the
     * reason it works there is that it happens once -- a hazard with no defeat condition is a
     * set-piece the first time and a tax every time after.
     */
    private static final int CLOWN_CAR_WORLD = 4;

    private BossArena() {
    }

    /** True when this course is the one at the end of a world. */
    public static boolean isBossCourse(String courseId) {
        com.studio.planeshift.common.course.WorldDefinition world =
                com.studio.planeshift.common.course.WorldRegistry.worldForCourse(courseId);
        return world != null && courseId.equals(world.bossCourseId());
    }

    /** Which world this boss course ends, or 0 if it cannot be placed. */
    public static int worldIndexOf(String courseId) {
        com.studio.planeshift.common.course.WorldDefinition world =
                com.studio.planeshift.common.course.WorldRegistry.worldForCourse(courseId);
        if (world == null) {
            return 0;
        }
        return Math.max(0, com.studio.planeshift.common.course.WorldRegistry
                .worldIndex(world.worldId()));
    }

    /**
     * How much is in the arena, by world.
     *
     * <p>There are five boss courses and they were all the same room, which makes the fifth castle
     * the first castle again with a longer walk in front of it. The escalation is deliberately in
     * the furniture rather than in Bowser: the player has spent a world learning to read firebars
     * and Podoboos, and the castle is the place those get asked about together.
     *
     * <p>Indexed by world, clamped, so adding a sixth world degrades to the hardest arena rather
     * than to an empty one.
     */
    private record Escalation(int firebars, int podoboos, int boos) {

        private static final Escalation[] BY_WORLD = {
            new Escalation(0, 0, 0),   // grassland: the encounter alone, so it can be learned
            new Escalation(1, 0, 0),   // frozen
            new Escalation(2, 2, 0),   // volcano: the pit starts fighting back
            new Escalation(2, 2, 2),   // haunted
            new Escalation(3, 3, 3),   // sky: everything at once
        };

        static Escalation forWorld(int worldIndex) {
            return BY_WORLD[Math.clamp(worldIndex, 0, BY_WORLD.length - 1)];
        }
    }

    /** Builds the arena into a canvas, ready for {@link CourseWriter}. */
    public static CourseCanvas build() {
        return build(0);
    }

    /** Builds the arena for a given world, which decides how much is in it. */
    public static CourseCanvas build(int worldIndex) {
        Escalation escalation = Escalation.forWorld(worldIndex);
        CourseCanvas c = new CourseCanvas();
        BlockState castle = ModBlocks.COURSE_CASTLE_BLOCK.get().defaultBlockState();
        BlockState lava = Blocks.LAVA.defaultBlockState();

        // Approach and far bank. Solid ground either side of the pit, so a player who falls has
        // clearly made a mistake rather than been given nowhere to stand.
        for (int x = -4; x < BRIDGE_FROM; x++) {
            lane(c, x, 0, castle);
        }
        for (int x = BRIDGE_TO + 1; x <= END; x++) {
            lane(c, x, 0, castle);
        }

        // The pit, and the bridge over it. The bridge is the arena floor: the whole encounter
        // happens on the thing the axe is about to remove.
        for (int x = BRIDGE_FROM; x <= BRIDGE_TO; x++) {
            for (int z = -HALF; z <= HALF; z++) {
                c.set(x, -3, z, lava);
                c.set(x, -4, z, castle);
            }
            lane(c, x, 0, castle);
        }

        // A Barrier Block charm on the approach, before the bridge.
        //
        // Barrier Block is the only Form in the defense category and it was creative-only, so no
        // player had ever held a defensive Form. The run-up to the one fight in the game is the
        // place it means the most, and it is a charm rather than a pickup so taking it is the
        // player deciding to spend it here.
        c.item(ModItems.BARRIER_CHARM.get(), 8.5D, 1.5D, 0.5D);

        // Bowser, on the bridge, facing back down it at the approaching player.
        c.spawn(ModEntities.BOWSER.get(), 23.5D, 1.0D, 0.5D, 90.0F, SegmentLibrary.GENERATED_TAG);

        // Firebars over the bridge, spread across the span rather than stacked, so they read as
        // separate clocks instead of one wall of fire.
        for (int i = 0; i < escalation.firebars(); i++) {
            int at = BRIDGE_FROM + 3 + i * (BRIDGE_TO - BRIDGE_FROM - 5)
                    / Math.max(1, escalation.firebars());
            c.spawn(ModEntities.FIREBAR.get(), at + 0.5D, 6.0D, 0.5D, 0.0F,
                    SegmentLibrary.GENERATED_TAG);
        }

        // Podoboos in the lava under the bridge. They are the reason the pit is worth looking at
        // rather than merely worth not falling into.
        for (int i = 0; i < escalation.podoboos(); i++) {
            int at = BRIDGE_FROM + 4 + i * 6;
            if (at <= BRIDGE_TO) {
                c.spawn(ModEntities.PODOBOO.get(), at + 0.5D, -2.0D, 0.5D, 0.0F,
                        SegmentLibrary.GENERATED_TAG);
            }
        }

        // Boos along the approach, so the walk in is no longer free.
        for (int i = 0; i < escalation.boos(); i++) {
            c.spawn(ModEntities.BOO.get(), 5.5D + i * 4, 3.0D, 0.5D, 0.0F,
                    SegmentLibrary.GENERATED_TAG);
        }

        // No gaps in the bridge, at any difficulty, however tempting. AxeBlock walks its collapse
        // westward and stops at the first tile that is not bridge, so a gap would leave everything
        // beyond it standing and the castle would end with half a bridge in mid-air.

        // The axe. Taking it drops the bridge; it does not end the course, because AxeBlock is
        // also ordinary mid-course furniture in CASTLE_BRIDGE and making it a finish line would
        // cut every lava course short the moment the player reached one -- the same mistake as
        // treating the keyhole as a locked door.
        c.set(AXE_X, 1, 0, ModBlocks.AXE_BLOCK.get().defaultBlockState());

        // The flagpole past it, which is what actually ends the course. Walking to it after the
        // bridge falls is the victory lap.
        BlockState pole = ModBlocks.FLAG_POLE.get().defaultBlockState();
        c.set(FLAG_X, 1, 0, pole.setValue(FlagPoleBlock.PART, FlagPoleBlock.Part.BASE));
        for (int h = 2; h <= 7; h++) {
            c.set(FLAG_X, h, 0, pole.setValue(FlagPoleBlock.PART, FlagPoleBlock.Part.POLE));
        }
        c.set(FLAG_X, 8, 0, pole.setValue(FlagPoleBlock.PART, FlagPoleBlock.Part.TOP));

        // The staircase to the pole, so the top band is reachable here too. Without it a boss
        // course would be the one place in the game the 1-Up cannot be taken.
        for (int step = 0; step < 7; step++) {
            for (int h = 1; h <= step + 1; h++) {
                lane(c, FLAG_X - 10 + step, h, castle);
            }
        }

        // Pillars, standing in the back row of the lane.
        //
        // The wiki's account of the castle fights keeps mentioning hiding behind pillars, and the
        // World 1 castle footage shows grey columns standing in the hall. They go at z=HALF rather
        // than on the lane centre for two reasons: a pillar in the middle of a three-wide lane is
        // a wall, not cover, and the gaps between them are the shelter -- the player steps into
        // the back row where there is no column and is behind one from the camera's side.
        //
        // They are furniture now and load-bearing later: the seven-in-a-clown-car set-piece turns
        // the player to stone unless they are behind something, and this is the something.
        for (int at : APPROACH_PILLARS) {
            for (int y = 1; y <= 12; y++) {
                c.set(at, y, HALF, castle);
            }
        }
        for (int at : PILLARS) {
            for (int y = 1; y <= 12; y++) {
                c.set(at, y, HALF, castle);
            }
        }

        // Walls and a ceiling, so it reads as a room. Open at negative Z like every other interior
        // in the mod, because that is where the camera is.
        for (int x = -4; x <= END; x++) {
            for (int y = 1; y <= 12; y++) {
                c.set(x, y, HALF + 1, castle);
            }
            lane(c, x, 13, castle);
        }

        // The clown car, over the final castle's approach.
        //
        // It cannot be beaten and does not try to fight: it flashes, and a player with a column
        // between themselves and it is not hit. That is the whole encounter, and it is a different
        // verb -- get past -- from everything else in the game, which is what earns it a place at
        // the last castle rather than a place in the rotation.
        if (worldIndex >= CLOWN_CAR_WORLD) {
            c.spawn(ModEntities.CLOWN_CAR.get(), 7.5D, 9.0D, 0.5D, 90.0F,
                    SegmentLibrary.GENERATED_TAG);
        }

        c.marker("flag", FLAG_X, 1, 0);
        c.marker("boss", 23, 1, 0);
        return c;
    }

    /** Lays one column across the walkable lane. */
    private static void lane(CourseCanvas c, int x, int y, BlockState state) {
        for (int z = -HALF; z <= HALF; z++) {
            c.set(x, y, z, state);
        }
    }
}
