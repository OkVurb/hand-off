package com.studio.planeshift.server.gen;

import com.studio.planeshift.common.block.FlagPoleBlock;
import com.studio.planeshift.common.course.CourseTheme;
import com.studio.planeshift.common.registry.ModBlocks;
import com.studio.planeshift.common.registry.ModItems;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.random.RandomGenerator;

/**
 * Arranges segments into a course.
 *
 * <p>This is where a level stops being a pile of obstacles and becomes something with a shape. The
 * composer follows the structure Nintendo's own designers describe: <em>introduce, develop, twist,
 * conclude</em>. A mechanic appears first somewhere safe, where failing costs nothing and the
 * player can watch it work. It comes back harder. Then it is combined with something else. Only
 * then does it appear in a form that can kill.
 *
 * <p>Three rules do most of the work:
 *
 * <ol>
 *   <li><b>A difficulty envelope</b> that rises across the course, dips just after the checkpoint,
 *       then climbs past its earlier peak. The dip matters: a checkpoint is a moment of relief, and
 *       following it immediately with the hardest thing in the level reads as punishment for having
 *       reached it.</li>
 *   <li><b>Teaching before testing.</b> A segment tagged with a mechanic cannot appear at
 *       difficulty 3 or above until a gentler segment using the same mechanic has already been
 *       placed. Otherwise the player meets donut blocks for the first time over a lava pit.</li>
 *   <li><b>Rest after strain.</b> Anything difficult is followed by a breather. Constant pressure
 *       is not hard, it is exhausting, and the contrast is what makes the hard parts feel hard.</li>
 * </ol>
 *
 * <p>Everything is driven by a seeded random, so a course regenerates identically for a retry.
 */
public final class CourseComposer {

    /** How far the floor may wander from the spawn height, so courses stay readable. */
    private static final int MAX_FLOOR_DRIFT = 12;
    private static final int MIN_FLOOR_DRIFT = -8;

    /** Star coins hidden per course. */
    private static final int STAR_COINS = 3;

    /** Flat ground at the spawn, before anything is asked. */
    private static final int SPAWN_RUN = 10;
    /** Flat ground before the flag, so the finish is never a blind jump. */
    private static final int FINISH_RUN = 12;

    private CourseComposer() {
    }

    /**
     * A finished course.
     *
     * @param canvas       every block, entity and item
     * @param segmentIds   what was placed, in order — invaluable when a course plays badly
     * @param spawnY       the floor height at the spawn
     * @param checkpointX  where the checkpoint beacon went
     * @param flagX        where the flagpole went
     */
    public record Composition(CourseCanvas canvas, List<String> segmentIds, int spawnY,
                              int checkpointX, int flagX) {
    }

    /** One placed segment, kept so star coins and the checkpoint can be sited afterwards. */
    private record Placed(Segment segment, int x, int y) {
    }

    public static Composition compose(CourseTheme theme, int length, int difficulty, long seed) {
        return compose(theme, length, difficulty, seed, GenContext.LANE_HALF_WIDTH);
    }

