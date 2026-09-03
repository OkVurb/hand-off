package com.studio.planeshift.server.gen;

import com.studio.planeshift.common.course.CourseTheme;
import com.studio.planeshift.common.registry.ModBlocks;
import java.util.random.RandomGenerator;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Everything a segment needs to draw itself that is not geometry: the theme's blocks, how hard the
 * course should be, and a seeded random.
 *
 * <p>The random is passed in rather than created per segment so a course is reproducible end to
 * end. A player who dies and retries must get the identical course back — route memory is most of
 * what a platformer rewards, and a level that reshuffles between attempts is unlearnable.
 */
public final class GenContext {

    /**
     * Half-width of a 2.5D lane: three blocks wide, which is exactly enough to walk down and not
     * enough to have opinions about depth.
     */
    public static final int LANE_HALF_WIDTH = 1;

    /**
     * Half-width of a 3D course.
     *
     * <p>Nine blocks across. 3D Mario levels are still linear ribbons rather than open worlds —
     * 3D World in particular is a corridor with width — so a 3D course here is the same segment
     * spine with room to move around in. That is why the two modes can share one segment library
     * instead of needing two: widening the ribbon does not change what any piece of it means.
     */
    public static final int WIDE_HALF_WIDTH = 4;

    private final CourseTheme theme;
    private final Palette palette;
    private final int difficulty;
    private final RandomGenerator random;
    private final int halfWidth;

    public GenContext(CourseTheme theme, int difficulty, RandomGenerator random) {
        this(theme, difficulty, random, LANE_HALF_WIDTH);
    }

    public GenContext(CourseTheme theme, int difficulty, RandomGenerator random, int halfWidth) {
        this.theme = theme;
        this.palette = Palette.forTheme(theme);
        this.difficulty = Math.clamp(difficulty, 0, 4);
        this.random = random;
        this.halfWidth = halfWidth;
    }

    /** How wide the course is, either side of the centre line. */
    public int halfWidth() {
        return halfWidth;
    }

    /** Whether this course is being built wide enough to move around in. */
    public boolean isWide() {
        return halfWidth > LANE_HALF_WIDTH;
    }

    public CourseTheme theme() {
        return theme;
    }

    public Palette palette() {
        return palette;
    }

    /** 0 for the opening world, 4 for the last. Segments use it to scale their own internals. */
    public int difficulty() {
        return difficulty;
    }

    public RandomGenerator random() {
        return random;
    }

    public int range(int minInclusive, int maxInclusive) {
        if (maxInclusive <= minInclusive) {
            return minInclusive;
        }
        return minInclusive + random.nextInt(maxInclusive - minInclusive + 1);
    }

    public boolean chance(double probability) {
        return random.nextDouble() < probability;
    }

