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
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

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

    /**
     * Steps in the finish staircase. Seven puts the top step at floorY+7, one below the top of the
     * eight-block pole, so the top band is reachable at the peak of a jump and not by standing.
     */
    private static final int STAIR_STEPS = 7;

    /**
     * Columns between the top step and the pole. Wide enough that the jump has to be committed to,
     * narrow enough that it is clearly the jump the staircase was asking for.
     */
    private static final int STAIR_GAP = 3;

    /** Flat ground at the spawn, before anything is asked. */
    private static final int SPAWN_RUN = 10;
    /** Flat ground before the flag, so the finish is never a blind jump. */
    private static final int FINISH_RUN = 12;

    /**
     * How far below the floor a first-appearance safety net sits.
     *
     * <p>Three blocks: deep enough that falling in is unmistakably a mistake, shallow enough that
     * {@code CourseReachability.MAX_RISE} (4) can climb back out. The player pays for the error
     * with their run-up, which is the correct price for getting a mechanic wrong the first time
     * they have ever seen it.
     */
    static final int INTRO_NET_DROP = 3;

    /** Slack on each end of the per-column floor map, since content starts a little before x=0. */
    private static final int FLOOR_MAP_MARGIN = 16;

    /** How far above the design floor a roaming enemy may be placed, in blocks. */
    private static final int ROAM_MAX_CLIMB = 6;

    /** Coins in the arc that leads to a secret. Enough to read as a trail, not as a payout. */
    private static final int SIGNPOST_COINS = 4;

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
     * @param lesson       the mechanic this course was built around, introduced early and paid
     *                     off late — see {@link CourseLesson}
     */
    public record Composition(CourseCanvas canvas, List<String> segmentIds, int spawnY,
                              int checkpointX, int flagX, Segment.Tag lesson) {
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

        // The floor height each column was *designed* around.
        //
        // Needed by the roaming pass, and it has to be recorded here because it cannot be
        // recovered afterwards. Asking the finished canvas "what is the lowest solid block in this
        // column" finds the bottom of a pit as readily as the floor, and the first version of the
        // roaming pass did exactly that — it cheerfully placed enemies three blocks down inside
        // pits, where the player never sees them and they can never be part of the level. The
        // composer knows the answer while it is placing segments; nothing else ever does.
        int[] floorAt = new int[length + FLOOR_MAP_MARGIN * 2 + 8];

        List<Placed> placed = new ArrayList<>();
        List<String> ids = new ArrayList<>();
        // What this course is about. See CourseLesson: the composer implemented introduce, develop
        // and rest but never *conclude*, so a course was a well-paced sequence of unrelated ideas
        // that stopped when it ran out of room. One mechanic, set up early and paid off late, is
        // the whole difference between a level and a corridor.
        Segment.Tag lesson = CourseLesson.pickLesson(theme, ctx.random());
        CourseLesson.ThemeRules themeRules = CourseLesson.rules(theme);
        boolean lessonIntroduced = false;
        boolean lessonConcluded = false;

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
            // The set piece waits for a breather too.
            //
            // It was chosen before the breather rule ran, so a castle bridge could land directly
            // on top of a hard segment - the one arrangement the rule exists to prevent, taken by
            // the one segment most likely to kill you. Latent until ON_OFF_CORRIDOR made difficulty
            // 3 common enough to expose it.
            if (!setPieceUsed && lastDifficulty < 3 && progress > 0.62D && progress < 0.82D) {
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

            // The conclude step, forced rather than hoped for. Once the course is into its last
            // third, the lesson gets one guaranteed hard appearance — that payoff is the entire
            // reason the lesson exists, and leaving it to weighted chance meant courses that
            // taught something and then simply ended.
            // A breather is forced after anything demanding.
            if (chosen == null && lastDifficulty >= 3) {
                chosen = SegmentLibrary.BREATHER;
            }

            // The two ends of the four-step structure, forced rather than hoped for — but placed
            // *after* the breather rule and subject to the same teaching rule as everything else.
            // An earlier version put these first and quietly punched a hole through both: forced
            // segments skipped the mandatory breather after a hard segment, and introduced
            // mechanics the player had never been shown. A guarantee that suspends the other
            // guarantees is not a guarantee, it is a special case.
            if (chosen == null && !lessonIntroduced && progress > 0.30D) {
                Segment intro = CourseLesson.carrying(lesson, remaining, Math.max(allowed, 1), false);
                if (intro != null && teachable(intro, taught)) {
                    chosen = intro;
                }
            }
            if (chosen == null && !lessonConcluded && progress > 0.72D && lessonIntroduced) {
                Segment payoff = CourseLesson.carrying(lesson, remaining, Math.max(allowed, 2), true);
                if (payoff != null && teachable(payoff, taught)) {
                    chosen = payoff;
                    lessonConcluded = true;
                }
            }

            if (chosen == null) {
                chosen = pick(catalogue, ctx, allowed, remaining, taught, recent, floorY,
                        lesson, themeRules, CourseLesson.phase(progress), lessonIntroduced);
            }
            if (chosen == null) {
                // Nothing fits the remaining space; fill it with ground rather than leaving a hole.
                for (int i = 0; i < remaining; i++) {
                    ctx.ground(canvas, cursor + i, floorY);
                }
                recordFloor(floorAt, cursor, remaining, floorY, length);
                cursor = contentEnd;
                break;
            }

            Segment.SegmentSpec s = chosen.spec();

            // Computed before taught is updated below: this asks whether the player has met this
            // mechanic before, and after taught.addAll the answer is always no.
            boolean introducesGaps = s.tags().contains(Segment.Tag.GAP)
                    && !taught.contains(Segment.Tag.GAP);

            chosen.build(canvas, cursor, floorY, ctx);
            if (introducesGaps) {
                netIntroduction(canvas, ctx, cursor, s.width(), floorY);
                canvas.marker("intro_net", cursor, floorY, 0);
            }
            recordFloor(floorAt, cursor, s.width(), floorY, length);
            placed.add(new Placed(chosen, cursor, floorY));
            ids.add(s.id());

            if (s.tags().contains(lesson)) {
                lessonIntroduced = true;
            }
            if (s.tags().contains(Segment.Tag.SECRET)) {
                signpostSecret(canvas, ctx, cursor, floorY);
            }

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
        recordFloor(floorAt, cursor, length + 7 - cursor, floorY, length);
        int flagX = length;
        finishStaircase(canvas, ctx, flagX, floorY);
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
        populate(canvas, ctx, floorAt, SPAWN_RUN, contentEnd, length);
        // The high road, before scenery: it needs the floor map and it writes real geometry, so it
        // belongs with the level rather than with the dressing.
        CourseRoutes.build(canvas, ctx, floorAt, FLOOR_MAP_MARGIN, SPAWN_RUN, contentEnd);
        // Scenery last, so it can see the finished floor and fill in behind everything else.
        CourseDecorator.decorate(canvas, ctx, floorAt, FLOOR_MAP_MARGIN, 0, length + 6);

        // spawnY is a standing position, which is one above the surface block: ground(x, 0)
        // fills y=0 with solid, so the player's feet are at y=1. Reporting the surface height
        // here instead was an off-by-one that made every reachability search start inside rock.
        return new Composition(canvas, ids, 1, checkpointX, flagX, lesson);
    }

    /**
     * The staircase before the pole.
     *
     * <p>Fixes a mechanic that existed but could never fire. {@code FlagPoleBlock} pays by grab
     * height across eight bands and gives a 1-Up for the top one, and the finish apron was flat
     * ground — so from a standing run the player could only ever reach the bottom two or three.
     * Six of the eight bands, and the 1-Up, were unreachable in every course the generator had
     * ever produced. The scoring was not wrong; there was simply nothing to jump off.
     *
     * <p>Built as the solid triangle every 2D Mario level ends on, because the shape is the
     * instruction: a player who has never been told the pole scores by height still climbs a
     * staircase that points at one and jumps at the top of it.
     *
     * <p>The gap between the top step and the pole is deliberate and is the entire skill in it.
     * Standing on the top step is not enough to touch the top of the pole — the player has to
     * cross {@code STAIR_GAP} columns while at the peak of a jump. Mistiming it grabs lower down
     * and still completes the course, which is what makes it a choice rather than a toll.
     *
     * <p>Writes only solid ground on top of the existing flat apron, so it adds no gap, no hazard
     * and no ceiling: {@code CourseReachability}'s proof that the flag is reachable is unaffected,
     * and a player who ignores the staircase entirely can still walk to the pole along the floor.
     */
    private static void finishStaircase(CourseCanvas canvas, GenContext ctx, int flagX, int floorY) {
        int from = flagX - STAIR_GAP - STAIR_STEPS;
        for (int step = 0; step < STAIR_STEPS; step++) {
            // Solid to the ground rather than a floating ledge per step. A staircase you can walk
            // under is a corridor with steps above it, and the player reads the two differently.
            for (int y = floorY + 1; y <= floorY + step + 1; y++) {
                canvas.setLane(from + step, y, ctx.palette().surface(), ctx.halfWidth());
            }
        }
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
                                Set<Segment.Tag> taught, Set<Segment.Tag> recent, int floorY,
                                Segment.Tag lesson, CourseLesson.ThemeRules themeRules,
                                int phase, boolean lessonIntroduced) {
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
            // Discourage repeating what was just played — but not equally for every tag.
            //
            // A gap puzzle immediately after a gap puzzle is the level running out of ideas. Two
            // stretches with enemies in a row is just a level with enemies in it, which is the
            // normal state of a Mario course. Penalising ENEMY as hard as everything else is a
            // large part of why courses felt so empty: only seven of the catalogue's segments
            // carry enemies at all, and the repeat rule then pushed those seven apart.
            for (Segment.Tag tag : s.tags()) {
                if (recent.contains(tag)) {
                    weight -= tag == Segment.Tag.ENEMY ? 2 : 7;
                }
                if (!taught.contains(tag)) {
                    weight += 5;
                }
            }
            // The course's own lesson, and what this theme is for. Both are deltas so that every
            // rule steering the choice stays legible together rather than being spread around.
            weight += CourseLesson.weightFor(s, lesson, themeRules, phase, lessonIntroduced);

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

    /**
     * Scatters roaming enemies across the finished course.
     *
     * <p>Segments own the enemies that are part of a <em>designed</em> encounter — a patrol on a
     * ledge between two pits, a Hammer Bro on a perch — and those stay exactly as they are. This
     * pass fills in the space between them.
     *
     * <p>It exists because tagging enemies onto segments alone produces a course that is empty
     * and then briefly dangerous and then empty again. Only seven of the catalogue's segments
     * carry enemies, so across a 720-block course a player crosses long stretches with nothing to
     * do. Real Mario levels are not built that way: enemies are the ambient texture, and the
     * designed encounters are the moments that stand out <em>against</em> that texture. Without
     * something in the background there is no foreground.
     *
     * <p>Runs on the finished canvas rather than inside the segments for two reasons. It can see
     * the actual ground, including ground a segment raised or lowered, so an enemy is never
     * placed inside rock or floating over a pit. And it cannot disturb a segment's own design,
     * because it only ever adds to flat, empty, unclaimed ground.
     */
    private static void populate(CourseCanvas canvas, GenContext ctx, int[] floorAt,
                                 int from, int to, int length) {
        List<EntityType<?>> roster = SegmentLibrary.cast(ctx.theme());
        if (roster.isEmpty()) {
            return;
        }

        // Spacing shrinks with difficulty: an early course breathes, a late one crowds.
        int spacing = Math.clamp(16 - 2 * ctx.difficulty(), 7, 16);
        // ...and is capped against the course's own length, so a short course is not simply a long
        // course with most of it cut off. Density is what the player feels; a fixed stride turns a
        // 96-block course into three enemies and a walk.
        spacing = Math.min(spacing, Math.max(6, (to - from) / 8));

        // Not right at the start — the opening seconds are for orienting, and an enemy standing in
        // the spawn apron is a death the player had no chance to read.
        int cursor = from + 6;
        int index = ctx.random().nextInt(roster.size());

        while (cursor < to - 6) {
            // Jitter, so the course does not tick like a metronome. Each slot gets a few tries,
            // because a single sample lands on a pit or a wall often enough to leave whole
            // stretches empty, and an empty stretch is exactly what this pass exists to prevent.
            for (int attempt = 0; attempt < 4; attempt++) {
                int x = cursor + ctx.random().nextInt(Math.max(1, spacing - 1));
                if (x >= to - 6) {
                    break;
                }
                int y = standingY(canvas, floorAt, x, length);
                if (y == Integer.MIN_VALUE || occupied(canvas, x)) {
                    continue;
                }
                // Facing back down the course, so the player meets it head-on rather than
                // catching up with its back.
                canvas.spawn(roster.get(index % roster.size()),
                        x + 0.5D, y, 0.5D, 90.0F, SegmentLibrary.GENERATED_TAG);
                index++;
                break;
            }
            cursor += spacing;
        }
    }

    /** Records the floor a stretch of course was designed around. */
    private static void recordFloor(int[] floorAt, int from, int width, int floorY, int length) {
        for (int x = from; x < from + width; x++) {
            int slot = x + FLOOR_MAP_MARGIN;
            if (slot >= 0 && slot < floorAt.length) {
                floorAt[slot] = floorY;
            }
        }
    }

    /**
     * A standing position in column {@code x} at or just above the designed floor, or
     * {@link Integer#MIN_VALUE} if the column has none.
     *
     * <p>Searches upward from the design floor rather than from the bottom of the world, which is
     * the whole reason {@code floorAt} exists. It allows a few blocks of climb so an enemy can sit
     * on a low platform the segment built, but not so many that it ends up on a roof.
     *
     * <p>Requires floor under at least one neighbouring column too. An enemy dropped onto a single
     * block walks straight off it — that is the same failure the air-drop GameTest caught — and a
     * one-second cameo is not an obstacle.
     */
    private static int standingY(CourseCanvas canvas, int[] floorAt, int x, int length) {
        int slot = x + FLOOR_MAP_MARGIN;
        if (slot < 0 || slot >= floorAt.length) {
            return Integer.MIN_VALUE;
        }
        int base = floorAt[slot];
        for (int y = base + 1; y <= base + ROAM_MAX_CLIMB; y++) {
            if (!solid(canvas, x, y - 1)) {
                continue;
            }
            if (!solid(canvas, x - 1, y - 1) && !solid(canvas, x + 1, y - 1)) {
                continue;
            }
            if (canvas.isEmpty(x, y, 0) && canvas.isEmpty(x, y + 1, 0)) {
                return y;
            }
        }
        return Integer.MIN_VALUE;
    }

    /** Solid, and not something an enemy standing on it would die on or be carried off by. */
    private static boolean solid(CourseCanvas canvas, int x, int y) {
        BlockState state = canvas.get(x, y, 0);
        return state != null && !ROAM_EXCLUDED.contains(state.getBlock());
    }

    /** Whether a segment already put something within a few blocks of this column. */
    private static boolean occupied(CourseCanvas canvas, int x) {
        for (CourseCanvas.EntitySpawn spawn : canvas.entities()) {
            if (Math.abs(spawn.x() - x) < 4.0D) {
                return true;
            }
        }
        return false;
    }

    /**
     * Blocks a roaming enemy must not be dropped onto.
     *
     * <p>Hazards, because an enemy standing in a Muncher is a corpse rather than an obstacle, and
     * moving or vanishing surfaces, because an enemy placed on one is gone by the time the player
     * arrives. Segments may still place enemies on any of these deliberately; this is only about
     * what the automatic pass is allowed to guess at.
     */
    private static final Set<Block> ROAM_EXCLUDED = Set.of(
            ModBlocks.MUNCHER.get(),
            ModBlocks.SPIKE_BLOCK.get(),
            ModBlocks.DONUT_BLOCK.get(),
            ModBlocks.CONVEYOR_BELT.get(),
            ModBlocks.TRAMPOLINE.get(),
            ModBlocks.SPRING_PAD.get(),
            ModBlocks.FLAG_POLE.get(),
            ModBlocks.COURSE_ICE_BLOCK.get());


    /**
     * Floors the pits in the segment that first teaches the player about pits.
     *
     * <p>The first time a course shows a mechanic is where the player is allowed to be wrong. A
     * gap they have never seen before should cost them their momentum, not their life - they have
     * to understand the rule before failing it can mean anything, and a course whose opening lesson
     * is a death teaches only that the course is unfair.
     *
     * <p>Written with {@link CourseCanvas#setIfEmpty}, so it can never overwrite anything the
     * segment itself placed - the net fills the hole and leaves the design alone.
     *
     * <p>It is also safe against {@link CourseReachability} for a reason worth stating rather than
     * assuming. The solver's proof is one-sided: it certifies that a route <em>exists</em> under a
     * deliberately pessimistic jump arc. This only ever adds standable floor three blocks below an
     * existing surface, which cannot remove a route or break an arc that was already clear, so any
     * course that passed before still passes. That is the argument - not "the solver is
     * conservative so it will be fine", which is the shape of reasoning review R13 was written
     * about.
     */
    private static void netIntroduction(CourseCanvas canvas, GenContext ctx,
                                        int fromX, int width, int floorY) {
        int y = floorY - INTRO_NET_DROP;
        for (int x = fromX; x < fromX + width; x++) {
            for (int z = -ctx.halfWidth(); z <= ctx.halfWidth(); z++) {
                canvas.setIfEmpty(x, y, z, ctx.palette().surface());
            }
        }
    }


    /**
     * Lays a short rising arc of coins in front of a secret.
     *
     * <p>Generated secrets had no tell. A hidden room the player never looks at is not a secret,
     * it is wasted geometry — and every segment in the catalogue tagged SECRET was, in practice,
     * unfindable unless the player happened to jump at the right blank wall.
     *
     * <p>Coins are the genre's standard answer and they are honest: they are a reward in
     * themselves, so following them is never a waste even when the player does not realise they
     * are being led. An arrow would be clearer and much worse — it tells the player there is a
     * secret instead of letting them find one.
     *
     * <p>Placed with {@code setIfEmpty} semantics via the item list, so it cannot disturb the
     * segment's own contents.
     */
    private static void signpostSecret(CourseCanvas canvas, GenContext ctx, int cursor, int floorY) {
        for (int i = 0; i < SIGNPOST_COINS; i++) {
            // Rising as it approaches, so the eye is led upward toward the entrance rather than
            // along the floor it was already walking down.
            canvas.item(ModItems.COIN.get(),
                    cursor - SIGNPOST_COINS + i + 0.5D, floorY + 2.0D + i * 0.6D, 0.5D);
        }
    }


    /**
     * Whether a segment may be placed given what the player has been shown.
     *
     * <p>The same rule {@code pick} applies inline: nothing demanding may be the first thing to
     * use a mechanic. Extracted so the forced lesson picks obey it too rather than each caller
     * re-deriving it — which is exactly how the forced picks came to skip it.
     */
    private static boolean teachable(Segment segment, Set<Segment.Tag> taught) {
        Segment.SegmentSpec spec = segment.spec();
        return spec.difficulty() < 3 || taught.containsAll(mechanics(spec));
    }

}
