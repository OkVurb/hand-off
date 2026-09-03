package com.studio.planeshift.server.gen;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import com.studio.planeshift.common.registry.ModBlocks;
import java.util.Set;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Proves a course can actually be walked from the spawn to the flag.
 *
 * <p>Everything before this validated proxies: "no pit wider than seven blocks". That is a
 * necessary condition and nowhere near a sufficient one. It says nothing about a platform placed
 * five blocks above its approach, a ledge with a ceiling too low to jump under, or a staircase
 * whose last step is one block too tall. Courses shipped with all three.
 *
 * <p>This searches the finished {@link CourseCanvas} directly. It builds the set of positions a
 * player can stand in, connects them with the moves a player can actually make, and asks whether
 * the flag is in the same connected component as the spawn. If it is not, the failure comes back
 * with the furthest point reached, which is almost always exactly where the level is broken.
 *
 * <h2>Why the model is deliberately pessimistic</h2>
 *
 * The real player, with the course jump boost, clears about six blocks. This validates against
 * four. That gap is intentional: a course proven clearable at four blocks is <em>comfortable</em>
 * at six, and comfort is the thing being designed for. Validating against the true maximum would
 * certify courses that are technically possible and horrible to play — every jump a pixel-perfect
 * one. A generator that produces only barely-possible levels has failed even though every level
 * passes.
 */
public final class CourseReachability {

    /** Height the player is assumed to gain from a standing jump. Real value is higher. */
    public static final int MAX_RISE = 4;

    /**
     * Horizontal reach for a given rise, indexed by rise (0 = flat or downward).
     *
     * <p>Reach shrinks as the jump climbs, which is the shape of a real arc: all the horizontal
     * distance is bought with vertical distance. The numbers are a conservative reading of the
     * arc at {@code courseJumpBoost = 1.3} and {@code courseRunBoost = 0.9}.
     */
    private static final int[] REACH_BY_RISE = {6, 5, 5, 4, 3};

    /** How far the player may drift horizontally while falling. Falls are cheap; drift is not. */
    public static final int FALL_REACH = 6;

    /** Headroom a standing player needs. */
    private static final int PLAYER_HEIGHT = 2;

    private final CourseCanvas canvas;
    private final int laneZ;
    private final int minY;
    private final int maxY;
    private final int maxX;

    public CourseReachability(CourseCanvas canvas, int laneZ) {
        this.canvas = canvas;
        this.laneZ = laneZ;
        this.minY = canvas.minY() - 2;
        this.maxY = canvas.maxY() + 4;
        this.maxX = canvas.maxX() + 4;
    }

    /** The outcome of a search, with enough detail to fix a failure without rerunning it. */
    public record Result(boolean reachable, int furthestX, int standCount, int reachedCount) {
        public String describe(int targetX) {
            if (reachable) {
                return "reachable (" + reachedCount + " of " + standCount + " standing positions)";
            }
            return "UNREACHABLE: got to x=" + furthestX + " of " + targetX
                    + " (" + reachedCount + " of " + standCount + " standing positions reached)";
        }
    }

    /**
     * Blocks that do not stop the player.
     *
     * <p>Two kinds. Some are physically non-solid — the flagpole is a thin pole, vines are
     * climbable, the loop trigger is an invisible marker. Others are solid but removable: a brick
     * is always breakable from below, so a brick wall is a door with an extra step, not a dead end.
     * Treating either as rock made the solver report perfectly good courses as impossible, which
     * would have been the worst possible outcome — a validator that cries wolf gets switched off.
     */
    private static final Set<Block> PASSABLE = Set.of(
            ModBlocks.FLAG_POLE.get(),
            ModBlocks.BRICK_BLOCK.get(),
            ModBlocks.SECRET_VINE.get(),
            ModBlocks.COURSE_VINE.get(),
            ModBlocks.LOOP_TRIGGER.get(),
            ModBlocks.COIN_RING_BLOCK.get(),
            // The axe is touched, not climbed: it sits in the player's path on purpose and must
            // not read as a wall standing between them and the end of the bridge.
            ModBlocks.AXE_BLOCK.get());

    /**
     * Solid blocks the solver refuses to stand on.
     *
     * <p>These hold weight, so without this set the flood fill happily walks a player across a
     * carpet of Munchers and declares the course completable. It is completable, in the sense that
     * a course you cross by absorbing damage is completable — which is not what the proof is
     * supposed to be asserting. A hazard has to be an obstacle to the solver or the guarantee is
     * about geometry only, and geometry was never the interesting part.
     *
     * <p>They stay <em>solid</em> rather than joining {@link #PASSABLE}: a Muncher does block a
     * jump arc, so pretending the player passes through it would trade one wrong answer for
     * another.
     */
    private static final Set<Block> HAZARD = Set.of(
            ModBlocks.MUNCHER.get(),
            ModBlocks.SPIKE_BLOCK.get());

    private boolean solid(int x, int y) {
        BlockState state = canvas.get(x, y, laneZ);
        return state != null && !PASSABLE.contains(state.getBlock());
    }

