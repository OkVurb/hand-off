package com.studio.planeshift.server;

import com.studio.planeshift.common.course.CourseDefinition;
import com.studio.planeshift.common.course.CourseLayout;
import com.studio.planeshift.common.course.CourseTheme;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Pure, deterministic geometry plan used by {@link CourseStructureService}.
 *
 * <p>The pits used to be six hard-coded pairs offset by a per-theme constant, which had two
 * problems. Every course of a theme was laid out identically, and at some theme offsets two of
 * those pairs landed adjacent — a ghost-house course ended up with a fourteen-block continuous
 * hole that nothing can jump. Gaps are now either listed in the course JSON or derived from the
 * course length, and the derivation enforces the properties the old constants only happened to
 * satisfy: bounded width, a run of ground between consecutive pits, and solid ground under the
 * checkpoint.
 */
record CourseLayoutPlan(int length, int[][] gaps, Set<CourseLayout.Feature> features,
                        int setPieceCount, int seed) {

    /** Blocks of ground guaranteed between two pits, so they can never merge into one. */
    static final int MIN_GROUND_RUN = 4;
    /** Ground kept solid either side of the midpoint, where the checkpoint beacon stands. */
    private static final int CHECKPOINT_CLEARANCE = 2;
    /** Roughly one pit per this many blocks when the course does not say otherwise. */
    private static final int BLOCKS_PER_GAP = 19;
    /** Roughly one set piece per this many blocks when the course does not say otherwise. */
    private static final int BLOCKS_PER_SET_PIECE = 24;

    static CourseLayoutPlan forCourse(CourseDefinition course, String courseId, long worldSeed) {
        return build(course.theme(), course.length(), course.layout().orElse(CourseLayout.DEFAULT),
                seedOf(courseId, worldSeed));
    }

    /** Convenience for the theme defaults; the generator always goes through {@link #forCourse}. */
    static CourseLayoutPlan forTheme(CourseTheme theme, int length) {
        return build(theme, length, CourseLayout.DEFAULT, 0);
    }

    /**
     * A stable per-course seed.
     *
     * <p>Every course of a theme used to produce a byte-identical layout, because the derivation
     * read only the theme and the length. Fifty courses generated as five, repeated ten times
     * each.
     *
     * <p>Mixing the world seed in makes a new save a new set of courses, the way a new Minecraft
     * world is new terrain. Mixing the course id in keeps the fifty courses of one save distinct
     * from each other. And because it is a hash rather than a random draw, a given course in a
     * given save regenerates identically every time it loads — which retrying a course after a
     * death depends on completely.
     */
    static int seedOf(String courseId, long worldSeed) {
        long hash = worldSeed;
        if (courseId != null) {
            for (int i = 0; i < courseId.length(); i++) {
                hash = hash * 31L + courseId.charAt(i);
            }
        }
        // Fold the 64-bit value down and mix, so two ids one character apart do not produce two
        // seeds one apart — which would give neighbouring courses near-identical layouts.
        hash ^= hash >>> 32;
        hash *= 0x9E3779B97F4A7C15L;
        hash ^= hash >>> 29;
        return (int) (hash & 0x7FFFFFFF);
    }

    /** Overload for tests and for callers with no world, e.g. the theme-default plans. */
    static int seedOf(String courseId) {
        return seedOf(courseId, 0L);
    }

    static CourseLayoutPlan build(CourseTheme theme, int length, CourseLayout layout) {
        return build(theme, length, layout, 0);
    }

    static CourseLayoutPlan build(CourseTheme theme, int length, CourseLayout layout, int seed) {
        int[][] gaps = layout.gaps()
                .map(list -> explicitGaps(list, length, layout))
                .orElseGet(() -> derivedGaps(theme, length, layout, seed));
        int setPieces = layout.setPieceCount()
                .orElseGet(() -> Math.clamp(length / BLOCKS_PER_SET_PIECE, 3, 12));
        return new CourseLayoutPlan(length, gaps, layout.resolvedFeatures(theme), setPieces, seed);
    }

    /**
     * Explicit pits from JSON, still filtered through the safety rules. An author can say where
     * the holes go; they cannot place one under the flagpole or make one unjumpable, because a
     * course that cannot be finished is not a course.
     */
    private static int[][] explicitGaps(List<CourseLayout.Gap> declared, int length,
                                        CourseLayout layout) {
        List<int[]> accepted = new ArrayList<>();
        int spanStart = layout.safeStart() + 1;
        int spanEnd = length - layout.safeFinish() - 1;
        int mid = length / 2;
        for (CourseLayout.Gap gap : declared) {
            int from = Math.min(gap.from(), gap.to());
            int to = Math.max(gap.from(), gap.to());
            if (to - from + 1 > layout.maxGapWidth() || from < spanStart || to > spanEnd) {
                continue;
            }
            if (from <= mid + CHECKPOINT_CLEARANCE && to >= mid - CHECKPOINT_CLEARANCE) {
                continue;
            }
            if (!accepted.isEmpty()) {
                int[] previous = accepted.get(accepted.size() - 1);
                if (from - previous[1] <= MIN_GROUND_RUN) {
                    continue;
                }
            }
            accepted.add(new int[]{from, to});
        }
        return accepted.toArray(new int[0][]);
    }

    /**
     * Evenly spaced pits across the playable span.
     *
     * <p>Slot width comes out of the course length, so a longer course genuinely gets more pits
     * rather than the same six spread thinner. The theme still perturbs the widths so two themes
     * at the same length do not produce an identical silhouette, but it no longer moves the pits
     * bodily — that displacement was what let them collide.
     */
    private static int[][] derivedGaps(CourseTheme theme, int length, CourseLayout layout, int seed) {
        int spanStart = layout.safeStart() + 1;
        int spanEnd = length - layout.safeFinish() - 1;
        int span = spanEnd - spanStart + 1;
        if (span < MIN_GROUND_RUN * 2) {
            return new int[0][];
        }

        int count = layout.gapCount().orElseGet(() -> Math.clamp(span / BLOCKS_PER_GAP, 2, 10));
        if (count <= 0) {
            return new int[0][];
        }
        int slot = span / count;
        if (slot <= layout.maxGapWidth() + MIN_GROUND_RUN) {
            // Too many pits to fit while keeping ground between them; drop back to what fits.
            count = Math.max(1, span / (layout.maxGapWidth() + MIN_GROUND_RUN + 1));
            slot = span / count;
        }

        int widthSpread = Math.max(1, layout.maxGapWidth() - 3);
        // Theme still perturbs the widths; the seed makes two courses of the same theme and
        // length differ, which is the whole point of it existing.
        int shift = theme.ordinal() + seed;
        int mid = length / 2;

        List<int[]> accepted = new ArrayList<>();
        int previousEnd = spanStart - MIN_GROUND_RUN - 1;
        for (int i = 0; i < count; i++) {
            int width = Math.min(layout.maxGapWidth(), 4 + ((i + shift) % widthSpread));
            // Nudge each pit within its own slot rather than moving it into a neighbour's, so the
            // spacing guarantees below still hold no matter what the seed is.
            int jitter = slot <= width + MIN_GROUND_RUN * 2
                    ? 0
                    : ((seed / (i + 1)) % (slot - width - MIN_GROUND_RUN)) - (slot - width - MIN_GROUND_RUN) / 2;
            int centre = spanStart + slot * i + slot / 2 + jitter;
            int from = centre - width / 2;
            int to = from + width - 1;

            // Never straddle the checkpoint: slide the pit clear of it, in whichever direction
            // still leaves it inside the playable span.
            if (from <= mid + CHECKPOINT_CLEARANCE && to >= mid - CHECKPOINT_CLEARANCE) {
                int shifted = mid - CHECKPOINT_CLEARANCE - width;
                if (shifted - MIN_GROUND_RUN > previousEnd) {
                    from = shifted;
                } else {
                    from = mid + CHECKPOINT_CLEARANCE + 1;
                }
                to = from + width - 1;
            }

            if (from - previousEnd <= MIN_GROUND_RUN) {
                from = previousEnd + MIN_GROUND_RUN + 1;
                to = from + width - 1;
            }
            if (to > spanEnd) {
                break;
            }
            accepted.add(new int[]{from, to});
            previousEnd = to;
        }
        return accepted.toArray(new int[0][]);
    }

    int midpoint() {
        return length / 2;
    }

    boolean has(CourseLayout.Feature feature) {
        return features.contains(feature);
    }

    /** First planned pit at or past {@code offset}, or null when the course has none left. */
    int[] gapAfter(int offset) {
        for (int[] gap : gaps) {
            if (gap[0] >= offset) {
                return gap;
            }
        }
        return null;
    }

    boolean hasGroundAt(int offset) {
        for (int[] gap : gaps) {
            if (offset >= gap[0] && offset <= gap[1]) {
                return false;
            }
        }
        return true;
    }
}
