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

    /** Half-width of the playable lane, in blocks either side of centre. */
    public static final int LANE_HALF_WIDTH = 1;

    private final CourseTheme theme;
    private final Palette palette;
    private final int difficulty;
    private final RandomGenerator random;

    public GenContext(CourseTheme theme, int difficulty, RandomGenerator random) {
        this.theme = theme;
        this.palette = Palette.forTheme(theme);
        this.difficulty = Math.clamp(difficulty, 0, 4);
        this.random = random;
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

    /** Lays solid ground across the lane at one column, with fill beneath it. */
    public void ground(CourseCanvas canvas, int x, int topY) {
        canvas.setLane(x, topY, palette.surface(), LANE_HALF_WIDTH);
        for (int depth = 1; depth <= 3; depth++) {
            canvas.setLane(x, topY - depth, palette.fill(), LANE_HALF_WIDTH);
        }
    }

    /** Drops the theme's hazard into the bottom of a pit, where the theme has one. */
    public void pitFloor(CourseCanvas canvas, int x, int floorY) {
        if (palette.hazard() == null) {
            return;
        }
        canvas.setLane(x, floorY - 4, palette.hazard(), LANE_HALF_WIDTH);
    }
}