    /**
     * Composes a course at a given width.
     *
     * <p>The only difference between a 2.5D course and a 3D one is this number. 3D Mario levels are
     * linear ribbons with room to move, not open worlds, so widening the ribbon does not change
     * what any segment means — a gap is still a gap, a staircase is still a staircase. That is why
     * one segment library serves both modes rather than needing a second one, and it is the whole
     * reason the 2.5D/3D shift can be a mechanic instead of two separate games.
     */
    public static Composition compose(CourseTheme theme, int length, int difficulty, long seed,
                                      int halfWidth) {
        // java.util.Random, not RandomGeneratorFactory.
        //
        // RandomGeneratorFactory resolves algorithms through ServiceLoader, and ServiceLoader does
        // not initialise under FML's classloader: the factory's holder class fails with
        // NoClassDefFoundError the first time a course is composed, which surfaced as
        // "Failed to process payload: planeshift:course_select" and the player simply never being
        // teleported. Nothing here needs a better generator than a seeded LCG — the requirement is
        // that the same seed gives the same course, and Random satisfies that exactly.
        RandomGenerator random = new java.util.Random(seed);
        GenContext ctx = new GenContext(theme, difficulty, random, halfWidth);
        CourseCanvas canvas = new CourseCanvas();

        int floorY = 0;
        int cursor = -4;

        // Spawn apron. Always flat, always safe: the first seconds of a level are for orienting,
        // not for being tested.
        for (int i = cursor; i < SPAWN_RUN; i++) {
            ctx.ground(canvas, i, floorY);
        }
        cursor = SPAWN_RUN;

        List<Placed> placed = new ArrayList<>();
        List<String> ids = new ArrayList<>();
        Set<Segment.Tag> taught = EnumSet.noneOf(Segment.Tag.class);
        Set<Segment.Tag> recent = EnumSet.noneOf(Segment.Tag.class);
        boolean setPieceUsed = false;
        int lastDifficulty = 0;

        int contentEnd = length - FINISH_RUN;
        List<Segment> catalogue = SegmentLibrary.all();

        while (cursor < contentEnd) {
            int remaining = contentEnd - cursor;
            double progress = (double) (cursor - SPAWN_RUN) / Math.max(1, contentEnd - SPAWN_RUN);
            int allowed = allowedDifficulty(progress, difficulty);

            Segment chosen = null;

            // The set piece goes in the last third, once, when the theme has one.
            if (!setPieceUsed && progress > 0.62D && progress < 0.82D) {
                for (Segment candidate : SegmentLibrary.setPieces()) {
                    // A set piece is chosen directly rather than through pick(), so the teaching
                    // rule has to be applied here too. A castle bridge is a climax, not a place to
                    // meet a firebar for the first time.
                    if (!taught.containsAll(mechanics(candidate.spec()))) {
                        continue;
                    }
                    if (suitsTheme(candidate, theme) && candidate.spec().width() <= remaining) {
                        chosen = candidate;
                        setPieceUsed = true;
                        break;
                    }
                }
            }

            // A breather is forced after anything demanding.
            if (chosen == null && lastDifficulty >= 3) {
                chosen = SegmentLibrary.BREATHER;
            }

            if (chosen == null) {
                chosen = pick(catalogue, ctx, allowed, remaining, taught, recent, floorY);
            }
            if (chosen == null) {
                // Nothing fits the remaining space; fill it with ground rather than leaving a hole.
                for (int i = 0; i < remaining; i++) {
                    ctx.ground(canvas, cursor + i, floorY);
                }
                cursor = contentEnd;
                break;
            }

            Segment.SegmentSpec s = chosen.spec();
            chosen.build(canvas, cursor, floorY, ctx);
            placed.add(new Placed(chosen, cursor, floorY));
            ids.add(s.id());

            taught.addAll(s.tags());
            recent.clear();
            recent.addAll(s.tags());
            lastDifficulty = s.difficulty();

            cursor += s.width();
            floorY = Math.clamp(floorY + s.exitRise(), MIN_FLOOR_DRIFT, MAX_FLOOR_DRIFT);
        }

        // Finish apron and the flag.
        for (int i = cursor; i <= length + 6; i++) {
            ctx.ground(canvas, i, floorY);
        }
        int flagX = length;
        canvas.set(flagX, floorY + 1, 0, ModBlocks.FLAG_POLE.get().defaultBlockState()
                .setValue(FlagPoleBlock.PART, FlagPoleBlock.Part.BASE));
        for (int h = 2; h <= 7; h++) {
            canvas.set(flagX, floorY + h, 0, ModBlocks.FLAG_POLE.get().defaultBlockState()
                    .setValue(FlagPoleBlock.PART, FlagPoleBlock.Part.POLE));
        }
        canvas.set(flagX, floorY + 8, 0, ModBlocks.FLAG_POLE.get().defaultBlockState()
                .setValue(FlagPoleBlock.PART, FlagPoleBlock.Part.TOP));
        canvas.marker("flag", flagX, floorY + 1, 0);

        int checkpointX = placeCheckpoint(canvas, placed, length);
        placeStarCoins(canvas, placed, ctx);

        // spawnY is a standing position, which is one above the surface block: ground(x, 0)
        // fills y=0 with solid, so the player's feet are at y=1. Reporting the surface height
        // here instead was an off-by-one that made every reachability search start inside rock.
        return new Composition(canvas, ids, 1, checkpointX, flagX);
    }

    /**
     * The difficulty envelope.
     *
     * <p>Rises to about three quarters of the course's ceiling by the midpoint, drops sharply just
     * after it for the post-checkpoint breather, then climbs past the earlier peak for the run to
     * the flag. The shape is more important than the numbers: tension, release, greater tension.
     */
    private static int allowedDifficulty(double progress, int courseDifficulty) {
        int ceiling = Math.clamp(courseDifficulty + 1, 1, 4);
        double curve;
        if (progress < 0.5D) {
            curve = progress * 1.5D;
        } else if (progress < 0.62D) {
            curve = 0.25D;
        } else {
            curve = 0.55D + (progress - 0.62D) * 1.2D;
        }
        return Math.clamp((int) Math.round(curve * ceiling), 0, ceiling);
    }

    /** Set pieces only belong where the theme supports them. */
    private static boolean suitsTheme(Segment segment, CourseTheme theme) {
        if (segment == SegmentLibrary.CASTLE_BRIDGE) {
            return theme == CourseTheme.LAVA || theme == CourseTheme.UNDERGROUND;
        }
        return true;
    }

