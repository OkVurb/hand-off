package com.studio.planeshift.server.gen;

import static com.studio.planeshift.server.gen.Segment.Tag;

import com.studio.planeshift.common.course.CourseTheme;
import com.studio.planeshift.common.entity.MovingPlatformEntity;
import com.studio.planeshift.common.registry.ModBlocks;
import com.studio.planeshift.common.registry.ModEntities;
import com.studio.planeshift.common.registry.ModItems;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The catalogue of level pieces a course can be built from.
 *
 * <p>Each entry is one idea. That is the discipline that separates this from noise: a segment
 * should be describable in a sentence — "a pit with a moving platform across it", "a row of pipes
 * with Piranha Plants", "a wall of bricks you have to break through" — and a player should be able
 * to look at it and understand what is being asked before they commit to it.
 *
 * <p>Segments never mention a specific block. They ask {@link GenContext.Palette} for a surface, a
 * platform, an accent, and the theme supplies it. One library therefore serves six visually
 * distinct biomes rather than six near-duplicates, and adding a seventh theme requires no changes
 * here at all.
 *
 * <p>Every segment is responsible for laying its own ground. A segment that leaves a hole leaves a
 * hole on purpose.
 */
public final class SegmentLibrary {

    /** Tag applied to every generated entity so a course reload can clear them. */
    public static final String GENERATED_TAG = "planeshift.generated_course";

    private SegmentLibrary() {
    }

    // ------------------------------------------------------------------ helpers

    private static Segment.SegmentSpec def(String id, int width, int exitRise, int difficulty,
                                           Tag... tags) {
        return new Segment.SegmentSpec(id, width, 0, exitRise, difficulty, Set.of(tags));
    }

    /** Solid ground for the whole width of a segment. */
    private static void floor(CourseCanvas c, int x0, int width, int y, GenContext ctx) {
        for (int i = 0; i < width; i++) {
            ctx.ground(c, x0 + i, y);
        }
    }

    /** A floating platform: one row of the theme's platform block. */
    private static void platform(CourseCanvas c, int x0, int width, int y, GenContext ctx) {
        for (int i = 0; i < width; i++) {
            c.setLane(x0 + i, y, ctx.palette().platform(), ctx.halfWidth());
        }
    }

    /** A trail of coins, which is how a level tells the player where the intended route goes. */
    private static void coinTrail(CourseCanvas c, int x0, int count, int y, int step) {
        for (int i = 0; i < count; i++) {
            c.item(ModItems.COIN.get(), x0 + i * step + 0.5D, y + 0.5D, 0.5D);
        }
    }

    /** A coin arc over a pit — the classic "this jump is possible" signal. */
    private static void coinArc(CourseCanvas c, int x0, int width, int y) {
        for (int i = 0; i < width; i++) {
            double t = (i + 0.5D) / width;
            double lift = Math.sin(t * Math.PI) * 3.0D;
            c.item(ModItems.COIN.get(), x0 + i + 0.5D, y + 1.5D + lift, 0.5D);
        }
    }

    private static void mob(CourseCanvas c, EntityType<?> type, int x, int y, float facing) {
        c.spawn(type, x + 0.5D, y, 0.5D, facing, GENERATED_TAG);
    }

    /** Enemies suited to a theme, so a snow course is not full of Hammer Bros. */
    private static List<EntityType<?>> cast(CourseTheme theme) {
        return switch (theme) {
            case GRASS -> List.of(ModEntities.GOOMBA.get(), ModEntities.KOOPA.get());
            case DESERT -> List.of(ModEntities.SPINY.get(), ModEntities.GOOMBA.get());
            case SNOW -> List.of(ModEntities.BUZZY_BEETLE.get(), ModEntities.KOOPA.get());
            case LAVA -> List.of(ModEntities.HAMMER_BRO.get(), ModEntities.BUZZY_BEETLE.get());
            case UNDERGROUND -> List.of(ModEntities.BUZZY_BEETLE.get(), ModEntities.SPINY.get());
            case GHOST_HOUSE -> List.of(ModEntities.BOO.get(), ModEntities.KOOPA.get());
        };
    }

    // ------------------------------------------------------------------ rest

    /** Plain ground. Every level needs places where nothing is being asked. */
    static final Segment FLAT_RUN = new Segment() {
        public SegmentSpec spec() {
            return def("flat_run", 8, 0, 0, Tag.REST);
        }

        public void build(CourseCanvas c, int x, int y, GenContext ctx) {
            floor(c, x, 8, y, ctx);
            if (ctx.chance(0.5D)) {
                coinTrail(c, x + 2, 4, y + 2, 1);
            }
            ctx.decorate(c, x, 8, y);
        }
    };

    /**
     * A short breather after something hard. Deliberately shorter than {@link #FLAT_RUN} so the
     * composer can insert recovery without padding the course out.
     */
    static final Segment BREATHER = new Segment() {
        public SegmentSpec spec() {
            return def("breather", 5, 0, 0, Tag.REST);
        }

        public void build(CourseCanvas c, int x, int y, GenContext ctx) {
            floor(c, x, 5, y, ctx);
        }
    };

