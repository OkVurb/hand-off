package com.studio.planeshift.server;

import com.studio.planeshift.PlaneShift;
import com.studio.planeshift.common.PlaneShiftConfig;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

/**
 * The course movement baseline: everyone jumps higher and runs faster inside a course.
 *
 * <p>This used to live in {@link RoleService}, applied alongside the per-role multipliers. That
 * was wrong in a way that made the whole mod feel broken: {@code RoleService} only applies
 * anything when a role is selected, no role is ever selected by default, and so a player who had
 * not run the role command was platforming at vanilla jump height and vanilla walking speed —
 * which cannot clear the gaps the course generator builds.
 *
 * <p>The baseline belongs to being in a course, not to having a role. Roles still tune it, and
 * because they apply as {@code ADD_MULTIPLIED_TOTAL} on top of this, a role's ±12% stays ±12% of
 * the boosted value rather than of the vanilla one.
 */
public final class CourseMovementService {

    private static final Identifier JUMP_MODIFIER_ID = PlaneShift.id("course_jump_base");
    private static final Identifier RUN_MODIFIER_ID = PlaneShift.id("course_run_base");

    private CourseMovementService() {
    }

    /**
     * Applies or removes the baseline to match whether the player is in a course.
     *
     * <p>Safe to call every time the player's situation changes; the modifiers are removed before
     * being re-added, so repeated calls cannot stack them.
     */
    public static void refresh(ServerPlayer player) {
        clear(player);
        if (!CourseStateAccess.get(player).inCourse()) {
            return;
        }

        AttributeInstance jump = player.getAttribute(Attributes.JUMP_STRENGTH);
        if (jump != null) {
            jump.addTransientModifier(new AttributeModifier(JUMP_MODIFIER_ID,
                    PlaneShiftConfig.SERVER.courseJumpBoost.get(),
                    AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
        }
        AttributeInstance speed = player.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speed != null) {
            speed.addTransientModifier(new AttributeModifier(RUN_MODIFIER_ID,
                    PlaneShiftConfig.SERVER.courseRunBoost.get(),
                    AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
        }
    }

    /** Drops the baseline. Transient modifiers, so nothing leaks into the save. */
    public static void clear(ServerPlayer player) {
        AttributeInstance jump = player.getAttribute(Attributes.JUMP_STRENGTH);
        if (jump != null) {
            jump.removeModifier(JUMP_MODIFIER_ID);
        }
        AttributeInstance speed = player.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speed != null) {
            speed.removeModifier(RUN_MODIFIER_ID);
        }
    }
}