    /**
     * Chooses the next segment.
     *
     * <p>Weighted rather than uniform, and the weights encode the design rules: a segment at the
     * top of the currently allowed difficulty is preferred over one well below it, so the course
     * keeps pace with its own envelope; anything repeating the mechanic just used is heavily
     * penalised; and an untested mechanic gets a bonus so a course covers ground rather than
     * playing one idea eight times.
     */
    private static Segment pick(List<Segment> catalogue, GenContext ctx, int allowed, int remaining,
                                Set<Segment.Tag> taught, Set<Segment.Tag> recent, int floorY) {
        List<Segment> candidates = new ArrayList<>();
        List<Integer> weights = new ArrayList<>();
        int total = 0;

        for (Segment segment : catalogue) {
            Segment.SegmentSpec s = segment.spec();
            if (s.width() > remaining || s.difficulty() > allowed) {
                continue;
            }
            // Would this push the floor outside the readable band?
            int nextFloor = floorY + s.exitRise();
            if (nextFloor > MAX_FLOOR_DRIFT || nextFloor < MIN_FLOOR_DRIFT) {
                continue;
            }
            // Teaching rule: nothing demanding may use a mechanic the player has not met.
            if (s.difficulty() >= 3 && !taught.containsAll(mechanics(s))) {
                continue;
            }

            int weight = 10;
            // Prefer segments near the top of the allowed band, so the envelope is actually felt.
            weight += 6 * (s.difficulty() - Math.max(0, allowed - 1));
            // Discourage repeating what was just played.
            for (Segment.Tag tag : s.tags()) {
                if (recent.contains(tag)) {
                    weight -= 7;
                }
                if (!taught.contains(tag)) {
                    weight += 5;
                }
            }
            weight = Math.max(1, weight);

            candidates.add(segment);
            weights.add(weight);
            total += weight;
        }

        if (candidates.isEmpty()) {
            return null;
        }
        int roll = ctx.random().nextInt(total);
        for (int i = 0; i < candidates.size(); i++) {
            roll -= weights.get(i);
            if (roll < 0) {
                return candidates.get(i);
            }
        }
        return candidates.get(candidates.size() - 1);
    }

    /** The mechanics a segment tests, ignoring the ones that are never a threat. */
    private static Set<Segment.Tag> mechanics(Segment.SegmentSpec spec) {
        EnumSet<Segment.Tag> set = EnumSet.copyOf(spec.tags());
        set.remove(Segment.Tag.REST);
        set.remove(Segment.Tag.SECRET);
        set.remove(Segment.Tag.SETPIECE);
        return set;
    }

    /**
     * Puts the checkpoint on flat ground near the middle.
     *
     * <p>Sited at a segment boundary rather than at the arithmetic midpoint, because a boundary is
     * guaranteed to be walkable ground; the middle of a segment might be a pit or a moving
     * platform. A checkpoint the player respawns into mid-air is worse than none.
     */
    private static int placeCheckpoint(CourseCanvas canvas, List<Placed> placed, int length) {
        int target = length / 2;
        Placed best = null;
        for (Placed p : placed) {
            if (best == null || Math.abs(p.x() - target) < Math.abs(best.x() - target)) {
                if (p.segment().spec().has(Segment.Tag.REST)
                        || !p.segment().spec().has(Segment.Tag.GAP)) {
                    best = p;
                }
            }
        }
        if (best == null) {
            return target;
        }
        int cx = best.x() + 1;
        canvas.set(cx, best.y() + 1, 0, ModBlocks.CHECKPOINT_BEACON.get().defaultBlockState());
        canvas.marker("checkpoint", cx, best.y() + 1, 0);
        return cx;
    }

    /**
     * Places the three star coins.
     *
     * <p>Backlog item 67: the progress system has tracked star coins per course since it was
     * written, and generation never placed any, so the counter could only ever read zero.
     *
     * <p>They go on the three hardest segments in the course, above the action rather than beside
     * it — a star coin should cost a detour or a risk, not be picked up by walking forward. If the
     * course has fewer than three difficult segments, the remainder go on whatever is hardest,
     * which is the honest answer for an easy course.
     */
    private static void placeStarCoins(CourseCanvas canvas, List<Placed> placed, GenContext ctx) {
        List<Placed> ranked = new ArrayList<>(placed);
        ranked.removeIf(p -> p.segment().spec().has(Segment.Tag.REST));
        ranked.sort((a, b) -> Integer.compare(b.segment().spec().difficulty(),
                a.segment().spec().difficulty()));

        int count = Math.min(STAR_COINS, ranked.size());
        for (int i = 0; i < count; i++) {
            Placed p = ranked.get(i);
            int width = p.segment().spec().width();
            // Above the middle of the segment, high enough to need the segment's own mechanic.
            canvas.item(ModItems.STAR_COIN.get(),
                    p.x() + width / 2.0D, p.y() + 6.5D, 0.5D);
        }
        // A course with almost no content still owes the player three; put the rest over the
        // spawn apron rather than silently shipping a course that cannot be completed 3/3.
        for (int i = count; i < STAR_COINS; i++) {
            canvas.item(ModItems.STAR_COIN.get(), SPAWN_RUN + i * 3 + 0.5D, 5.5D, 0.5D);
        }
    }
}