    /** Rolling ground. Reads as terrain rather than as a corridor, and costs the player nothing. */
    static final Segment GENTLE_HILL = new Segment() {
        public SegmentSpec spec() {
            return def("gentle_hill", 10, 0, 0, Tag.REST, Tag.CLIMB);
        }

        public void build(CourseCanvas c, int x, int y, GenContext ctx) {
            ctx.decorate(c, x, 10, y);
            int[] profile = {0, 1, 1, 2, 2, 2, 1, 1, 0, 0};
            for (int i = 0; i < profile.length; i++) {
                ctx.ground(c, x + i, y + profile[i]);
                // Fill the step face so a rise never leaves a floating lip.
                for (int fillY = y; fillY < y + profile[i]; fillY++) {
                    c.setLane(x + i, fillY, ctx.palette().fill(), ctx.halfWidth());
                }
            }
        }
    };

    // ------------------------------------------------------------------ gaps

    /** One pit. The first thing any platformer teaches. */
    static final Segment SINGLE_GAP = new Segment() {
        public SegmentSpec spec() {
            return def("single_gap", 11, 0, 1, Tag.GAP);
        }

        public void build(CourseCanvas c, int x, int y, GenContext ctx) {
            int pit = Math.min(3 + ctx.difficulty(), 5);
            int lead = (11 - pit) / 2;
            floor(c, x, lead, y, ctx);
            for (int i = 0; i < pit; i++) {
                ctx.pitFloor(c, x + lead + i, y);
            }
            floor(c, x + lead + pit, 11 - lead - pit, y, ctx);
            coinArc(c, x + lead, pit, y);
        }
    };

    /** Two pits with a narrow island between them, so the landing itself is the challenge. */
    static final Segment DOUBLE_GAP = new Segment() {
        public SegmentSpec spec() {
            return def("double_gap", 16, 0, 2, Tag.GAP);
        }

        public void build(CourseCanvas c, int x, int y, GenContext ctx) {
            int pit = Math.min(3 + ctx.difficulty(), 5);
            int island = Math.max(2, 4 - ctx.difficulty() / 2);
            int cursor = x;
            floor(c, cursor, 3, y, ctx);
            cursor += 3;
            for (int i = 0; i < pit; i++) {
                ctx.pitFloor(c, cursor + i, y);
            }
            cursor += pit;
            floor(c, cursor, island, y, ctx);
            coinTrail(c, cursor, island, y + 2, 1);
            cursor += island;
            for (int i = 0; i < pit; i++) {
                ctx.pitFloor(c, cursor + i, y);
            }
            cursor += pit;
            floor(c, cursor, x + 16 - cursor, y, ctx);
        }
    };

    /** A pit too wide to clear, with a platform placed where the jump can reach it. */
    static final Segment GAP_WITH_PLATFORM = new Segment() {
        public SegmentSpec spec() {
            return def("gap_with_platform", 15, 0, 2, Tag.GAP, Tag.CLIMB);
        }

        public void build(CourseCanvas c, int x, int y, GenContext ctx) {
            floor(c, x, 4, y, ctx);
            for (int i = 4; i < 11; i++) {
                ctx.pitFloor(c, x + i, y);
            }
            platform(c, x + 6, 3, y + 2, ctx);
            coinTrail(c, x + 6, 3, y + 4, 1);
            floor(c, x + 11, 4, y, ctx);
        }
    };

    /** Three short pits in a row. Rhythm rather than reach: the player finds a cadence. */
    static final Segment STAGGERED_GAPS = new Segment() {
        public SegmentSpec spec() {
            return def("staggered_gaps", 18, 0, 2, Tag.GAP);
        }

        public void build(CourseCanvas c, int x, int y, GenContext ctx) {
            int cursor = x;
            for (int i = 0; i < 3; i++) {
                floor(c, cursor, 3, y, ctx);
                cursor += 3;
                for (int p = 0; p < 3; p++) {
                    ctx.pitFloor(c, cursor + p, y);
                }
                cursor += 3;
            }
            floor(c, cursor, x + 18 - cursor, y, ctx);
        }
    };

    // ------------------------------------------------------------------ climbs

    /** Stepped ground going up. Ends higher than it began, and the composer carries that forward. */
    static final Segment STAIRCASE_UP = new Segment() {
        public SegmentSpec spec() {
            return def("staircase_up", 8, 4, 1, Tag.CLIMB);
        }

        public void build(CourseCanvas c, int x, int y, GenContext ctx) {
            for (int i = 0; i < 8; i++) {
                int step = Math.min(i / 2, 4);
                ctx.ground(c, x + i, y + step);
                for (int fillY = y; fillY < y + step; fillY++) {
                    c.setLane(x + i, fillY, ctx.palette().fill(), ctx.halfWidth());
                }
            }
        }
    };

