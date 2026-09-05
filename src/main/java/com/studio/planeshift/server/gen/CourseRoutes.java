package com.studio.planeshift.server.gen;

import com.studio.planeshift.common.registry.ModBlocks;
import com.studio.planeshift.common.registry.ModItems;
import java.util.random.RandomGenerator;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Builds a high road above the low road.
 *
 * <p>Every generated course has been a single ribbon: one route, taken by everyone, with the only
 * variation being how well you execute it. Real Mario levels are layered — there is a safe way
 * through and a faster, richer, more dangerous way over the top, and choosing between them is most
 * of what makes a level replayable. This pass adds the second one.
 *
 * <p>It could not have existed before semisolid platforms. An upper deck built from ordinary blocks
 * is also a ceiling over the lower route, so adding one would have made the safe path worse; the
 * whole idea depends on a platform you can jump up through from anywhere underneath it.
 *
 * <p><b>The low road is never touched.</b> This only ever writes above the existing floor, and only
 * semisolids, which {@code CourseReachability} treats as floor-from-above and invisible-to-the-body.
 * So the proof that the course is completable is entirely unaffected — the high road is a bonus the
 * solver does not need to know about, and cannot be a route the player is forced onto.
 */
public final class CourseRoutes {

    /** How high the upper deck sits above the floor it shadows. */
    private static final int DECK_HEIGHT = 5;

    /** Shortest stretch of level ground worth building over. */
    private static final int MIN_RUN = 26;

    /** Length of each span of deck, before a deliberate gap. */
    private static final int SPAN = 7;

    /** Gap between spans. Wide enough to be a jump, narrow enough to be an obvious one. */
    private static final int GAP = 3;

    private CourseRoutes() {
    }

    /**
     * Finds flat stretches and roofs them with an optional route.
     *
     * @param floorAt the per-column design floor recorded during composition
     * @param margin  offset applied to index {@code floorAt}
     */
    public static void build(CourseCanvas canvas, GenContext ctx, int[] floorAt,
                             int margin, int from, int to) {
        RandomGenerator random = ctx.random();
        int runStart = from;
        int built = 0;

        for (int x = from; x <= to; x++) {
            boolean level = x < to && sameFloor(floorAt, margin, x, runStart);
            if (level) {
                continue;
            }
            int length = x - runStart;
            // At most two per course. A level where every stretch has an attic stops having an
            // attic and just has two floors, and the choice disappears again.
            if (length >= MIN_RUN && built < 2 && random.nextInt(3) != 0) {
                deck(canvas, ctx, random, runStart + 2, length - 4, floorAt[runStart + margin]);
                built++;
            }
            runStart = x;
        }
    }

    private static boolean sameFloor(int[] floorAt, int margin, int x, int runStart) {
        int a = x + margin;
        int b = runStart + margin;
        if (a < 0 || a >= floorAt.length || b < 0 || b >= floorAt.length) {
            return false;
        }
        return floorAt[a] == floorAt[b];
    }

    /** Lays one upper route: a ramp on, spans with gaps, and a reward for staying up there. */
    private static void deck(CourseCanvas canvas, GenContext ctx, RandomGenerator random,
                             int from, int length, int floorY) {
        BlockState semi = ModBlocks.SEMISOLID_PLATFORM.get().defaultBlockState();
        int deckY = floorY + DECK_HEIGHT;

        // The way up. Three steps, at the near end, so the route is enterable rather than a thing
        // the player can see and not reach — the commonest way an optional route goes unused.
        for (int i = 0; i < 3; i++) {
            canvas.setIfEmpty(from + i, floorY + 2 + i, 0, semi);
        }

        int x = from + 3;
        boolean rewarded = false;
        while (x < from + length) {
            int span = Math.min(SPAN, from + length - x);
            for (int i = 0; i < span; i++) {
                for (int z = -ctx.halfWidth(); z <= ctx.halfWidth(); z++) {
                    canvas.setIfEmpty(x + i, deckY, z, semi);
                }
                // Coins along the deck: the reason to be up here at all.
                canvas.item(ModItems.COIN.get(), x + i + 0.5D, deckY + 1.5D, 0.5D);
            }
            // One real prize per deck, in the middle span rather than at the end, so the player has
            // to commit to the route before they can see whether it was worth it.
            if (!rewarded && x > from + length / 3) {
                canvas.set(x + span / 2, deckY + 3, 0,
                        ModBlocks.QUESTION_BLOCK.get().defaultBlockState());
                rewarded = true;
            }
            x += span + GAP;
        }
    }
}
