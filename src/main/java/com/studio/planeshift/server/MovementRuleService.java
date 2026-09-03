package com.studio.planeshift.server;

import com.studio.planeshift.common.course.CourseState;
import com.studio.planeshift.common.mode.PlaneRail;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
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

    /**
     * Depth speed below this is ordinary collision jitter and is simply dropped. Folding it would
     * add a constant tiny push along the rail.
     */
    private static final double DEPTH_FOLD_THRESHOLD = 0.08D;

    private static final java.util.Map<ServerPlayer, Integer> SPRINT_TICKS = new java.util.WeakHashMap<>();

    /** Last measured X position, so the skid check reads movement the server can trust. */
    private static final java.util.Map<ServerPlayer, Double> LAST_X = new java.util.WeakHashMap<>();

    /** Smoothed measured X velocity, so a single jittery packet does not read as a turnaround. */
    private static final java.util.Map<ServerPlayer, Double> SMOOTH_VEL_X = new java.util.WeakHashMap<>();

    /** Ticks left before another skid puff may play. */
    private static final java.util.Map<ServerPlayer, Integer> SKID_COOLDOWN = new java.util.WeakHashMap<>();

    /**
     * How long the skid stays quiet after firing.
     *
     * <p>A skid is a one-shot flourish, but its trigger — the sign of horizontal velocity flipping
     * — is true on a large fraction of ticks for a player tapping left and right. Without a
     * cooldown that is a sound effect several times a second, which reads as a bug rather than as
     * feedback.
     */
    private static final int SKID_COOLDOWN_TICKS = 12;

    /** Speed the measured velocity must exceed before a reversal counts as a skid, not a nudge. */
    private static final double SKID_MIN_SPEED = 0.12D;

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

        // P-Speed running meter
        if (player.onGround() && player.isSprinting()) {
            int ticks = SPRINT_TICKS.getOrDefault(player, 0) + 1;
            SPRINT_TICKS.put(player, ticks);
            if (ticks == 30) {
                player.level().playSound(null, player.blockPosition(), com.studio.planeshift.common.registry.ModSounds.POWER_UP.get(), net.minecraft.sounds.SoundSource.PLAYERS, 0.6F, 1.8F);
            }
            if (ticks >= 30) {
                Vec3 v = player.getDeltaMovement();
                player.setDeltaMovement(v.x * 1.05D, v.y, v.z * 1.05D);
                if (player.level() instanceof net.minecraft.server.level.ServerLevel sl && player.level().getGameTime() % 2 == 0) {
                    sl.sendParticles(net.minecraft.core.particles.ParticleTypes.CLOUD, player.getX(), player.getY() + 0.1D, player.getZ(), 1, 0.05D, 0.02D, 0.05D, 0.01D);
                }
            }
        } else if (!player.isSprinting()) {
            SPRINT_TICKS.remove(player);
        }

        // Skid turnaround.
        //
        // Measured from position rather than from getDeltaMovement(): the client owns player
        // movement, so the server's delta is often stale or zero while the player is plainly
        // running, and a stale delta never flips sign — the effect simply would not fire.
        Double lastX = LAST_X.get(player);
        double measuredVx = lastX == null ? 0.0D : player.getX() - lastX;
        LAST_X.put(player, player.getX());

        double previousVx = SMOOTH_VEL_X.getOrDefault(player, 0.0D);
        int skidCooldown = SKID_COOLDOWN.getOrDefault(player, 0);
        if (skidCooldown > 0) {
            skidCooldown--;
        } else if (player.onGround()
                && Math.abs(previousVx) > SKID_MIN_SPEED
                && Math.abs(measuredVx) > SKID_MIN_SPEED * 0.5D
                && measuredVx * previousVx < 0.0D
                && player.level() instanceof net.minecraft.server.level.ServerLevel sl) {
            sl.sendParticles(net.minecraft.core.particles.ParticleTypes.CAMPFIRE_COSY_SMOKE,
                    player.getX(), player.getY() + 0.1D, player.getZ(), 4, 0.1D, 0.05D, 0.1D, 0.02D);
            sl.playSound(null, player.blockPosition(),
                    com.studio.planeshift.common.registry.ModSounds.BRICK_BREAK.get(),
                    net.minecraft.sounds.SoundSource.PLAYERS, 0.4F, 1.6F);
            skidCooldown = SKID_COOLDOWN_TICKS;
        }
        SKID_COOLDOWN.put(player, skidCooldown);
        // Exponential smoothing: one dropped or duplicated movement packet should not be able to
        // manufacture a direction change on its own.
        SMOOTH_VEL_X.put(player, previousVx * 0.6D + measuredVx * 0.4D);

        if (state.in2_5D() && state.rail().isPresent()) {
            constrainToRail(player, state.rail().get());
        }

        FormService.tick(player);
    }

    /**
     * Folds depth momentum onto the travel axis instead of discarding it.
     *
     * <p>Simply zeroing the depth component keeps the player on the rail, but it throws away any
     * movement aimed across it. That is what makes a third-party dash feel broken in 2.5D: a dash
     * fires along the direction the player is *looking*, the camera is side-on, so most of that
     * impulse points into the screen — straight down the depth axis — and zeroing it turns the
     * dash into nothing at all.
     *
     * <p>Rotating the component onto the travel axis preserves the speed the dash asked for and
     * spends it in the direction the player is actually moving. Sign comes from their current
     * travel, falling back to the way they are facing when they are standing still, so a dash from
     * a standstill still goes somewhere sensible rather than picking an arbitrary direction.
     */
    private static Vec3 projectOntoRail(PlaneRail rail, Vec3 velocity, ServerPlayer player) {
        Vec3 flattened = rail.flattenVelocity(velocity);
        double depth = depthComponent(rail, velocity);
        if (Math.abs(depth) < DEPTH_FOLD_THRESHOLD) {
            return flattened;
        }

        double travel = travelComponent(rail, flattened);
        double sign = travel != 0.0D ? Math.signum(travel) : facingSign(rail, player);
        double folded = travel + sign * Math.abs(depth);
        return rail.travelAxis() == Direction.Axis.X
                ? new Vec3(folded, flattened.y, flattened.z)
                : new Vec3(flattened.x, flattened.y, folded);
    }

    private static double depthComponent(PlaneRail rail, Vec3 velocity) {
        return rail.travelAxis() == Direction.Axis.X ? velocity.z : velocity.x;
    }

    private static double travelComponent(PlaneRail rail, Vec3 velocity) {
        return rail.travelAxis() == Direction.Axis.X ? velocity.x : velocity.z;
    }

    /** Which way along the rail the player is facing, as -1 or 1. */
    private static double facingSign(PlaneRail rail, ServerPlayer player) {
        float yaw = player.getYRot() * Mth.DEG_TO_RAD;
        double along = rail.travelAxis() == Direction.Axis.X
                ? -Mth.sin(yaw)
                : Mth.cos(yaw);
        return along >= 0.0D ? 1.0D : -1.0D;
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
        Vec3 projected = projectOntoRail(rail, velocity, player);
        if (!projected.equals(velocity)) {
            player.setDeltaMovement(projected);
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
