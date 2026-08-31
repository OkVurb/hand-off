package com.studio.planeshift.server;

import com.studio.planeshift.common.course.CourseState;
import com.studio.planeshift.common.mode.PlaneRail;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

/**
 * Server-side movement rules for course play.
 *
 * <p>2.5D constraint (Design Bible, "2.5D camera specification"): "Project input onto
 * course forward and vertical axes. Keep depth drift within 0.05 block after collision
 * resolution." The client projects input; this service is the authoritative backstop
 * that corrects drift the client failed (or refused) to prevent.
 */
public final class MovementRuleService {

    private MovementRuleService() {
    }

    /** Called every server tick for every player. */
    public static void tick(ServerPlayer player) {
        CourseState state = CourseStateAccess.get(player);
        if (!state.inCourse()) {
            return;
        }

        // Kill plane: "Falling into a kill volume returns to checkpoint with a short wipe."
        if (player.getY() < state.killY()) {
            DamageService.down(player, player.damageSources().fellOutOfWorld());
            return;
        }

        if (state.in2_5D() && state.rail().isPresent()) {
            constrainToRail(player, state.rail().get());
        }

        FormService.tick(player);
    }

    private static void constrainToRail(ServerPlayer player, PlaneRail rail) {
        double drift = rail.driftBeyondCorridor(player.position());
        if (drift > PlaneRail.DRIFT_TOLERANCE) {
            Vec3 snapped = rail.snapToPlane(player.position());
            player.teleportTo(snapped.x, snapped.y, snapped.z);
            player.setDeltaMovement(rail.flattenVelocity(player.getDeltaMovement()));
            player.hurtMarked = true;
        }
    }
}
