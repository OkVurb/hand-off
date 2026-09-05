package com.studio.planeshift.server.gen;

import com.studio.planeshift.common.course.CourseTheme;
import java.util.List;
import java.util.Set;
import java.util.random.RandomGenerator;

/**
 * What a course is <em>about</em>, and what its theme is allowed to be about.
 *
 * <p>{@code CourseComposer} already implements three of the four steps Nintendo's designers
 * describe — introduce, develop, rest. It has never implemented <b>conclude</b>, and the gap shows
 * in exactly the way you would predict: a generated course is a well-paced sequence of unrelated
 * ideas that stops when it runs out of room. Nothing is set up early and paid off late, because
 * nothing is <em>about</em> anything.
 *
 * <p>A lesson fixes that for the cost of one field. Each course picks one mechanic at composition
 * time. That mechanic is guaranteed an easy appearance early, is preferred through the middle, and
 * is guaranteed a hard appearance in the last third. The player is taught something, made to
 * practise it, and then tested on it — which is the whole of the four-step structure and the
 * difference between a level and a corridor.
 *
 * <p>The second half of this class is per-theme rules. Themes were purely cosmetic: a ghost house
 * and a grassland drew from an identical segment pool and differed only in which blocks were
 * placed. That is why the themes felt like palettes. Giving each one a bias — and, more
 * importantly, a short list of things it will not do — is what makes a castle feel like a hazard
 * gauntlet and a ghost house feel like a navigation puzzle.
 */
public final class CourseLesson {

    /**
     * Per-theme composition rules.
     *
     * @param lessons  mechanics this theme may build a course around
     * @param favoured mechanics weighted up throughout, whether or not they are the lesson
     * @param avoided  mechanics this theme will not use unless nothing else fits
     */
    public record ThemeRules(List<Segment.Tag> lessons,
                             Set<Segment.Tag> favoured,
                             Set<Segment.Tag> avoided) {
    }

    private CourseLesson() {
    }

    /**
     * The rules for a theme.
     *
     * <p>Each set is a claim about what that kind of level is for, and the {@code avoided} list is
     * doing at least as much work as the other two. A ghost house that keeps throwing moving
     * platforms at you is not a ghost house, it is an athletic level with grey walls.
     */
    public static ThemeRules rules(CourseTheme theme) {
        return switch (theme) {
            // The teaching world. Gaps and enemies, the two things everything else builds on.
            case GRASS -> new ThemeRules(
                    List.of(Segment.Tag.GAP, Segment.Tag.ENEMY, Segment.Tag.BLOCKS),
                    Set.of(Segment.Tag.GAP, Segment.Tag.ENEMY, Segment.Tag.BLOCKS),
                    Set.of(Segment.Tag.OVERHEAD));
            // Wide open and unstable underfoot: the ground is the hazard.
            case DESERT -> new ThemeRules(
                    List.of(Segment.Tag.GAP, Segment.Tag.UNSTABLE, Segment.Tag.PIPE),
                    Set.of(Segment.Tag.GAP, Segment.Tag.UNSTABLE),
                    Set.of(Segment.Tag.SECRET));
            // Ice: everything slides, so the interesting problem is stopping, not jumping.
            case SNOW -> new ThemeRules(
                    List.of(Segment.Tag.UNSTABLE, Segment.Tag.MOVING, Segment.Tag.GAP),
                    Set.of(Segment.Tag.UNSTABLE, Segment.Tag.MOVING),
                    Set.of(Segment.Tag.SECRET));
            // A hazard gauntlet. Things overhead, things below, very little standing still.
            case LAVA -> new ThemeRules(
                    List.of(Segment.Tag.OVERHEAD, Segment.Tag.GAP, Segment.Tag.MOVING),
                    Set.of(Segment.Tag.OVERHEAD, Segment.Tag.GAP, Segment.Tag.MOVING),
                    Set.of(Segment.Tag.REST));
            // A navigation puzzle, not a platforming test. Secrets and doors, nothing athletic.
            case GHOST_HOUSE -> new ThemeRules(
                    List.of(Segment.Tag.SECRET, Segment.Tag.PIPE, Segment.Tag.BLOCKS),
                    Set.of(Segment.Tag.SECRET, Segment.Tag.PIPE, Segment.Tag.BLOCKS),
                    Set.of(Segment.Tag.MOVING, Segment.Tag.UNSTABLE));
            // Tight and vertical: ceilings matter, and so does climbing out.
            case UNDERGROUND -> new ThemeRules(
                    List.of(Segment.Tag.CLIMB, Segment.Tag.BLOCKS, Segment.Tag.OVERHEAD),
                    Set.of(Segment.Tag.CLIMB, Segment.Tag.BLOCKS, Segment.Tag.OVERHEAD),
                    Set.of(Segment.Tag.MOVING));
        };
    }