    static final Segment STAIRCASE_DOWN = new Segment() {
        public SegmentSpec spec() {
            return def("staircase_down", 8, -4, 0, Tag.CLIMB);
        }

        public void build(CourseCanvas c, int x, int y, GenContext ctx) {
            for (int i = 0; i < 8; i++) {
                int step = Math.min(i / 2, 4);
                ctx.ground(c, x + i, y - step);
                for (int fillY = y - step - 3; fillY < y - step; fillY++) {
                    c.setLane(x + i, fillY, ctx.palette().fill(), ctx.halfWidth());
                }
            }
        }
    };

    /** Floating platforms climbing away from the ground, with the ground still present below. */
    static final Segment PLATFORM_LADDER = new Segment() {
        public SegmentSpec spec() {
            return def("platform_ladder", 14, 0, 2, Tag.CLIMB);
        }

        public void build(CourseCanvas c, int x, int y, GenContext ctx) {
            floor(c, x, 14, y, ctx);
            for (int i = 0; i < 4; i++) {
                platform(c, x + 2 + i * 3, 2, y + 2 + i * 2, ctx);
                c.item(ModItems.COIN.get(), x + 2 + i * 3 + 0.5D, y + 4 + i * 2 + 0.5D, 0.5D);
            }
        }
    };

    // ------------------------------------------------------------------ blocks

    /** A row of question blocks and bricks overhead. The core Mario verb. */
    static final Segment QUESTION_ROW = new Segment() {
        public SegmentSpec spec() {
            return def("question_row", 10, 0, 0, Tag.BLOCKS);
        }

        public void build(CourseCanvas c, int x, int y, GenContext ctx) {
            floor(c, x, 10, y, ctx);
            BlockState brick = ModBlocks.BRICK_BLOCK.get().defaultBlockState();
            BlockState question = ModBlocks.QUESTION_BLOCK.get().defaultBlockState();
            for (int i = 0; i < 5; i++) {
                c.set(x + 3 + i, y + 4, 0, i == 2 ? question : brick);
            }
        }
    };

    /** A wall of bricks the player must break through, so the mechanic becomes mandatory. */
    static final Segment BRICK_WALL = new Segment() {
        public SegmentSpec spec() {
            return def("brick_wall", 9, 0, 1, Tag.BLOCKS);
        }

        public void build(CourseCanvas c, int x, int y, GenContext ctx) {
            floor(c, x, 9, y, ctx);
            BlockState brick = ModBlocks.BRICK_BLOCK.get().defaultBlockState();
            // A full-height wall with a one-block gap at head height: the player must break the
            // brick above to pass, which is the whole point of the segment.
            for (int h = 1; h <= 5; h++) {
                if (h == 1) {
                    continue;
                }
                c.setLane(x + 4, y + h, brick, ctx.halfWidth());
            }
            c.set(x + 4, y + 1, 0, brick);
        }
    };

    /** A coin block and a hidden block, the second only findable by jumping into empty air. */
    static final Segment HIDDEN_CACHE = new Segment() {
        public SegmentSpec spec() {
            return def("hidden_cache", 11, 0, 1, Tag.BLOCKS, Tag.SECRET);
        }

        public void build(CourseCanvas c, int x, int y, GenContext ctx) {
            floor(c, x, 11, y, ctx);
            c.set(x + 3, y + 4, 0, ModBlocks.COIN_BLOCK.get().defaultBlockState());
            c.set(x + 7, y + 4, 0, ModBlocks.HIDDEN_QUESTION_BLOCK.get().defaultBlockState());
            // A ledge only reachable by standing on the hidden block once revealed.
            platform(c, x + 8, 3, y + 8, ctx);
            coinTrail(c, x + 8, 3, y + 9, 1);
        }
    };

    // ------------------------------------------------------------------ enemies

    /** Enemies on open ground, where they can be seen coming and dealt with freely. */
    static final Segment ENEMY_LINE = new Segment() {
        public SegmentSpec spec() {
            return def("enemy_line", 12, 0, 1, Tag.ENEMY);
        }

        public void build(CourseCanvas c, int x, int y, GenContext ctx) {
            floor(c, x, 12, y, ctx);
            List<EntityType<?>> roster = cast(ctx.theme());
            int count = 1 + ctx.difficulty() / 2;
            for (int i = 0; i <= count; i++) {
                mob(c, roster.get(i % roster.size()), x + 3 + i * 3, y + 1, 90.0F);
            }
        }
    };

    /**
     * An enemy patrolling a narrow ledge between two pits.
     *
     * <p>Harder than the same enemy on open ground, because the safe answer — back off and wait —
     * costs the player their run-up. Combining two ideas is where difficulty should come from.
     */
    static final Segment LEDGE_PATROL = new Segment() {
        public SegmentSpec spec() {
            return def("ledge_patrol", 17, 0, 3, Tag.ENEMY, Tag.GAP);
        }

        public void build(CourseCanvas c, int x, int y, GenContext ctx) {
            floor(c, x, 4, y, ctx);
            for (int i = 4; i < 7; i++) {
                ctx.pitFloor(c, x + i, y);
            }
            floor(c, x + 7, 4, y, ctx);
            mob(c, cast(ctx.theme()).get(0), x + 8, y + 1, 90.0F);
            for (int i = 11; i < 14; i++) {
                ctx.pitFloor(c, x + i, y);
            }
            floor(c, x + 14, 3, y, ctx);
        }
    };

