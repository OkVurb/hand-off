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

    /**
     * Holds the player on the rail.
     *
     * <p>Two stages, and the order matters. Depth velocity is zeroed every tick first, so anything
     * that added momentum on the depth axis is neutralised before it can move the player at all.
     * Only then is position checked, and a teleport used as the backstop.
     *
     * <p>The velocity stage is what makes third-party movement mods survivable. This service used
     * to teleport and nothing else: a mod that adds depth velocity every tick — a dodge, a
     * wall-run, a vault — would push the player out of the corridor every tick and be teleported
     * back every tick, which reads as rubber-banding rather than as a mod conflict. Zeroing the
     * component costs one vector allocation and means the teleport almost never fires.
     *
     * <p>Only the depth component is touched. Travel and vertical momentum are left alone, so a
     * movement mod's along-the-rail behaviour still works — which is the useful half of it in a
     * side-on course anyway.
     */
    private static void constrainToRail(ServerPlayer player, PlaneRail rail) {
        Vec3 velocity = player.getDeltaMovement();
        Vec3 flattened = rail.flattenVelocity(velocity);
        if (!flattened.equals(velocity)) {
            player.setDeltaMovement(flattened);
            // No hurtMarked here: a velocity correction the client also predicts does not need a
            // forced position sync, and marking every tick would fight the client's own movement.
        }

        double drift = rail.driftBeyondCorridor(player.position());
        if (drift > PlaneRail.DRIFT_TOLERANCE) {
            Vec3 snapped = rail.snapToPlane(player.position());
            player.teleportTo(snapped.x, snapped.y, snapped.z);
            player.setDeltaMovement(rail.flattenVelocity(player.getDeltaMovement()));
            player.hurtMarked = true;
        }
    }
}
