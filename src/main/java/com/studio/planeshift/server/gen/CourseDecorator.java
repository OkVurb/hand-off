package com.studio.planeshift.server.gen;

import com.studio.planeshift.common.course.CourseTheme;
import com.studio.planeshift.common.registry.ModBlocks;
import java.util.random.RandomGenerator;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Puts scenery behind the lane, so a course is somewhere rather than something.
 *
 * <p>Generated courses have always had geometry and never had a <em>place</em>. Every block in one
 * was load-bearing: a platform to stand on, a hazard to avoid, a block to hit. Nothing was ever
 * simply built, which is why a 720-block course reads as an assault course rather than as a level
 * in a world — and it is why the mod accumulated eight decorative blocks that no course contained.
 *
 * <p><b>Everything here writes outside the lane.</b> The play plane is {@code z = 0} and the lane
 * is {@code +/- halfWidth}; decoration starts at {@link #NEAR_Z} and goes back from there, which is
 * inside {@code CourseWriter}'s cleared volume and outside anything {@code CourseReachability}
 * looks at. That is not a convenient accident, it is the design: scenery must never be able to make
 * a course harder, and the cheapest way to guarantee that is for the solver and the decorator to be
 * working on disjoint columns.
 *
 * <p>Written with {@code setIfEmpty}, so a segment that has deliberately built something out at
 * that depth keeps it.
 */
public final class CourseDecorator {

    /**
     * Nearest depth decoration may occupy.
     *
     * <p>Two, against a lane half-width of one. The gap of one column matters: scenery flush
     * against the play plane reads as part of the level and the player will try to stand on it.
     */
    public static final int NEAR_Z = 2;

    /** Furthest depth for ordinary props. */
    public static final int FAR_Z = 3;

    /**
     * The backdrop plane: big silhouettes outdoors, a decorated wall indoors.
     *
     * <p>Reference backgrounds run three bands, not two, and they are told apart by how washed out
     * they are rather than by how far back they sit. CourseWriter's clear width was widened to 4
     * to cover this, or scenery from a previous visit would survive behind the new course.
     */
    public static final int BACKDROP_Z = 4;

    /** How far apart decoration clusters are placed, before jitter. */
    private static final int SPACING = 9;

    private CourseDecorator() {
    }

    /**
     * Decorates a finished course.
     *
     * @param floorAt the per-column design floor, so scenery sits on the ground rather than
     *                floating at whatever height the course happened to start at
     */
    public static void decorate(CourseCanvas canvas, GenContext ctx, int[] floorAt,
                                int margin, int from, int to) {
        RandomGenerator random = ctx.random();
        // The backdrop, laid first so props sit in front of it. Interiors get a continuous
        // decorated wall; exteriors get silhouettes standing against the sky. The reference builds
        // these two cases completely differently, and treating every theme the same way -- which is
        // what scattering props at two depths did -- matches neither.
        backdrop(canvas, ctx, random, from, to, floorAt, margin);

        for (int x = from; x < to; x += SPACING) {
            int at = x + random.nextInt(SPACING / 2);
            if (at >= to) {
                break;
            }
            int slot = at + margin;
            if (slot < 0 || slot >= floorAt.length) {
                continue;
            }
            int floor = floorAt[slot];
            // Both sides, independently, so the course is not symmetrical about the lane.
            for (int side : new int[] {-1, 1}) {
                if (random.nextInt(3) == 0) {
                    continue;
                }
                place(canvas, ctx, random, at, floor, side);
            }
        }
    }

    private static void place(CourseCanvas canvas, GenContext ctx, RandomGenerator random,
                              int x, int floorY, int side) {
        int z = side * (random.nextInt(2) == 0 ? NEAR_Z : FAR_Z);
        // Whether this prop is on the back plane, so it can be drawn as distance rather than
        // merely placed at a distance. Two depths existed in the geometry and none in the image.
        boolean far = Math.abs(z) == FAR_Z;
        switch (ctx.theme()) {
            case GRASS -> hedgerow(canvas, random, x, floorY, z, far);
            case DESERT -> column(canvas, random, x, floorY, z, 4, far);
            case SNOW -> drift(canvas, random, x, floorY, z);
            case LAVA -> lit(canvas, random, x, floorY, z, true);
            case GHOST_HOUSE -> lit(canvas, random, x, floorY, z, false);
            case UNDERGROUND -> column(canvas, random, x, floorY, z, 6, far);
        }
    }

    /**
     * Lays the furthest background band across the whole course.
     *
     * <p>Two completely different treatments, because the reference has two. Indoors the
     * background is architecture: a continuous wall carrying a repeated motif of arches and
     * columns, low contrast, in the room's own colour. Outdoors it is negative space with
     * silhouettes standing in it -- hills and trees, flat and washed out, with sky between them.
     *
     * <p>Written with setIfEmpty and only at {@link #BACKDROP_Z}, so it can never touch the
     * playfield or the props in front of it.
     */
    private static void backdrop(CourseCanvas canvas, GenContext ctx, RandomGenerator random,
                                 int from, int to, int[] floorAt, int margin) {
        switch (ctx.theme()) {
            case GHOST_HOUSE, UNDERGROUND, LAVA ->
                    backWall(canvas, ctx, random, from, to, floorAt, margin);
            default -> skyline(canvas, random, from, to, floorAt, margin);
        }
    }

    /**
     * Indoor backdrop: a wall with arched openings.
     *
     * <p>The arch spacing is deliberately not the prop spacing. Two repeating rhythms at the same
     * interval read as one thing and the wall stops being a separate layer.
     */
    private static void backWall(CourseCanvas canvas, GenContext ctx, RandomGenerator random,
                                 int from, int to, int[] floorAt, int margin) {
        BlockState stone = ModBlocks.COURSE_CASTLE_BLOCK.get().defaultBlockState();
        BlockState opening = ModBlocks.COURSE_LATTICE.get().defaultBlockState();
        final int arch = 7;
        for (int x = from; x < to; x++) {
            int slot = x + margin;
            if (slot < 0 || slot >= floorAt.length) {
                continue;
            }
            int floor = floorAt[slot];
            for (int h = 1; h <= 9; h++) {
                boolean windowRow = h >= 3 && h <= 6;
                boolean windowCol = Math.floorMod(x, arch) >= 2 && Math.floorMod(x, arch) <= 4;
                canvas.setIfEmpty(x, floor + h, BACKDROP_Z,
                        windowRow && windowCol ? opening : stone);
            }
        }
    }

    /**
     * Outdoor backdrop: hills and trees as flat silhouettes, with sky left between them.
     *
     * <p>Sparse on purpose. A continuous line of scenery is a wall painted green; the gaps are
     * what make the shapes read as separate objects standing at a distance.
     */
    private static void skyline(CourseCanvas canvas, RandomGenerator random,
                                int from, int to, int[] floorAt, int margin) {
        int x = from;
        while (x < to) {
            int slot = x + margin;
            if (slot < 0 || slot >= floorAt.length) {
                x += 8;
                continue;
            }
            int floor = floorAt[slot];
            if (random.nextInt(3) == 0) {
                tree(canvas, random, x, floor);
                x += 5 + random.nextInt(4);
            } else {
                x += hill(canvas, random, x, floor) + 2 + random.nextInt(5);
            }
        }
    }

    /** A rounded mound. Returns its width so the caller can space the next shape past it. */
    private static int hill(CourseCanvas canvas, RandomGenerator random, int x, int floor) {
        BlockState mass = ModBlocks.COURSE_HEDGE_DISTANT.get().defaultBlockState();
        int width = 7 + random.nextInt(7);
        int peak = 3 + random.nextInt(3);
        for (int i = 0; i < width; i++) {
            // A rounded profile rather than a triangle: distance flattens a hill's shoulders, and
            // a triangle at this scale reads as a tent.
            double t = (double) i / (width - 1);
            int h = (int) Math.round(peak * Math.sin(Math.PI * t));
            for (int y = 1; y <= h; y++) {
                canvas.setIfEmpty(x + i, floor + y, BACKDROP_Z, mass);
            }
        }
        return width;
    }

    /** A trunk with a rounded crown. */
    private static void tree(CourseCanvas canvas, RandomGenerator random, int x, int floor) {
        BlockState trunk = ModBlocks.COURSE_WOOD_DISTANT.get().defaultBlockState();
        BlockState leaf = ModBlocks.COURSE_HEDGE_DISTANT.get().defaultBlockState();
        int height = 3 + random.nextInt(3);
        for (int h = 1; h <= height; h++) {
            canvas.setIfEmpty(x, floor + h, BACKDROP_Z, trunk);
        }
        int crown = floor + height;
        for (int dy = 0; dy <= 2; dy++) {
            int half = dy == 2 ? 1 : 2;
            for (int dx = -half; dx <= half; dx++) {
                canvas.setIfEmpty(x + dx, crown + dy, BACKDROP_Z, leaf);
            }
        }
    }

    /** Grass: bushes, and the occasional tall one so the skyline is not flat. */
    private static void hedgerow(CourseCanvas canvas, RandomGenerator random,
                                 int x, int floorY, int z, boolean far) {
        BlockState hedge = (far ? ModBlocks.COURSE_HEDGE_FAR : ModBlocks.COURSE_HEDGE)
                .get().defaultBlockState();
        int height = 1 + random.nextInt(3);
        for (int h = 1; h <= height; h++) {
            canvas.setIfEmpty(x, floorY + h, z, hedge);
        }
        if (random.nextInt(4) == 0) {
            // A cloud well above the skyline. Depth cue, and the only thing up there.
            BlockState cloud = (far ? ModBlocks.COURSE_CLOUD_BLOCK_FAR : ModBlocks.COURSE_CLOUD_BLOCK)
                    .get().defaultBlockState();
            int cy = floorY + 9 + random.nextInt(4);
            for (int i = 0; i < 3; i++) {
                canvas.setIfEmpty(x + i, cy, z, cloud);
            }
        }
    }

    /** Desert and underground: a standing column with a capital, and sometimes a crate at its foot. */
    private static void column(CourseCanvas canvas, RandomGenerator random,
                               int x, int floorY, int z, int maxHeight, boolean far) {
        BlockState pillar = (far ? ModBlocks.COURSE_PILLAR_FAR : ModBlocks.COURSE_PILLAR)
                .get().defaultBlockState();
        BlockState trim = ModBlocks.COURSE_TRIM.get().defaultBlockState();
        int height = 2 + random.nextInt(maxHeight);
        for (int h = 1; h <= height; h++) {
            canvas.setIfEmpty(x, floorY + h, z, h == height ? trim : pillar);
        }
        if (random.nextInt(3) == 0) {
            BlockState crate = ModBlocks.COURSE_CRATE.get().defaultBlockState();
            canvas.setIfEmpty(x + 1, floorY + 1, z, crate);
            if (random.nextInt(2) == 0) {
                canvas.setIfEmpty(x + 1, floorY + 2, z, crate);
            }
        }
    }

    /** Snow: low banks of cloud block, which reads as heaped snow at this scale. */
    private static void drift(CourseCanvas canvas, RandomGenerator random,
                              int x, int floorY, int z) {
        BlockState snow = ModBlocks.COURSE_CLOUD_BLOCK.get().defaultBlockState();
        int width = 2 + random.nextInt(3);
        for (int i = 0; i < width; i++) {
            int height = i == width / 2 ? 2 : 1;
            for (int h = 1; h <= height; h++) {
                canvas.setIfEmpty(x + i, floorY + h, z, snow);
            }
        }
        if (random.nextInt(3) == 0) {
            canvas.setIfEmpty(x, floorY + 3, z, ModBlocks.COURSE_LATTICE.get().defaultBlockState());
        }
    }

    /**
     * Castle and ghost house: a lamp on a bracket, and hangings.
     *
     * <p>The lamp is the point. Both of these themes are dark, and a light source behind the lane
     * throws the platforms into silhouette — which is the single most useful thing decoration can
     * do for readability in a side-on game.
     */
    private static void lit(CourseCanvas canvas, RandomGenerator random,
                            int x, int floorY, int z, boolean banners) {
        BlockState lamp = ModBlocks.COURSE_LAMP.get().defaultBlockState();
        BlockState trim = ModBlocks.COURSE_TRIM.get().defaultBlockState();
        int height = 3 + random.nextInt(3);
        for (int h = 1; h < height; h++) {
            canvas.setIfEmpty(x, floorY + h, z, trim);
        }
        canvas.setIfEmpty(x, floorY + height, z, lamp);

        if (banners && random.nextInt(2) == 0) {
            BlockState banner = ModBlocks.COURSE_BANNER.get().defaultBlockState();
            for (int h = height + 1; h <= height + 3; h++) {
                canvas.setIfEmpty(x + 2, floorY + h, z, banner);
            }
        } else if (!banners && random.nextInt(2) == 0) {
            BlockState lattice = ModBlocks.COURSE_LATTICE.get().defaultBlockState();
            for (int h = height; h <= height + 2; h++) {
                canvas.setIfEmpty(x + 2, floorY + h, z, lattice);
            }
        }
    }
}