    /** A Hammer Bro on a raised perch, which today's perch clamp keeps it standing on. */
    static final Segment HAMMER_PERCH = new Segment() {
        public SegmentSpec spec() {
            return def("hammer_perch", 14, 0, 3, Tag.ENEMY, Tag.CLIMB);
        }

        public void build(CourseCanvas c, int x, int y, GenContext ctx) {
            floor(c, x, 14, y, ctx);
            platform(c, x + 5, 5, y + 4, ctx);
            mob(c, ModEntities.HAMMER_BRO.get(), x + 7, y + 5, 90.0F);
            // A route under the perch, so the player may choose to run past rather than fight.
            coinTrail(c, x + 5, 5, y + 2, 1);
        }
    };

    /** Pipes with Piranha Plants: timing, not reflexes. */
    static final Segment PIRANHA_PIPES = new Segment() {
        public SegmentSpec spec() {
            return def("piranha_pipes", 16, 0, 2, Tag.ENEMY, Tag.PIPE);
        }

        public void build(CourseCanvas c, int x, int y, GenContext ctx) {
            floor(c, x, 16, y, ctx);
            BlockState pipe = ModBlocks.WARP_PIPE.get().defaultBlockState();
            for (int i = 0; i < 3; i++) {
                int px = x + 3 + i * 5;
                int height = 2 + (i % 2);
                for (int h = 1; h <= height; h++) {
                    c.setLane(px, y + h, pipe, ctx.halfWidth());
                }
                mob(c, ModEntities.PIRANHA_PLANT.get(), px, y + height + 1, 0.0F);
            }
        }
    };

    // ------------------------------------------------------------------ moving

    /** A pit crossed by a platform that runs along the lane. */
    static final Segment MOVING_CROSSING = new Segment() {
        public SegmentSpec spec() {
            return def("moving_crossing", 18, 0, 2, Tag.MOVING, Tag.GAP);
        }

        public void build(CourseCanvas c, int x, int y, GenContext ctx) {
            floor(c, x, 5, y, ctx);
            for (int i = 5; i < 13; i++) {
                ctx.pitFloor(c, x + i, y);
            }
            floor(c, x + 13, 5, y, ctx);
            c.spawn(ModEntities.MOVING_PLATFORM.get(), x + 6.5D, y + 2, 0.5D, 0.0F, GENERATED_TAG);
            // The platform sweeps along the lane; declare the band so the reachability proof knows
            // the pit is crossable rather than treating the crossing as a wall.
            c.movingSurface(x + 4, x + 13, y + 2);
        }
    };

    /** A vertical lift climb, where waiting is the skill. */
    static final Segment LIFT_SHAFT = new Segment() {
        public SegmentSpec spec() {
            return def("lift_shaft", 12, 6, 3, Tag.MOVING, Tag.CLIMB);
        }

        public void build(CourseCanvas c, int x, int y, GenContext ctx) {
            floor(c, x, 4, y, ctx);
            c.spawn(ModEntities.MOVING_PLATFORM.get(), x + 5.5D, y + 2, 0.5D, 0.0F, GENERATED_TAG);
            // A vertical lift: the sweep is a column, so declare a surface at each height it
            // reaches. This is the one segment where the platform is the only route, which is
            // exactly why the declaration has to be right.
            for (int lift = 2; lift <= 7; lift++) {
                c.movingSurface(x + 4, x + 8, y + lift);
            }
            // The exit ledge sits above; the lift is the only way onto it.
            platform(c, x + 8, 4, y + 6, ctx);
            for (int i = 8; i < 12; i++) {
                ctx.ground(c, x + i, y + 6);
            }
        }
    };

    // ------------------------------------------------------------------ unstable ground

    /** Donut blocks over a pit: the ground is there until you stand on it. */
    static final Segment DONUT_RUN = new Segment() {
        public SegmentSpec spec() {
            return def("donut_run", 16, 0, 2, Tag.UNSTABLE, Tag.GAP);
        }

        public void build(CourseCanvas c, int x, int y, GenContext ctx) {
            floor(c, x, 4, y, ctx);
            for (int i = 4; i < 12; i++) {
                ctx.pitFloor(c, x + i, y);
            }
            BlockState donut = ModBlocks.DONUT_BLOCK.get().defaultBlockState();
            for (int i = 0; i < 7; i++) {
                c.set(x + 4 + i, y + 1, 0, donut);
            }
            floor(c, x + 12, 4, y, ctx);
        }
    };

