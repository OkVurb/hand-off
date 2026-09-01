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
                        int setPieceCount) {

    /** Blocks of ground guaranteed between two pits, so they can never merge into one. */
    static final int MIN_GROUND_RUN = 4;
    /** Ground kept solid either side of the midpoint, where the checkpoint beacon stands. */
    private static final int CHECKPOINT_CLEARANCE = 2;
    /** Roughly one pit per this many blocks when the course does not say otherwise. */
    private static final int BLOCKS_PER_GAP = 19;
    /** Roughly one set piece per this many blocks when the course does not say otherwise. */
    private static final int BLOCKS_PER_SET_PIECE = 24;

    static CourseLayoutPlan forCourse(CourseDefinition course) {
        return build(course.theme(), course.length(), course.layout().orElse(CourseLayout.DEFAULT));
    }

    /** Convenience for the theme defaults; the generator always goes through {@link #forCourse}. */
    static CourseLayoutPlan forTheme(CourseTheme theme, int length) {
        return build(theme, length, CourseLayout.DEFAULT);
    }

    static CourseLayoutPlan build(CourseTheme theme, int length, CourseLayout layout) {
        int[][] gaps = layout.gaps()
                .map(list -> explicitGaps(list, length, layout))
                .orElseGet(() -> derivedGaps(theme, length, layout));
        int setPieces = layout.setPieceCount()
                .orElseGet(() -> Math.clamp(length / BLOCKS_PER_SET_PIECE, 3, 12));
        return new CourseLayoutPlan(length, gaps, layout.resolvedFeatures(theme), setPieces);
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
    private static int[][] derivedGaps(CourseTheme theme, int length, CourseLayout layout) {
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
        int shift = theme.ordinal();
        int mid = length / 2;

        List<int[]> accepted = new ArrayList<>();
        int previousEnd = spanStart - MIN_GROUND_RUN - 1;
        for (int i = 0; i < count; i++) {
            int width = Math.min(layout.maxGapWidth(), 4 + ((i + shift) % widthSpread));
            int centre = spanStart + slot * i + slot / 2;
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
