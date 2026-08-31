package com.studio.planeshift.client.input;

import com.studio.planeshift.client.ClientCourseState;
import com.studio.planeshift.common.course.CourseState;
import com.studio.planeshift.common.mode.PlaneRail;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.client.player.KeyboardInput;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Input;
import net.minecraft.world.phys.Vec2;

/**
 * 2.5D input projection (Design Bible, "2.5D camera specification"):
 * "Project input onto course forward and vertical axes."
 *
 * <p>Replaces the local player's {@link KeyboardInput} while 2.5D play is active. A/D
 * move along the travel axis in screen space (D is always screen-right), W/S depth input
 * is discarded. Jump, sneak and sprint pass through untouched — "jump and role actions
 * remain identical across modes".
 *
 * <p>This class only writes {@code moveVector} and {@code keyPresses}. It does not touch
 * the player entity at all. The movement assists that genuinely need to change velocity
 * live in {@link PlaneMovementAssists}, and the avatar's facing is presentation handled
 * there too.
 */
public final class PlaneConstrainedInput extends KeyboardInput {

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

        float impulse = (keyPresses.right() ? 1.0F : 0.0F) - (keyPresses.left() ? 1.0F : 0.0F);
        if (impulse == 0.0F) {
            this.moveVector = Vec2.ZERO;
            this.keyPresses = new Input(false, false, false, false,
                    keyPresses.jump(), keyPresses.shift(), keyPresses.sprint());
            return;
        }

        // Screen-space remap: camera yaw + 90 degrees is world "screen right".
        float travelYaw = rail.sideOnCameraYaw() + 90.0F;
        if (impulse < 0.0F) {
            travelYaw -= 180.0F;
        }

        this.moveVector = railProjection(player.getYRot(), travelYaw);
        // The keyPresses rewrite below erases the left/right bits, so publish the heading for
        // the avatar-facing pass before it is gone.
        PlaneMovementAssists.recordTravelYaw(travelYaw);
        // Depth is gone, and the remap has already consumed the strafe keys. Reporting a
        // forward press keeps the server's view of the input consistent with the vector.
        this.keyPresses = new Input(true, false, false, false,
                keyPresses.jump(), keyPresses.shift(), keyPresses.sprint());
    }

    /**
     * The {@code (strafe, forward)} pair that walks the player along {@code travelYaw} in world
     * space, whichever way they happen to be looking.
     *
     * <p>This is what lets the projection stay off the player entity. {@code moveVector} is
     * interpreted relative to the player's own yaw — {@code Entity#getInputVector} rotates it by
     * {@code getYRot()} before adding it to the velocity — so the previous approach of forcing
     * {@code setYRot} to the travel direction and pushing a constant {@code (0, 1)} was really
     * just a way of making that rotation the identity. Cancelling the yaw here instead gives the
     * same world-space motion without writing to the player, and leaves the look direction free
     * for aiming (see {@code FormActionPayload}, which sends {@code getLookAngle}).
     *
     * <p>Derivation: for player yaw {@code t} and travel yaw {@code v}, the engine maps
     * {@code (x, z)} to world {@code (x·cos t − z·sin t, z·cos t + x·sin t)}. Substituting
     * {@code x = sin(t − v)} and {@code z = cos(t − v)} collapses to {@code (−sin v, cos v)},
     * which is the unit vector along {@code v}, independent of {@code t}.
     *
     * <p>Package-private and static so the geometry can be exercised on its own.
     */
    static Vec2 railProjection(float playerYaw, float travelYaw) {
        float delta = (playerYaw - travelYaw) * Mth.DEG_TO_RAD;
        return new Vec2(Mth.sin(delta), Mth.cos(delta));
    }
}