    /** Staggered donut blocks — the same idea, one step harder, for later in a course. */
    static final Segment DONUT_STAGGER = new Segment() {
        public SegmentSpec spec() {
            return def("donut_stagger", 16, 0, 4, Tag.UNSTABLE, Tag.GAP, Tag.CLIMB);
        }

        public void build(CourseCanvas c, int x, int y, GenContext ctx) {
            floor(c, x, 4, y, ctx);
            for (int i = 4; i < 12; i++) {
                ctx.pitFloor(c, x + i, y);
            }
            BlockState donut = ModBlocks.DONUT_BLOCK.get().defaultBlockState();
            int[] heights = {1, 2, 3, 2, 1, 2, 3};
            for (int i = 0; i < heights.length; i++) {
                c.set(x + 4 + i, y + heights[i], 0, donut);
            }
            floor(c, x + 12, 4, y, ctx);
        }
    };

    /** Conveyor belts, alternating direction. Movement stops being free. */
    static final Segment CONVEYOR_RUN = new Segment() {
        public SegmentSpec spec() {
            return def("conveyor_run", 14, 0, 2, Tag.UNSTABLE);
        }

        public void build(CourseCanvas c, int x, int y, GenContext ctx) {
            floor(c, x, 14, y, ctx);
            BlockState east = ModBlocks.CONVEYOR_BELT.get().defaultBlockState()
                    .setValue(HorizontalDirectionalBlock.FACING, Direction.EAST);
            BlockState west = ModBlocks.CONVEYOR_BELT.get().defaultBlockState()
                    .setValue(HorizontalDirectionalBlock.FACING, Direction.WEST);
            for (int i = 0; i < 10; i++) {
                c.setLane(x + 2 + i, y + 1, (i / 3) % 2 == 0 ? west : east, ctx.halfWidth());
            }
        }
    };

    /** Note blocks that bounce the player to a ledge they cannot otherwise reach. */
    static final Segment NOTE_BOUNCE = new Segment() {
        public SegmentSpec spec() {
            return def("note_bounce", 13, 0, 2, Tag.UNSTABLE, Tag.CLIMB);
        }

        public void build(CourseCanvas c, int x, int y, GenContext ctx) {
            floor(c, x, 13, y, ctx);
            BlockState note = ModBlocks.NOTE_BLOCK.get().defaultBlockState();
            for (int i = 0; i < 3; i++) {
                c.set(x + 3 + i * 3, y + 1, 0, note);
            }
            platform(c, x + 4, 4, y + 7, ctx);
            coinTrail(c, x + 4, 4, y + 8, 1);
        }
    };

    // ------------------------------------------------------------------ overhead

    /** A ceiling low enough to force a crouch, which the course crouch exists for. */
    static final Segment LOW_CEILING = new Segment() {
        public SegmentSpec spec() {
            return def("low_ceiling", 10, 0, 1, Tag.OVERHEAD);
        }

        public void build(CourseCanvas c, int x, int y, GenContext ctx) {
            floor(c, x, 10, y, ctx);
            for (int i = 3; i < 7; i++) {
                c.setLane(x + i, y + 2, ctx.palette().accent(), ctx.halfWidth());
                c.setLane(x + i, y + 3, ctx.palette().accent(), ctx.halfWidth());
            }
        }
    };

    /** Thwomps over a corridor: wait, read the timing, commit. */
    static final Segment THWOMP_HALL = new Segment() {
        public SegmentSpec spec() {
            return def("thwomp_hall", 16, 0, 3, Tag.OVERHEAD, Tag.ENEMY);
        }

        public void build(CourseCanvas c, int x, int y, GenContext ctx) {
            floor(c, x, 16, y, ctx);
            for (int i = 0; i < 2 + ctx.difficulty() / 2; i++) {
                mob(c, ModEntities.THWOMP.get(), x + 4 + i * 5, y + 7, 0.0F);
            }
            // A ceiling to hang them from, so they read as part of the architecture.
            for (int i = 2; i < 14; i++) {
                c.setLane(x + i, y + 9, ctx.palette().accent(), ctx.halfWidth());
            }
        }
    };

    /** A firebar to time. Rotational hazards ask a different question than moving ones. */
    static final Segment FIREBAR_GATE = new Segment() {
        public SegmentSpec spec() {
            return def("firebar_gate", 14, 0, 4, Tag.OVERHEAD);
        }

        public void build(CourseCanvas c, int x, int y, GenContext ctx) {
            floor(c, x, 14, y, ctx);
            // A pillar for the bar to pivot on, so the rotation has a visible centre.
            for (int h = 1; h <= 4; h++) {
                c.setLane(x + 7, y + h, ctx.palette().accent(), ctx.halfWidth());
            }
            c.spawn(ModEntities.FIREBAR.get(), x + 7.5D, y + 5, 0.5D, 0.0F, GENERATED_TAG);
        }
    };

    // ------------------------------------------------------------------ secrets