    /**
     * The blocks a theme is built from.
     *
     * <p>Split by role rather than by name so segments never mention a specific block: a segment
     * asks for "the surface" or "a platform", and the theme decides what that is. That is what
     * makes one segment library serve six visually distinct biomes instead of six near-copies.
     *
     * @param surface  the walkable top of the ground
     * @param fill     what sits beneath the surface
     * @param accent   decorative structure — pillars, frames, steps
     * @param platform floating platforms, which must read as separate from the ground
     * @param hazard   the thing at the bottom of a pit, if the theme has one
     */
    public record Palette(BlockState surface, BlockState fill, BlockState accent,
                          BlockState platform, BlockState hazard) {

        static Palette forTheme(CourseTheme theme) {
            return switch (theme) {
                case GRASS -> new Palette(
                        ModBlocks.COURSE_GRASS_BLOCK.get().defaultBlockState(),
                        Blocks.DIRT.defaultBlockState(),
                        ModBlocks.BRICK_BLOCK.get().defaultBlockState(),
                        ModBlocks.COURSE_CLOUD_BLOCK.get().defaultBlockState(),
                        null);
                case DESERT -> new Palette(
                        ModBlocks.COURSE_SAND_BLOCK.get().defaultBlockState(),
                        Blocks.SANDSTONE.defaultBlockState(),
                        Blocks.ORANGE_TERRACOTTA.defaultBlockState(),
                        ModBlocks.COURSE_SAND_BLOCK.get().defaultBlockState(),
                        null);
                case SNOW -> new Palette(
                        ModBlocks.COURSE_SNOW_BLOCK.get().defaultBlockState(),
                        Blocks.PACKED_ICE.defaultBlockState(),
                        Blocks.LIGHT_BLUE_CONCRETE.defaultBlockState(),
                        ModBlocks.COURSE_SNOW_BLOCK.get().defaultBlockState(),
                        null);
                case LAVA -> new Palette(
                        ModBlocks.COURSE_CASTLE_BLOCK.get().defaultBlockState(),
                        Blocks.BLACKSTONE.defaultBlockState(),
                        ModBlocks.COURSE_MAGMA_BLOCK.get().defaultBlockState(),
                        ModBlocks.COURSE_CASTLE_BLOCK.get().defaultBlockState(),
                        Blocks.LAVA.defaultBlockState());
                case UNDERGROUND -> new Palette(
                        ModBlocks.COURSE_CASTLE_BLOCK.get().defaultBlockState(),
                        Blocks.DEEPSLATE.defaultBlockState(),
                        Blocks.PURPLE_TERRACOTTA.defaultBlockState(),
                        ModBlocks.COURSE_CASTLE_BLOCK.get().defaultBlockState(),
                        null);
                case GHOST_HOUSE -> new Palette(
                        ModBlocks.COURSE_CASTLE_BLOCK.get().defaultBlockState(),
                        Blocks.DARK_OAK_PLANKS.defaultBlockState(),
                        Blocks.DARK_OAK_LOG.defaultBlockState(),
                        ModBlocks.COURSE_CASTLE_BLOCK.get().defaultBlockState(),
                        null);
            };
        }
    }

    /** Lays solid ground across the full course width at one column, with fill beneath it. */
    public void ground(CourseCanvas canvas, int x, int topY) {
        canvas.setLane(x, topY, palette.surface(), halfWidth);
        for (int depth = 1; depth <= 3; depth++) {
            canvas.setLane(x, topY - depth, palette.fill(), halfWidth);
        }
    }

    /**
     * Scatters purely decorative blocks along a stretch of ground.
     *
     * <p>None of these do anything, which is the point. A course built only from blocks that all
     * do something reads as a machine rather than a place — every surface becomes a promise, and
     * the player stops trusting that anything is just scenery. Giving them things to correctly
     * ignore is what makes the question block worth looking at.
     *
     * <p>Placed with {@code setIfEmpty} so decoration never overwrites a segment's real geometry,
     * and driven by the seeded random so it is part of the course's identity rather than noise
     * that changes on a retry.
     */
    public void decorate(CourseCanvas canvas, int x0, int width, int topY) {
        for (int i = 0; i < width; i++) {
            if (!random.nextBoolean() || !random.nextBoolean()) {
                continue;
            }
            int x = x0 + i;
            int edge = halfWidth;
            BlockState piece = switch (random.nextInt(6)) {
                case 0 -> ModBlocks.COURSE_CRATE.get().defaultBlockState();
                case 1 -> ModBlocks.COURSE_HEDGE.get().defaultBlockState();
                case 2 -> ModBlocks.COURSE_LAMP.get().defaultBlockState();
                case 3 -> ModBlocks.COURSE_PILLAR.get().defaultBlockState();
                case 4 -> ModBlocks.COURSE_LATTICE.get().defaultBlockState();
                default -> ModBlocks.COURSE_TRIM.get().defaultBlockState();
            };
            // Against the back edge of the lane, never in the middle: decoration the player has to
            // walk around is not decoration, it is an obstacle that looks like decoration.
            canvas.setIfEmpty(x, topY + 1, -edge, piece);
            if (isWide() && random.nextInt(3) == 0) {
                canvas.setIfEmpty(x, topY + 1, edge, piece);
            }
        }
    }

    /** Drops the theme's hazard into the bottom of a pit, where the theme has one. */
    public void pitFloor(CourseCanvas canvas, int x, int floorY) {
        if (palette.hazard() == null) {
            return;
        }
        canvas.setLane(x, floorY - 4, palette.hazard(), halfWidth);
    }
}