    /**
     * How many segments must carry a mechanic before a course can be built around it.
     *
     * <p>Three: one to introduce it, one to develop it, one to conclude on. A lesson with fewer
     * than that cannot be taught, and the first version of this class did not check — SNOW happily
     * chose MOVING, which two segments in the whole catalogue carry, and produced courses whose
     * stated subject appeared once or not at all.
     */
    private static final int MIN_LESSON_SEGMENTS = 3;

    /**
     * Picks the mechanic this course will be about, from the ones the catalogue can actually
     * support.
     *
     * <p>Falls back to the theme's best-represented option rather than throwing, because a
     * generator that refuses to produce a course is worse than one that produces a slightly
     * off-theme course, and the fallback still comes from the theme's own list.
     */
    public static Segment.Tag pickLesson(CourseTheme theme, RandomGenerator random) {
        List<Segment.Tag> options = rules(theme).lessons();
        List<Segment.Tag> teachable = new java.util.ArrayList<>();
        for (Segment.Tag tag : options) {
            if (count(tag) >= MIN_LESSON_SEGMENTS) {
                teachable.add(tag);
            }
        }
        if (teachable.isEmpty()) {
            Segment.Tag best = options.get(0);
            for (Segment.Tag tag : options) {
                if (count(tag) > count(best)) {
                    best = tag;
                }
            }
            return best;
        }
        return teachable.get(random.nextInt(teachable.size()));
    }

    /** How many segments in the catalogue carry this mechanic. */
    public static int count(Segment.Tag tag) {
        int n = 0;
        for (Segment segment : SegmentLibrary.all()) {
            if (segment.spec().tags().contains(tag)) {
                n++;
            }
        }
        return n;
    }

    /**
     * A segment carrying the lesson that fits the space left, or null.
     *
     * <p>Both ends of the structure are forced rather than hoped for, and they need opposite
     * choices from the same catalogue: the introduction wants the <em>gentlest</em> example, since
     * the first time a player meets a mechanic is where they are allowed to be wrong, and the
     * payoff wants the hardest. Weighted chance produced neither reliably — the first version left
     * both to {@code pick} and produced courses that named a lesson and never taught it.
     *
     * @param hardest true for the payoff, false for the introduction
     */
    public static Segment carrying(Segment.Tag lesson, int remaining, int allowed, boolean hardest) {
        Segment best = null;
        for (Segment segment : SegmentLibrary.all()) {
            Segment.SegmentSpec spec = segment.spec();
            if (!spec.tags().contains(lesson) || spec.width() > remaining
                    || spec.difficulty() > allowed) {
                continue;
            }
            if (best == null
                    || (hardest ? spec.difficulty() > best.spec().difficulty()
                                : spec.difficulty() < best.spec().difficulty())) {
                best = segment;
            }
        }
        return best;
    }

    /**
     * Weight adjustment for one candidate segment.
     *
     * <p>Returned as a delta rather than applied here so {@code CourseComposer.pick} keeps every
     * weighting rule visible in one place — the difficulty envelope, the repeat penalty and this
     * all have to be read together to understand why a segment was chosen.
     *
     * @param phase 0 = introduce, 1 = develop, 2 = conclude
     */
    public static int weightFor(Segment.SegmentSpec spec, Segment.Tag lesson,
                                ThemeRules rules, int phase, boolean lessonIntroduced) {
        int weight = 0;
        boolean carriesLesson = spec.tags().contains(lesson);

        if (carriesLesson) {
            weight += switch (phase) {
                // Early: strongly prefer an *easy* appearance. The introduction has to be somewhere
                // the player can fail safely, so a hard segment carrying the lesson is worse here
                // than one that does not carry it at all.
                case 0 -> spec.difficulty() <= 1 ? 22 : -6;
                case 1 -> 10;
                // Late: the payoff. Now the hard version is the one we want.
                default -> 8 + 7 * spec.difficulty();
            };
        } else if (phase == 0 && !lessonIntroduced) {
            // Nothing else should crowd out the introduction before it has happened.
            weight -= 5;
        }

        for (Segment.Tag tag : spec.tags()) {
            if (rules.favoured().contains(tag)) {
                weight += 6;
            }
            if (rules.avoided().contains(tag)) {
                weight -= 14;
            }
        }
        return weight;
    }

    /** Which of the three structural phases a position in the course falls in. */
    public static int phase(double progress) {
        if (progress < 0.25D) {
            return 0;
        }
        return progress < 0.70D ? 1 : 2;
    }
}
