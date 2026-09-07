package com.studio.planeshift.server.gen;

import com.studio.planeshift.common.course.CourseTheme;
import com.studio.planeshift.common.registry.ModBlocks;
import com.studio.planeshift.common.registry.ModFluids;
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

    /**
     * The theme of the world this course sits in, which is not always the theme of the course.
     *
     * <p>An underground stretch inside the desert is still desert: the reference tints its cave
     * interiors with the world around them -- ochre under the sand, brown-grey under the volcano,
     * blue-green under the flooded tower -- so that dropping into a cave reads as going below
     * <em>this</em> place rather than teleporting into the one grey cave every world shares.
     */
    private final CourseTheme worldTheme;
    private final Palette palette;
    private final int difficulty;
    private final RandomGenerator random;
    private final int halfWidth;

    public GenContext(CourseTheme theme, int difficulty, RandomGenerator random) {
        this(theme, difficulty, random, LANE_HALF_WIDTH);
    }

    public GenContext(CourseTheme theme, int difficulty, RandomGenerator random, int halfWidth) {
        this(theme, theme, difficulty, random, halfWidth);
    }

    /** As above, but for a course whose interior should be tinted by the world it sits in. */
    public GenContext(CourseTheme theme, CourseTheme worldTheme, int difficulty,
                      RandomGenerator random, int halfWidth) {
        this.theme = theme;
        this.worldTheme = worldTheme;
        this.palette = Palette.forTheme(theme, worldTheme);
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

    /** The world this course sits in. Equal to {@link #theme()} unless set apart deliberately. */
    public CourseTheme worldTheme() {
        return worldTheme;
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

        /** What a cave floor is cut from, in each world. */
        private static BlockState undergroundSurface(CourseTheme world) {
            return switch (world) {
                case DESERT -> ModBlocks.COURSE_SANDSTONE.get().defaultBlockState();
                case SNOW -> ModBlocks.COURSE_ICE_BLOCK.get().defaultBlockState();
                case LAVA -> ModBlocks.COURSE_BASALT.get().defaultBlockState();
                case GHOST_HOUSE -> ModBlocks.COURSE_GHOST_BEAM.get().defaultBlockState();
                default -> ModBlocks.COURSE_CASTLE_BLOCK.get().defaultBlockState();
            };
        }

        /** The mass behind that floor. Deepstone stays the default: an unmarked world is rock. */
        private static BlockState undergroundFill(CourseTheme world) {
            return switch (world) {
                case DESERT -> ModBlocks.COURSE_SAND_BLOCK.get().defaultBlockState();
                case SNOW -> ModBlocks.COURSE_ICE_BLOCK.get().defaultBlockState();
                case LAVA -> ModBlocks.COURSE_BASALT.get().defaultBlockState();
                default -> ModBlocks.COURSE_DEEPSTONE.get().defaultBlockState();
            };
        }

        static Palette forTheme(CourseTheme theme) {
            return forTheme(theme, theme);
        }

        /**
         * The palette for a course, with interiors tinted by the world around them.
         *
         * <p>{@code UNDERGROUND} was one grey cave used by every world, which collapsed six
         * distinct interiors into one and made the most common transition in the game -- surface
         * to cave and back -- feel like leaving the world rather than going under it. The rock a
         * cave is cut through is the rock the world is made of, so the fill comes from the world
         * and only the structure stays shared.
         */
        static Palette forTheme(CourseTheme theme, CourseTheme world) {
            if (theme == CourseTheme.UNDERGROUND) {
                return new Palette(
                        undergroundSurface(world),
                        undergroundFill(world),
                        ModBlocks.BRICK_BLOCK.get().defaultBlockState(),
                        ModBlocks.COURSE_CASTLE_BLOCK.get().defaultBlockState(),
                        null);
            }
            return switch (theme) {
                case GRASS -> new Palette(
                        ModBlocks.COURSE_GRASS_BLOCK.get().defaultBlockState(),
                        ModBlocks.COURSE_DIRT_BLOCK.get().defaultBlockState(),
                        ModBlocks.BRICK_BLOCK.get().defaultBlockState(),
                        ModBlocks.COURSE_CLOUD_BLOCK.get().defaultBlockState(),
                        null);
                case DESERT -> new Palette(
                        ModBlocks.COURSE_SAND_BLOCK.get().defaultBlockState(),
                        ModBlocks.COURSE_SANDSTONE.get().defaultBlockState(),
                        ModBlocks.COURSE_DESERT_BRICK.get().defaultBlockState(),
                        ModBlocks.COURSE_SAND_BLOCK.get().defaultBlockState(),
                        null);
                case SNOW -> new Palette(
                        ModBlocks.COURSE_SNOW_BLOCK.get().defaultBlockState(),
                        ModBlocks.COURSE_ICE_BLOCK.get().defaultBlockState(),
                        ModBlocks.COURSE_CLOUD_BLOCK.get().defaultBlockState(),
                        ModBlocks.COURSE_SNOW_BLOCK.get().defaultBlockState(),
                        null);
                case LAVA -> new Palette(
                        ModBlocks.COURSE_CASTLE_BLOCK.get().defaultBlockState(),
                        ModBlocks.COURSE_BASALT.get().defaultBlockState(),
                        ModBlocks.COURSE_EMBER_BLOCK.get().defaultBlockState(),
                        ModBlocks.COURSE_CASTLE_BLOCK.get().defaultBlockState(),
                        ModFluids.LAVA_BLOCK.get().defaultBlockState());
                case UNDERGROUND -> new Palette(
                        ModBlocks.COURSE_CASTLE_BLOCK.get().defaultBlockState(),
                        ModBlocks.COURSE_DEEPSTONE.get().defaultBlockState(),
                        ModBlocks.BRICK_BLOCK.get().defaultBlockState(),
                        ModBlocks.COURSE_CASTLE_BLOCK.get().defaultBlockState(),
                        null);
                // Water reuses the ordinary land blocks on purpose. The reference builds its
                // underwater levels from the same green-capped terrain as its grass levels; what
                // makes them read as submerged is the light shafts, the fluid, the coral and the
                // cast, none of which is terrain art. Giving water its own tileset would have been
                // the expensive way to arrive at a worse-matched result.
                case WATER -> new Palette(
                        ModBlocks.COURSE_GRASS_BLOCK.get().defaultBlockState(),
                        ModBlocks.COURSE_DEEPSTONE.get().defaultBlockState(),
                        ModBlocks.COURSE_CORAL.get().defaultBlockState(),
                        ModBlocks.COURSE_GRASS_BLOCK.get().defaultBlockState(),
                        null);
                case GHOST_HOUSE -> new Palette(
                        ModBlocks.COURSE_CASTLE_BLOCK.get().defaultBlockState(),
                        ModBlocks.COURSE_WOOD_BLOCK.get().defaultBlockState(),
                        ModBlocks.COURSE_GHOST_BEAM.get().defaultBlockState(),
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