    /**
     * Whether a player can stand at this cell: solid floor underneath, and enough clear space for
     * the player's own body.
     */
    public boolean isStand(int x, int y) {
        if (!solid(x, y - 1)) {
            return false;
        }
        BlockState floor = canvas.get(x, y - 1, laneZ);
        if (floor != null && HAZARD.contains(floor.getBlock())) {
            return false;
        }
        for (int h = 0; h < PLAYER_HEIGHT; h++) {
            if (solid(x, y + h)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Whether the player's body can pass through a column at a given foot height — used to check
     * the arc of a jump does not run into a ceiling.
     */
    private boolean passable(int x, int y) {
        for (int h = 0; h < PLAYER_HEIGHT; h++) {
            if (solid(x, y + h)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Whether a jump or fall from one stand to another is clear of obstructions.
     *
     * <p>Approximates the arc as: rise to the peak immediately, travel across at the peak, then
     * drop. That is pessimistic in the right direction — a real arc is rounded and passes lower
     * near its ends, so anything this accepts, the real arc also clears.
     */
    private boolean pathClear(int fromX, int fromY, int toX, int toY) {
        int peak = Math.max(fromY, toY);
        int step = toX > fromX ? 1 : -1;

        // Leaving the take-off column: the player must be able to rise to the peak in place.
        for (int y = fromY; y <= peak; y++) {
            if (!passable(fromX, y)) {
                return false;
            }
        }
        // Crossing.
        for (int x = fromX + step; x != toX; x += step) {
            if (!passable(x, peak)) {
                return false;
            }
        }
        // Dropping into the landing column.
        for (int y = peak; y >= toY; y--) {
            if (!passable(toX, y)) {
                return false;
            }
        }
        return true;
    }

    /** Horizontal reach available for a given vertical change. */
    private static int reachFor(int rise) {
        if (rise <= 0) {
            return REACH_BY_RISE[0];
        }
        if (rise > MAX_RISE) {
            return -1;
        }
        return REACH_BY_RISE[rise];
    }

    /**
     * Searches from a starting position and reports whether {@code targetX} can be reached.
     *
     * <p>Breadth-first over standing positions. The frontier is small — a course is a corridor,
     * not an open world — so this stays fast enough to run across thousands of generated courses
     * in a unit test.
     */
    public Result search(int startX, int startY, int targetX) {
        Set<Long> stands = new HashSet<>();
        for (int x = -4; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                if (isStand(x, y)) {
                    stands.add(pack(x, y));
                }
            }
        }
        // Surfaces that only exist while something is moving through them. The player stands on
        // top of the platform, so the standing position is one above the declared sweep height.
        for (CourseCanvas.MovingSurface surface : canvas.movingSurfaces()) {
            for (int x = surface.fromX(); x <= surface.toX(); x++) {
                if (passable(x, surface.y() + 1)) {
                    stands.add(pack(x, surface.y() + 1));
                }
            }
        }

        long start = pack(startX, startY);
        if (!stands.contains(start)) {
            // The spawn itself is not standable; drop to the first stand beneath it, which is what
            // the player does on arrival anyway.
            for (int y = startY; y >= minY; y--) {
                if (stands.contains(pack(startX, y))) {
                    start = pack(startX, y);
                    break;
                }
            }
        }

        Set<Long> seen = new HashSet<>();
        Deque<Long> queue = new ArrayDeque<>();
        seen.add(start);
        queue.add(start);
        int furthest = unpackX(start);
        boolean hit = false;

        while (!queue.isEmpty()) {
            long current = queue.poll();
            int cx = unpackX(current);
            int cy = unpackY(current);
            if (cx > furthest) {
                furthest = cx;
            }
            if (cx >= targetX) {
                hit = true;
            }

            for (int dir = -1; dir <= 1; dir += 2) {
                // Jumps and level moves, out to the reach the arc allows for that climb.
                for (int rise = MAX_RISE; rise >= -40; rise--) {
                    int reach = rise >= 0 ? reachFor(rise) : FALL_REACH;
                    if (reach < 0) {
                        continue;
                    }
                    for (int dx = 1; dx <= reach; dx++) {
                        int nx = cx + dir * dx;
                        int ny = cy + rise;
                        long next = pack(nx, ny);
                        if (!stands.contains(next) || seen.contains(next)) {
                            continue;
                        }
                        if (!pathClear(cx, cy, nx, ny)) {
                            continue;
                        }
                        seen.add(next);
                        queue.add(next);
                    }
                }
                // Stepping onto an adjacent block, including the one-block auto-step.
                for (int rise = -1; rise <= 1; rise++) {
                    long next = pack(cx + dir, cy + rise);
                    if (stands.contains(next) && !seen.contains(next)
                            && pathClear(cx, cy, cx + dir, cy + rise)) {
                        seen.add(next);
                        queue.add(next);
                    }
                }
            }
        }

        return new Result(hit, furthest, stands.size(), seen.size());
    }

    private static long pack(int x, int y) {
        return ((long) (x + 512) << 20) | (y + 512);
    }

    private static int unpackX(long k) {
        return (int) (k >> 20) - 512;
    }

    private static int unpackY(long k) {
        return (int) (k & 0xFFFFF) - 512;
    }
}
