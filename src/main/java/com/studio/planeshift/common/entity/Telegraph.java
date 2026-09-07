package com.studio.planeshift.common.entity;

import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Draws where a moving thing is going to be, before it gets there.
 *
 * <h2>Why this is one class and not five features</h2>
 *
 * <p>The reference draws the path of everything that moves, and it does it five different ways
 * without ever calling attention to it: a firebar has a faint sweep circle at its reach, a buzzsaw
 * has a rail line running through the level, a hanging enemy has a tether with a dot at its anchor,
 * a pendulum platform has its swing arc, and a platform on a wire has the wire. Five drawings, one
 * idea -- a hazard shows its reach before it reaches you, and a platform shows its path before you
 * commit to the jump.
 *
 * <p>It matters more here than it does there. This mod has more moving hazards planned than the
 * reference actually has, and a side-on camera at our distance gives the player less warning than a
 * flat 2D view does: a thing that is about to swing into the lane can be genuinely invisible until
 * it arrives. A fight that is lost to something unseen is not difficult, it is unfair, and the
 * difference is entirely in whether the path was drawn.
 *
 * <h2>Why particles rather than a renderer</h2>
 *
 * <p>A proper line renderer would look better and would need a render type, a vertex format and a
 * client-side entity registry entry for every hazard that wanted one. Sparse particles need none of
 * that, ride the existing sync, and are cheap enough to emit every few ticks. The sparseness is
 * also the point: a solid line reads as a wall, and a wall is a thing you avoid rather than a thing
 * you time.
 */
public final class Telegraph {

    /**
     * Ticks between emissions.
     *
     * <p>Six is roughly three times a second, which is often enough that the path never looks like
     * it has gaps in it and rare enough that a course full of hazards does not turn into a particle
     * storm. Emitting every tick looked solid, which is the one thing it must not look.
     */
    public static final int INTERVAL = 6;

    /** Blocks between marks along a path. Wide enough to read as dots rather than as a line. */
    private static final double SPACING = 0.75D;

    private Telegraph() {
    }

    /** Whether this tick should emit. Callers gate on this so the cost is paid once. */
    public static boolean due(int tickCount) {
        return tickCount % INTERVAL == 0;
    }

    /**
     * A ring at radius {@code radius} around {@code centre}, in the vertical play plane.
     *
     * <p>For anything that turns about a hub. Drawn in the plane the lane lies in, matching the
     * plane the hazard actually sweeps, so from the side camera it reads as a circle rather than as
     * an ellipse seen edge-on.
     */
    public static void ring(Level level, Vec3 centre, double radius, ParticleOptions particle) {
        if (radius <= 0.0D) {
            return;
        }
        int marks = Math.max(8, (int) Math.round(2.0D * Math.PI * radius / SPACING));
        for (int i = 0; i < marks; i++) {
            double a = 2.0D * Math.PI * i / marks;
            emit(level, centre.add(Math.cos(a) * radius, Math.sin(a) * radius, 0.0D), particle);
        }
    }

    /**
     * A straight run between two points.
     *
     * <p>For anything that slides: a platform on a track, a crusher above the lane. The endpoints
     * are included, because the ends of a run are the part the player actually needs -- the middle
     * is obvious once the ends are known.
     */
    public static void line(Level level, Vec3 from, Vec3 to, ParticleOptions particle) {
        double length = from.distanceTo(to);
        int marks = Math.max(2, (int) Math.round(length / SPACING));
        for (int i = 0; i <= marks; i++) {
            emit(level, from.lerp(to, (double) i / marks), particle);
        }
    }

    /**
     * The arc a pendulum swings through, between two extremes about a pivot.
     *
     * <p>Not a chord: a swing is an arc and drawing it straight would tell the player the wrong
     * thing about where the platform is at the midpoint, which is exactly where they will try to
     * step onto it.
     */
    public static void arc(Level level, Vec3 pivot, double radius,
                           double fromDegrees, double toDegrees, ParticleOptions particle) {
        double sweep = Math.toRadians(Math.abs(toDegrees - fromDegrees));
        int marks = Math.max(4, (int) Math.round(sweep * radius / SPACING));
        for (int i = 0; i <= marks; i++) {
            double a = Math.toRadians(fromDegrees + (toDegrees - fromDegrees) * i / marks);
            emit(level, pivot.add(Math.cos(a) * radius, Math.sin(a) * radius, 0.0D), particle);
        }
    }

    private static void emit(Level level, Vec3 at, ParticleOptions particle) {
        if (level instanceof net.minecraft.server.level.ServerLevel server) {
            // Count 1, zero spread, zero speed: one mark exactly where the path is. Any spread
            // makes the path fuzzy, and a fuzzy path is worse than none because it implies a
            // tolerance the hazard does not actually have.
            server.sendParticles(particle, at.x, at.y, at.z, 1, 0.0D, 0.0D, 0.0D, 0.0D);
        }
    }
}
