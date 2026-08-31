package com.studio.planeshift.common.course;

import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.Pose;

/**
 * The course crouch hitbox rule.
 *
 * <p>Vanilla crouching only drops the player from 1.8 to 1.5 blocks, which is not enough to slide
 * under the one-block gaps a platformer wants. Inside a course the crouched box collapses to
 * {@link #CROUCH_HEIGHT} instead.
 *
 * <p>Lives in {@code common} and takes the course flag as a plain boolean because the rule has to
 * produce the same answer on both sides: the server owns collision, but the client predicts its
 * own movement, and a client that disagrees about the player's height will stutter against
 * geometry the server thinks it fits through. The two subscribers differ only in where they read
 * that boolean from.
 */
public final class CourseCrouch {

    /** Crouched height in a course. Low enough to pass a one-block gap. */
    public static final float CROUCH_HEIGHT = 0.5F;

    /** Eye height as a fraction of the crouched box, keeping the camera just below the ceiling. */
    private static final float EYE_RATIO = 0.8F;

    private CourseCrouch() {
    }

    /**
     * The dimensions a player should use, or {@code null} when the default applies.
     *
     * <p>Returns null rather than the input so callers can skip the event write entirely and
     * leave every non-course pose exactly as vanilla computed it.
     */
    public static EntityDimensions crouchedDimensions(Pose pose, EntityDimensions current, boolean inCourse) {
        if (!inCourse || pose != Pose.CROUCHING) {
            return null;
        }
        if (current.height() <= CROUCH_HEIGHT) {
            return null;   // already at or below target; nothing to do
        }
        return EntityDimensions.fixed(current.width(), CROUCH_HEIGHT)
                .withEyeHeight(CROUCH_HEIGHT * EYE_RATIO);
    }
}
