package com.studio.planeshift.server.gen;

import java.util.Set;

/**
 * One authored piece of level.
 *
 * <p>A course is a sequence of these, not a field of per-block noise. That is how Mario levels are
 * actually built and it is the reason they read as designed rather than generated: a human author
 * writes a gap-jump, a staircase, a pipe row, an enemy gauntlet — and then arranges those pieces
 * into a rhythm. Generating individual blocks can produce a landscape, but it cannot produce an
 * idea, and a level without ideas is a corridor with decoration.
 *
 * <p>Every segment declares its own {@link SegmentSpec}, which is what lets the composer join them
 * safely: the composer only ever places a segment whose entry height matches the previous
 * segment's exit height, so the seam between two pieces is always walkable by construction. The
 * reachability check that runs afterwards is a proof, not a hope.
 */
public interface Segment {

    SegmentSpec spec();

    /**
     * Draws this segment into the canvas.
     *
     * @param canvas where to draw; coordinates are course-local
     * @param x      the local x this segment starts at
     * @param baseY  the floor height at the entry, as a local y
     * @param ctx    theme palette, difficulty and the seeded random
     */
    void build(CourseCanvas canvas, int x, int baseY, GenContext ctx);

    /**
     * What a segment is and how it may be joined.
     *
     * @param id         stable name, used in logs and tests
     * @param width      how many blocks of course it consumes
     * @param entryDrop  floor height at the entry, relative to the segment's base
     * @param exitRise   floor height at the exit, relative to the segment's base; the composer
     *                   carries this forward so the next segment starts at the right height
     * @param difficulty 0 (safe) to 4 (late-game), used by the arc to pace a course
     * @param tags       what mechanics this segment uses, so the composer can teach before testing
     */
    record SegmentSpec(String id, int width, int entryDrop, int exitRise, int difficulty,
                       Set<Tag> tags) {

        public SegmentSpec {
            if (width < 1) {
                throw new IllegalArgumentException("segment " + id + " has no width");
            }
            if (difficulty < 0 || difficulty > 4) {
                throw new IllegalArgumentException("segment " + id + " difficulty out of range");
            }
        }

        public boolean has(Tag tag) {
            return tags.contains(tag);
        }
    }

    /**
     * The mechanic a segment exercises.
     *
     * <p>Used for two things. The composer will not use a mechanic in a hard segment before a
     * gentle one has introduced it, which is the teaching structure every good Mario level
     * follows. And it stops a course being ten variations of the same idea, because the composer
     * penalises repeating a tag it has just used.
     */
    enum Tag {
        /** Plain ground. Always safe, used as a breather. */
        REST,
        /** A pit to jump. */
        GAP,
        /** Height gained or lost. */
        CLIMB,
        /** Blocks to hit from below. */
        BLOCKS,
        /** Enemies to defeat or avoid. */
        ENEMY,
        /** A platform that moves. */
        MOVING,
        /** Ground that disappears, falls or slides. */
        UNSTABLE,
        /** Something overhead: ceiling, Thwomp, firebar. */
        OVERHEAD,
        /** A reward route that is optional. */
        SECRET,
        /** A pipe, warp or door. */
        PIPE,
        /** Set piece; at most one per course. */
        SETPIECE
    }
}
