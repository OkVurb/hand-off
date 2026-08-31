package com.studio.planeshift.client.input;

import com.studio.planeshift.client.ClientCourseState;
import com.studio.planeshift.common.course.CourseState;
import com.studio.planeshift.common.mode.PlaneRail;
import com.studio.planeshift.common.role.PlayerRole;
import com.studio.planeshift.common.role.RoleSignature;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.client.player.KeyboardInput;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Input;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;

/**
 * 2.5D input projection (Design Bible, "2.5D camera specification"):
 * "Project input onto course forward and vertical axes."
 *
 * <p>Replaces the local player's {@link KeyboardInput} while 2.5D play is active. A/D
 * move along the travel axis in screen space (D is always screen-right), W/S depth input
 * is discarded, and the avatar faces its direction of travel. Jump, sneak and sprint
 * pass through untouched — "jump and role actions remain identical across modes".
 *
 * <p>Also implements the client-side movement assists (coyote time, jump buffering,
 * Glider float). These are presentation-adjacent assists on the client's own physics;
 * landing, damage, pickups and Form consumption stay server results.
 */
public final class PlaneConstrainedInput extends KeyboardInput {

    /** "Implement three ticks of coyote time and three ticks of jump buffering at 20 TPS." */
    private static final int COYOTE_TICKS = 3;
    private static final int JUMP_BUFFER_TICKS = 3;

    private int ticksSinceGrounded;
    private int jumpBufferRemaining;
    private boolean jumpWasDown;
    private int floatTicksUsed;

    public PlaneConstrainedInput(Options options) {
        super(options);
    }

    @Override
    public void tick() {
        super.tick();

        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null) {
            return;
        }
        CourseState state = ClientCourseState.get();
        if (!state.in2_5D() || state.rail().isEmpty()) {
            return;
        }
        PlaneRail rail = state.rail().get();

        // Screen-space remap: camera yaw + 90 degrees is world "screen right".
        float screenRightYaw = rail.sideOnCameraYaw() + 90.0F;
        float impulse = (keyPresses.right() ? 1.0F : 0.0F) - (keyPresses.left() ? 1.0F : 0.0F);

        if (impulse != 0.0F) {
            player.setYRot(impulse > 0.0F ? screenRightYaw : screenRightYaw - 180.0F);
            player.setYHeadRot(player.getYRot());
        }
        // Forward-only move vector in the direction the avatar now faces; depth dies here.
        this.moveVector = new Vec2(0.0F, Math.abs(impulse) > 0.0F ? 1.0F : 0.0F);
        this.keyPresses = new Input(
                impulse != 0.0F,   // forward along travel
                false,             // no backward
                false, false,      // strafes consumed by the remap
                keyPresses.jump(), keyPresses.shift(), keyPresses.sprint());

        tickAssists(player);
    }

    private void tickAssists(LocalPlayer player) {
        boolean jumpDown = keyPresses.jump();
        boolean jumpPressedEdge = jumpDown && !jumpWasDown;
        jumpWasDown = jumpDown;

        if (player.onGround()) {
            ticksSinceGrounded = 0;
            floatTicksUsed = 0;
        } else {
            ticksSinceGrounded++;
        }

        // Jump buffering: a press just before landing is honored on the landing tick.
        if (jumpPressedEdge && !player.onGround()) {
            jumpBufferRemaining = JUMP_BUFFER_TICKS;
        } else if (jumpBufferRemaining > 0) {
            jumpBufferRemaining--;
            if (player.onGround()) {
                makeJump();
                jumpBufferRemaining = 0;
            }
        }

        // Coyote time: a press just after walking off a ledge still jumps.
        Vec3 velocity = player.getDeltaMovement();
        if (jumpPressedEdge && !player.onGround()
                && ticksSinceGrounded <= COYOTE_TICKS && velocity.y < 0.0D) {
            float jumpPower = (float) player.getAttributeValue(Attributes.JUMP_STRENGTH);
            player.setDeltaMovement(velocity.x, jumpPower, velocity.z);
            ticksSinceGrounded = COYOTE_TICKS + 1;
        }

        // Glider signature: "Hold jump to float up to 1.25 s."
        PlayerRole role = ClientCourseState.localRole().orElse(null);
        if (role != null && role.signature() == RoleSignature.FLOAT_GLIDE
                && jumpDown && !player.onGround() && velocity.y < -0.06D
                && floatTicksUsed < role.floatTicks()) {
            player.setDeltaMovement(velocity.x, -0.06D, velocity.z);
            player.resetFallDistance();
            floatTicksUsed++;
        }
    }
}