    /** A vine hidden in a block, leading to a coin route above the course. */
    static final Segment VINE_SECRET = new Segment() {
        public SegmentSpec spec() {
            return def("vine_secret", 12, 0, 1, Tag.SECRET, Tag.BLOCKS);
        }

        public void build(CourseCanvas c, int x, int y, GenContext ctx) {
            floor(c, x, 12, y, ctx);
            c.set(x + 5, y + 4, 0, ModBlocks.SECRET_VINE.get().defaultBlockState());
            // The reward the vine leads to: a cloud shelf lined with coins.
            for (int i = 0; i < 10; i++) {
                c.setLane(x + 2 + i, y + 18, ModBlocks.COURSE_CLOUD_BLOCK.get().defaultBlockState(),
                        ctx.halfWidth());
            }
            coinTrail(c, x + 3, 8, y + 19, 1);
        }
    };

    // ------------------------------------------------------------------ set pieces

    /** The castle bridge and axe. One per course, only where the theme earns it. */
    static final Segment CASTLE_BRIDGE = new Segment() {
        public SegmentSpec spec() {
            return def("castle_bridge", 24, 0, 4, Tag.SETPIECE, Tag.OVERHEAD);
        }

        public void build(CourseCanvas c, int x, int y, GenContext ctx) {
            floor(c, x, 4, y, ctx);
            BlockState castle = ModBlocks.COURSE_CASTLE_BLOCK.get().defaultBlockState();
            // A lava pit spanned by a bridge, with the axe past the far end.
            for (int i = 4; i < 20; i++) {
                c.setLane(x + i, y - 4, ctx.palette().hazard() != null
                        ? ctx.palette().hazard()
                        : ctx.palette().fill(), ctx.halfWidth());
                c.set(x + i, y, 0, castle);
            }
            c.spawn(ModEntities.FIREBAR.get(), x + 11.5D, y + 5, 0.5D, 0.0F, GENERATED_TAG);
            floor(c, x + 20, 4, y, ctx);
            c.set(x + 21, y + 1, 0, ModBlocks.AXE_BLOCK.get().defaultBlockState());
            // Archways, not walls. These were solid across the full lane from the floor up, which
            // sealed the corridor at both ends — the reachability sweep found 544 lava courses the
            // player could not walk past the castle entrance. A castle should frame the arena, and
            // framing means leaving the doorway open: solid only from head height upward.
            // Four, not three. A three-block start leaves a two-block doorway, which is exactly
            // the player's own height — so any approach even one block high (the axe pedestal
            // does this) clips their head on the arch. Three blocks of clearance means the
            // doorway is passable from either approach height.
            for (int h = 4; h <= 7; h++) {
                c.setLane(x + 2, y + h, castle, ctx.halfWidth());
                c.setLane(x + 22, y + h, castle, ctx.halfWidth());
            }
            // Side pillars carry the arch down to the floor outside the walkable lane, so it still
            // reads as architecture rather than as a floating slab.
            for (int h = 1; h <= 7; h++) {
                for (int side = -1; side <= 1; side += 2) {
                    c.set(x + 2, y + h, side * (ctx.halfWidth() + 1), castle);
                    c.set(x + 22, y + h, side * (ctx.halfWidth() + 1), castle);
                }
            }
        }
    };

    /** Wooden platform trellis crossing with upper and lower routes over a pit. */
    static final Segment WOODEN_TRELLIS_CROSSING = new Segment() {
        public SegmentSpec spec() {
            return def("wooden_trellis_crossing", 16, 0, 2, Tag.CLIMB, Tag.GAP);
        }

        public void build(CourseCanvas c, int x, int y, GenContext ctx) {
            floor(c, x, 3, y, ctx);
            BlockState wood = ModBlocks.COURSE_WOOD_BLOCK.get().defaultBlockState();
            for (int i = 0; i < 3; i++) {
                c.setLane(x + 3 + i, y + 1, wood, ctx.halfWidth());
            }
            for (int i = 0; i < 3; i++) {
                c.setLane(x + 7 + i, y + 3, wood, ctx.halfWidth());
                c.item(ModItems.COIN.get(), x + 7 + i + 0.5D, y + 4.5D, 0.5D);
            }
            for (int i = 0; i < 3; i++) {
                c.setLane(x + 11 + i, y + 1, wood, ctx.halfWidth());
            }
            floor(c, x + 14, 2, y, ctx);
        }
    };

