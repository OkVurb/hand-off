package com.studio.planeshift.server.gen;

import com.studio.planeshift.common.block.FlagPoleBlock;
import com.studio.planeshift.common.course.WorldDefinition;
import com.studio.planeshift.common.course.WorldRegistry;
import com.studio.planeshift.common.entity.Koopaling;
import com.studio.planeshift.common.registry.ModBlocks;
import com.studio.planeshift.common.registry.ModEntities;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The tower halfway through a world.
 *
 * <p>Written immediately after {@code Koopaling}, because without it the eight tower bosses were
 * registered, modelled, textured, given eight distinct palettes and a renderer — and spawned by
 * nothing. That is the exact failure this project keeps producing and that the last twenty commits
 * have been undoing, and it would have been a poor joke to add another instance of it in the same
 * session.
 *
 * <p>Deliberately not a second {@code BossArena}. The castle is a set piece: a bridge over lava,
 * an axe, a collapse. A tower is a room with something in it, and the difference should be legible
 * the moment the player walks in — the floor is solid, there is nowhere to fall, and the only
 * problem in the room is the boss. That is what makes the castle feel like an escalation later.
 */
public final class KoopalingTower {

    /** Distance from the lane centre to the walls. */
    private static final int HALF = GenContext.LANE_HALF_WIDTH;

    /** The approach, before the chamber. Long enough to see what is waiting. */
    private static final int CHAMBER_FROM = 12;

    /** Where the boss stands. */
    private static final int BOSS_X = 24;

    /** The flagpole, past the boss, so the fight cannot simply be run around. */
    private static final int FLAG_X = 38;

    /** How far the room runs. */
    private static final int END = 45;

    /** Interior height. Tall enough for the one that drops from the ceiling to have a ceiling. */
    private static final int CEILING = 12;

    private KoopalingTower() {
    }

    /** True when this course is a world's mid-point tower. */
    public static boolean isTowerCourse(String courseId) {
        WorldDefinition world = WorldRegistry.worldForCourse(courseId);
        return world != null && courseId.equals(towerCourseIdOf(world));
    }

    /**
     * The tower is the middle course of a world.
     *
     * <p>Computed rather than declared, so a world with a different number of courses still gets
     * exactly one tower and it still lands in the middle. Never the last course, which is already
     * the castle: a world whose boss course and tower course were the same id would build one room
     * on top of the other.
     */
    public static String towerCourseIdOf(WorldDefinition world) {
        int count = world.courseCount();
        if (count < 3) {
            // Too short to have a middle. Two courses are an opening and a castle.
            return null;
        }
        return world.courseIds().get(count / 2);
    }

    /** Which world this tower belongs to, or 0 if it cannot be placed. */
    public static int worldIndexOf(String courseId) {
        WorldDefinition world = WorldRegistry.worldForCourse(courseId);
        if (world == null) {
            return 0;
        }
        return Math.max(0, WorldRegistry.worldIndex(world.worldId()));
    }

    /** Builds the tower for a given world, which decides which sibling is waiting in it. */
    public static CourseCanvas build(int worldIndex) {
        CourseCanvas c = new CourseCanvas();
        BlockState stone = ModBlocks.COURSE_CASTLE_BLOCK.get().defaultBlockState();
        BlockState tile = ModBlocks.COURSE_TILE.get().defaultBlockState();

        // Unbroken floor, the whole way. A tower boss is a fight, not a platforming section, and
        // giving the player a pit to fall in would mean losing to the room rather than to the boss.
        for (int x = -4; x <= END; x++) {
            for (int z = -HALF; z <= HALF; z++) {
                c.set(x, 0, z, x < CHAMBER_FROM ? stone : tile);
                c.set(x, CEILING, z, stone);
            }
            // Back wall only. The camera looks in from negative Z, the same as every other interior.
            for (int y = 1; y < CEILING; y++) {
                c.set(x, y, HALF + 1, stone);
            }
        }

        // End walls, so it is a chamber rather than a corridor that stops.
        for (int y = 1; y < CEILING; y++) {
            for (int z = -HALF; z <= HALF + 1; z++) {
                c.set(-4, y, z, stone);
                c.set(END, y, z, stone);
            }
        }

        // Lamps, because a sealed stone room is a dark one and the boss is the thing that has to
        // be readable in it.
        for (int x = CHAMBER_FROM; x < END; x += 7) {
            c.set(x, CEILING - 1, HALF, ModBlocks.COURSE_LAMP.get().defaultBlockState());
        }

        // The boss. Which sibling is a function of the world, so a player fights them in order.
        final Koopaling sibling = Koopaling.forWorld(worldIndex);
        c.spawn(ModEntities.KOOPALING.get(), BOSS_X + 0.5D, 1.0D, 0.5D, 90.0F,
                SegmentLibrary.GENERATED_TAG,
                entity -> {
                    if (entity instanceof com.studio.planeshift.common.entity.KoopalingEntity boss) {
                        boss.setVariant(sibling);
                    }
                });
        c.marker("tower_boss_" + sibling.id(), BOSS_X, 1, 0);

        // The pole, past the boss.
        BlockState pole = ModBlocks.FLAG_POLE.get().defaultBlockState();
        c.set(FLAG_X, 1, 0, pole.setValue(FlagPoleBlock.PART, FlagPoleBlock.Part.BASE));
        for (int h = 2; h <= 7; h++) {
            c.set(FLAG_X, h, 0, pole.setValue(FlagPoleBlock.PART, FlagPoleBlock.Part.POLE));
        }
        c.set(FLAG_X, 8, 0, pole.setValue(FlagPoleBlock.PART, FlagPoleBlock.Part.TOP));

        // Its staircase. Every other finish in the game has one, and a tower without it would be
        // the one place the top band and its 1-Up cannot be taken.
        for (int step = 0; step < 7; step++) {
            for (int h = 1; h <= step + 1; h++) {
                for (int z = -HALF; z <= HALF; z++) {
                    c.set(FLAG_X - 10 + step, h, z, stone);
                }
            }
        }

        c.marker("flag", FLAG_X, 1, 0);
        return c;
    }
}
