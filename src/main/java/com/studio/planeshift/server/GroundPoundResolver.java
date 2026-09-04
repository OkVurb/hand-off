package com.studio.planeshift.server;

/**
 * Decides what a ground pound does to one nearby enemy.
 *
 * <p>Deliberately free of every Minecraft type. The interesting part of a shockwave is a set of
 * range and precedence rules, and those rules are wrong far more often than the entity plumbing
 * around them — an inverted comparison or a radius applied in three dimensions instead of two
 * produces a move that fires through floors, or never fires at all, and neither is visible in a
 * build log. Keeping the decision here means it runs in a plain JUnit classloader with no server,
 * which is the distinction review R6 was written about: unit tests carry logic, GameTests carry
 * environment.
 *
 * <p>The one rule worth stating outright: <b>range is horizontal.</b> A pound is a wave along the
 * floor, so an enemy on a platform two blocks up is not in it even though a spherical distance
 * check would say otherwise. Vertical reach is a separate, much tighter bound.
 */
public final class GroundPoundResolver {

    /** What the wave does to a given enemy. */
    public enum Response {
        /** Flipped onto its back: harmless, stompable, and going nowhere for a few seconds. */
        FLIP,
        /** Knocked off its feet briefly — the crowd-clearing part of the move. */
        STAGGER,
        /** Out of range, airborne, or immune. */
        NONE
    }

    /**
     * How far the flip reaches, horizontally.
     *
     * <p>One block. The flip is the strong half of the move — it strips a shelled enemy of the
     * armour that is its whole identity — so it has to be aimed. Widening this turns "land on the
     * Buzzy Beetle" into "land somewhere near the Buzzy Beetle", and the difference between those
     * two is the entire skill in the mechanic.
     */
    public static final double FLIP_RADIUS = 1.6D;

    /** How far the stagger reaches. Wider, because it only buys a moment. */
    public static final double STAGGER_RADIUS = 3.0D;

    /**
     * Vertical reach, in blocks either way.
     *
     * <p>Tight on purpose. A pound travels along the ground it landed on; catching an enemy on the
     * platform above would make the move an area nuke rather than a floor sweep.
     */
    public static final double VERTICAL_REACH = 1.5D;

    /** How long a flipped enemy stays on its back. Long enough to walk over and stomp it. */
    public static final int FLIP_TICKS = 70;

    /** How long a staggered enemy is stopped. A beat, not a stun-lock. */
    public static final int STAGGER_TICKS = 15;

    private GroundPoundResolver() {
    }

    /**
     * Classifies one enemy against a pound that landed at the origin.
     *
     * @param dx        enemy x minus impact x
     * @param dy        enemy y minus impact y
     * @param dz        enemy z minus impact z
     * @param shelled   whether this enemy has a shell to be flipped out of
     * @param grounded  whether it is standing on something
     * @param immune    whether it opts out entirely (bosses, anchored and airborne enemies)
     */
    public static Response classify(double dx, double dy, double dz,
                                    boolean shelled, boolean grounded, boolean immune) {
        if (immune || !grounded) {
            // An airborne enemy has no feet to sweep. Checking this before range matters: it is
            // why a Boo drifting through the blast is untouched rather than merely lucky.
            return Response.NONE;
        }
        if (Math.abs(dy) > VERTICAL_REACH) {
            return Response.NONE;
        }
        double horizontal = Math.sqrt(dx * dx + dz * dz);
        if (horizontal > STAGGER_RADIUS) {
            return Response.NONE;
        }
        if (shelled && horizontal <= FLIP_RADIUS) {
            return Response.FLIP;
        }
        return Response.STAGGER;
    }
}