    /** Stepping stone pillars across a gap with coin rewards atop each pillar. */
    static final Segment STEPPING_STONE_HOPS = new Segment() {
        public SegmentSpec spec() {
            return def("stepping_stone_hops", 17, 0, 2, Tag.CLIMB, Tag.GAP);
        }

        public void build(CourseCanvas c, int x, int y, GenContext ctx) {
            floor(c, x, 3, y, ctx);
            BlockState stone = ModBlocks.COURSE_HARD_BLOCK.get().defaultBlockState();
            for (int h = -2; h <= 1; h++) {
                c.setLane(x + 4, y + h, stone, ctx.halfWidth());
                c.setLane(x + 5, y + h, stone, ctx.halfWidth());
            }
            c.item(ModItems.COIN.get(), x + 4.5D, y + 2.5D, 0.5D);

            for (int h = -2; h <= 2; h++) {
                c.setLane(x + 7, y + h, stone, ctx.halfWidth());
                c.setLane(x + 8, y + h, stone, ctx.halfWidth());
            }
            c.item(ModItems.COIN.get(), x + 7.5D, y + 3.5D, 0.5D);

            for (int h = -2; h <= 2; h++) {
                c.setLane(x + 10, y + h, stone, ctx.halfWidth());
                c.setLane(x + 11, y + h, stone, ctx.halfWidth());
            }
            c.item(ModItems.COIN.get(), x + 10.5D, y + 3.5D, 0.5D);

            for (int h = -2; h <= 1; h++) {
                c.setLane(x + 13, y + h, stone, ctx.halfWidth());
                c.setLane(x + 14, y + h, stone, ctx.halfWidth());
            }
            c.item(ModItems.COIN.get(), x + 13.5D, y + 2.5D, 0.5D);

            floor(c, x + 15, 2, y, ctx);
        }
    };

    /** A bridge made of Rotating Blocks: runs solid unless hit or ground-pounded. */
    static final Segment ROTATING_BRIDGE = new Segment() {
        public SegmentSpec spec() {
            return def("rotating_bridge", 15, 0, 2, Tag.BLOCKS, Tag.GAP);
        }

        public void build(CourseCanvas c, int x, int y, GenContext ctx) {
            floor(c, x, 3, y, ctx);
            BlockState rot = ModBlocks.ROTATING_BLOCK.get().defaultBlockState();
            for (int i = 0; i < 9; i++) {
                c.set(x + 3 + i, y, 0, rot);
                c.item(ModItems.COIN.get(), x + 3 + i + 0.5D, y + 1.5D, 0.5D);
            }
            floor(c, x + 12, 3, y, ctx);
        }
    };

    /** Multi-tier canopy with an elevated floating platform route over enemies below. */
    static final Segment MULTI_TIER_CANOPY = new Segment() {
        public SegmentSpec spec() {
            return def("multi_tier_canopy", 18, 0, 3, Tag.CLIMB, Tag.ENEMY);
        }

        public void build(CourseCanvas c, int x, int y, GenContext ctx) {
            floor(c, x, 18, y, ctx);
            List<EntityType<?>> enemies = cast(ctx.theme());
            if (!enemies.isEmpty()) {
                mob(c, enemies.get(ctx.random().nextInt(enemies.size())), x + 9, y + 1, 0.0F);
            }
            platform(c, x + 3, 3, y + 3, ctx);
            coinTrail(c, x + 3, 3, y + 4, 1);
            platform(c, x + 7, 4, y + 5, ctx);
            c.set(x + 9, y + 6, 0, ModBlocks.QUESTION_BLOCK.get().defaultBlockState());
            platform(c, x + 12, 3, y + 3, ctx);
            coinTrail(c, x + 12, 3, y + 4, 1);
        }
    };

    /** Hidden prize vault behind an illusory secret passage wall. */
    static final Segment SECRET_PRIZE_VAULT = new Segment() {
        public SegmentSpec spec() {
            return def("secret_prize_vault", 14, 0, 1, Tag.SECRET, Tag.BLOCKS);
        }

        public void build(CourseCanvas c, int x, int y, GenContext ctx) {
            floor(c, x, 14, y, ctx);
            BlockState brick = ModBlocks.BRICK_BLOCK.get().defaultBlockState();
            BlockState fake = ModBlocks.SECRET_PASSAGE.get().defaultBlockState();
            c.setLane(x + 5, y + 1, fake, ctx.halfWidth());
            c.setLane(x + 5, y + 2, fake, ctx.halfWidth());
            c.setLane(x + 5, y + 3, brick, ctx.halfWidth());
            c.setLane(x + 5, y + 4, brick, ctx.halfWidth());
            c.set(x + 8, y + 1, 0, ModBlocks.PRIZE_CACHE.get().defaultBlockState());
            c.set(x + 10, y + 4, 0, ModBlocks.QUESTION_BLOCK.get().defaultBlockState());
            c.item(ModItems.EXTRA_PIP.get(), x + 8.5D, y + 2.5D, 0.5D);
            coinTrail(c, x + 7, 3, y + 2, 1);
        }
    };

    /** A P-Switch room where hitting the switch converts bricks into a coin field. */
    static final Segment P_SWITCH_BONUS_ROOM = new Segment() {
        public SegmentSpec spec() {
            return def("p_switch_bonus_room", 16, 0, 2, Tag.SECRET, Tag.UNSTABLE);
        }

        public void build(CourseCanvas c, int x, int y, GenContext ctx) {
            floor(c, x, 16, y, ctx);
            c.set(x + 3, y + 1, 0, ModBlocks.P_SWITCH.get().defaultBlockState());
            BlockState brick = ModBlocks.BRICK_BLOCK.get().defaultBlockState();
            for (int i = 0; i < 8; i++) {
                c.set(x + 6 + i, y + 2, 0, brick);
                c.set(x + 6 + i, y + 4, 0, brick);
            }
            c.set(x + 10, y + 5, 0, ModBlocks.QUESTION_BLOCK.get().defaultBlockState());
        }
    };

