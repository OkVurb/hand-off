package com.studio.planeshift.client.input;

import com.studio.planeshift.client.ClientCourseState;
import com.studio.planeshift.common.course.CourseState;
import com.studio.planeshift.common.role.PlayerRole;
import com.studio.planeshift.common.role.RoleSignature;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.Vec3;

/**
 * Client-side movement assists for 2.5D play: coyote time, jump buffering and the Glider
 * float ("Implement three ticks of coyote time and three ticks of jump buffering at 20 TPS",
 * "Hold jump to float up to 1.25 s").
 *
 * <p>These used to live inside {@link PlaneConstrainedInput}. They are separated because they
 * are not input projection — they change the local player's velocity, which an
 * {@code Input} implementation has no business doing. Splitting them also puts them at the
 * right point in the tick: {@link #tick} is driven from {@code MovementInputUpdateEvent}
 * (see {@code ClientEvents#onMovementInputUpdate}), which NeoForge fires inside
 * {@code LocalPlayer#aiStep} immediately after {@code input.tick()} and before the movement
 * for the tick is applied.
 *
 * <p>Changing your own {@code LocalPlayer} velocity is the normal, supported way to do this —
 * the client is authoritative for its own position and reports it upward. What matters is that
 * each assist stays inside what the server's movement check tolerates: the coyote jump is one
 * ordinary jump impulse, and the float only ever *reduces* descent speed, never climbs.
 *
 * <p>State is static and reset by {@link #reset()} whenever the input is reinstalled, so a
 * respawn or dimension change does not carry a stale coyote or float budget across.
 */
public final class PlaneMovementAssists {

    private static final int COYOTE_TICKS = 3;
    private static final int JUMP_BUFFER_TICKS = 3;

    /** Terminal descent while floating. Negative, so the Glider sinks rather than hovers. */
    private static final double FLOAT_FALL_SPEED = -0.06D;

    private static int ticksSinceGrounded;
    private static int jumpBufferRemaining;
    private static boolean jumpWasDown;
    private static int floatTicksUsed;

    /** Last direction the player actually travelled, so the avatar keeps facing it at rest. */
    private static float lastTravelYaw;
    private static boolean hasTravelYaw;

    private PlaneMovementAssists() {
    }

    /** Clears carried-over assist budgets. Call when the input is reinstalled. */
    public static void reset() {
        ticksSinceGrounded = 0;
        jumpBufferRemaining = 0;
        jumpWasDown = false;
        floatTicksUsed = 0;
        hasTravelYaw = false;
    }

    /**
     * Applies the assists for this tick. Called from {@code MovementInputUpdateEvent}, which
     * fires after the input has been projected and before the player moves.
     */
    public static void tick(LocalPlayer player) {
        CourseState state = ClientCourseState.get();
        if (!state.in2_5D()) {
            reset();
            return;
        }

        boolean jumpDown = player.input.keyPresses.jump();
        boolean jumpPressedEdge = jumpDown && !jumpWasDown;
        jumpWasDown = jumpDown;

        boolean onGround = player.onGround();
        if (onGround) {
            ticksSinceGrounded = 0;
            floatTicksUsed = 0;
        } else {
            ticksSinceGrounded++;
        }

        // Jump buffering: a press just before landing is honored on the landing tick. This one
        // is pure input — makeJump only raises the jump bit, the engine does the rest.
        if (jumpPressedEdge && !onGround) {
            jumpBufferRemaining = JUMP_BUFFER_TICKS;
        } else if (jumpBufferRemaining > 0) {
            jumpBufferRemaining--;
            if (onGround) {
                player.input.makeJump();
                jumpBufferRemaining = 0;
            }
        }

        // Coyote time: a press just after walking off a ledge still jumps.
        Vec3 velocity = player.getDeltaMovement();
        boolean coyoteJumped = false;
        if (jumpPressedEdge && !onGround
                && ticksSinceGrounded <= COYOTE_TICKS && velocity.y < 0.0D) {
            float jumpPower = (float) player.getAttributeValue(Attributes.JUMP_STRENGTH);
            player.setDeltaMovement(velocity.x, jumpPower, velocity.z);
            ticksSinceGrounded = COYOTE_TICKS + 1;
            coyoteJumped = true;
        }

        // Glider signature: "Hold jump to float up to 1.25 s."
        PlayerRole role = ClientCourseState.localRole().orElse(null);
        if (coyoteJumped || role == null || role.signature() != RoleSignature.FLOAT_GLIDE) {
            return;
        }
        // Re-read: the coyote branch above may have just replaced the velocity, and floating
        // off a stale downward reading would cancel the jump it had only just granted.
        velocity = player.getDeltaMovement();
        if (jumpDown && !onGround && velocity.y < FLOAT_FALL_SPEED
                && floatTicksUsed < role.floatTicks()) {
            player.setDeltaMovement(velocity.x, FLOAT_FALL_SPEED, velocity.z);
            player.resetFallDistance();
            floatTicksUsed++;
        }
    }

    /**
     * Records the heading {@link PlaneConstrainedInput} projected onto this tick.
     *
     * <p>The facing pass below cannot recover this from the player's input: the projection
     * rewrites {@code keyPresses} to a plain forward press, so the left/right bits that named
     * the direction are gone by the time the client tick ends. The producer publishes it here
     * instead. Only called on ticks that actually produced movement, so the avatar keeps facing
     * the way it last travelled while standing still.
     */
    static void recordTravelYaw(float travelYaw) {
        lastTravelYaw = travelYaw;
        hasTravelYaw = true;
    }

    /**
     * Points the avatar's body along the direction of travel, so it still reads as a
     * side-scroller even though the projection no longer rotates the player.
     *
     * <p>Presentation only: this writes {@code yBodyRot}, not {@code yRot}. The look direction
     * stays the player's own, which is what {@code FormActionPayload} aims with. Called from
     * the client tick *after* entities have ticked, because {@code LivingEntity#tickHeadTurn}
     * would otherwise drag the body back toward the head within the same tick. The previous
     * value is written alongside it so rendering has nothing to interpolate across and the
     * avatar does not visibly snap.
     */
    public static void tickBodyFacing(LocalPlayer player) {
        CourseState state = ClientCourseState.get();
        if (!state.in2_5D() || state.rail().isEmpty()) {
            hasTravelYaw = false;
            return;
        }
        if (!hasTravelYaw) {
            return;
        }
        float facing = Mth.wrapDegrees(lastTravelYaw);
        player.yBodyRot = facing;
        player.yBodyRotO = facing;
    }
}