    /** An invisible block revealing a spring pad launching player to an elevated cloud highway. */
    static final Segment HIDDEN_SPRING_HIGHWAY = new Segment() {
        public SegmentSpec spec() {
            return def("hidden_spring_highway", 18, 0, 2, Tag.SECRET, Tag.CLIMB);
        }

        public void build(CourseCanvas c, int x, int y, GenContext ctx) {
            floor(c, x, 18, y, ctx);
            c.set(x + 4, y + 3, 0, ModBlocks.HIDDEN_QUESTION_BLOCK.get().defaultBlockState());
            c.set(x + 4, y + 1, 0, ModBlocks.SPRING_PAD.get().defaultBlockState());
            BlockState cloud = ModBlocks.COURSE_CLOUD_BLOCK.get().defaultBlockState();
            for (int i = 0; i < 10; i++) {
                c.setLane(x + 6 + i, y + 8, cloud, ctx.halfWidth());
            }
            coinTrail(c, x + 7, 6, y + 9, 1);
            c.item(ModItems.THREE_UP.get(), x + 14.5D, y + 9.5D, 0.5D);
        }
    };

    /** Illusory secret passage between pipes hiding an invincibility star. */
    static final Segment ILLUSORY_PIPE_CACHE = new Segment() {
        public SegmentSpec spec() {
            return def("illusory_pipe_cache", 15, 0, 1, Tag.SECRET, Tag.PIPE);
        }

        public void build(CourseCanvas c, int x, int y, GenContext ctx) {
            floor(c, x, 15, y, ctx);
            BlockState pipe = ModBlocks.WARP_PIPE.get().defaultBlockState();
            BlockState fake = ModBlocks.SECRET_PASSAGE.get().defaultBlockState();
            BlockState stone = ModBlocks.COURSE_HARD_BLOCK.get().defaultBlockState();
            c.setLane(x + 3, y + 1, pipe, ctx.halfWidth());
            c.setLane(x + 3, y + 2, pipe, ctx.halfWidth());
            c.setLane(x + 6, y + 1, fake, ctx.halfWidth());
            c.setLane(x + 6, y + 2, fake, ctx.halfWidth());
            c.setLane(x + 6, y + 3, stone, ctx.halfWidth());
            c.set(x + 8, y + 1, 0, ModBlocks.COIN_BLOCK.get().defaultBlockState());
            c.item(ModItems.STAR_POWER.get(), x + 10.5D, y + 1.5D, 0.5D);
            c.setLane(x + 12, y + 1, pipe, ctx.halfWidth());
        }
    };

    // ------------------------------------------------------------------ catalogue

    /** Every segment the composer may choose from, in a stable order. */
    public static List<Segment> all() {
        List<Segment> list = new ArrayList<>();
        list.add(FLAT_RUN);
        list.add(BREATHER);
        list.add(GENTLE_HILL);
        list.add(SINGLE_GAP);
        list.add(DOUBLE_GAP);
        list.add(GAP_WITH_PLATFORM);
        list.add(STAGGERED_GAPS);
        list.add(STAIRCASE_UP);
        list.add(STAIRCASE_DOWN);
        list.add(PLATFORM_LADDER);
        list.add(QUESTION_ROW);
        list.add(BRICK_WALL);
        list.add(HIDDEN_CACHE);
        list.add(ENEMY_LINE);
        list.add(LEDGE_PATROL);
        list.add(HAMMER_PERCH);
        list.add(PIRANHA_PIPES);
        list.add(MOVING_CROSSING);
        list.add(LIFT_SHAFT);
        list.add(DONUT_RUN);
        list.add(DONUT_STAGGER);
        list.add(CONVEYOR_RUN);
        list.add(NOTE_BOUNCE);
        list.add(LOW_CEILING);
        list.add(THWOMP_HALL);
        list.add(FIREBAR_GATE);
        list.add(VINE_SECRET);
        list.add(WOODEN_TRELLIS_CROSSING);
        list.add(STEPPING_STONE_HOPS);
        list.add(ROTATING_BRIDGE);
        list.add(MULTI_TIER_CANOPY);
        list.add(SECRET_PRIZE_VAULT);
        list.add(P_SWITCH_BONUS_ROOM);
        list.add(HIDDEN_SPRING_HIGHWAY);
        list.add(ILLUSORY_PIPE_CACHE);
        return list;
    }

    /** Set pieces, which the composer places at most one of. */
    public static List<Segment> setPieces() {
        return List.of(CASTLE_BRIDGE);
    }
}
