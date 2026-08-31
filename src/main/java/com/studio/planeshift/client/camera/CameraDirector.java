package com.studio.planeshift.client.camera;

import com.studio.planeshift.client.ClientCourseState;
import com.studio.planeshift.common.PlaneShiftConfig;
import com.studio.planeshift.common.camera.CameraProfile;
import com.studio.planeshift.common.course.CourseState;
import com.studio.planeshift.common.mode.PlaneMode;
import com.studio.planeshift.common.mode.TransitionSync;
import java.util.Optional;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.neoforged.neoforge.client.event.CalculateDetachedCameraDistanceEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;

/**
 * The client camera solve (Design Bible, "2.5D camera specification").
 *
 * <p>2.5D uses a perspective rail camera: fixed side-on yaw derived from the rail,
 * authored pitch/FOV/distance from the {@link CameraProfile}. During a transition the
 * angles blend between bases with smoothstep easing; the collision basis is untouched
 * (that is the server's commit).
 *
 * <p>Comfort: reduced-motion shortens the perceived blend by snapping at 50%; smoothing
 * and look-ahead scale from client config, never past authored profile bounds.
 */
public final class CameraDirector {

    private static CameraType restoreCameraType = null;

    private CameraDirector() {
    }

    /** True while PlaneShift is driving the camera. */
    public static boolean active() {
        CourseState state = ClientCourseState.get();
        return state.inCourse() || state.transition().isPresent();
    }

    public static void onComputeCameraAngles(ViewportEvent.ComputeCameraAngles event) {
        CourseState state = ClientCourseState.get();
        Optional<TransitionSync> transition = state.transition();
        long gameTime = Minecraft.getInstance().level != null
                ? Minecraft.getInstance().level.getGameTime() : 0L;

        if (transition.isPresent()) {
            TransitionSync sync = transition.get();
            float progress = sync.progress(gameTime, (float) event.getPartialTick());
            if (PlaneShiftConfig.CLIENT.reducedMotion.get()) {
                // Identical transaction timing, shorter perceived motion.
                progress = progress < 0.5F ? 0.0F : 1.0F;
            }
            float eased = progress * progress * (3.0F - 2.0F * progress);

            float fromYaw = angleFor(sync.fromMode(), state, event.getYaw());
            float fromPitch = pitchFor(sync.fromMode(), state, event.getPitch());
            float toYaw = angleForTarget(sync, event.getYaw());
            float toPitch = sync.toMode() == PlaneMode.SIDE_ON
                    ? ClientCourseState.profileFor(PlaneMode.SIDE_ON).pitchDegrees()
                    : event.getPitch();

            event.setYaw(Mth.rotLerp(eased, fromYaw, toYaw));
            event.setPitch(Mth.lerp(eased, fromPitch, toPitch));
            event.setRoll(0.0F);
            return;
        }

        if (state.in2_5D() && state.rail().isPresent()) {
            CameraProfile profile = ClientCourseState.profileFor(PlaneMode.SIDE_ON);
            event.setYaw(state.rail().get().sideOnCameraYaw());
            event.setPitch(profile.pitchDegrees());
            event.setRoll(0.0F);
        }
    }

    private static float angleFor(PlaneMode mode, CourseState state, float vanillaYaw) {
        if (mode == PlaneMode.SIDE_ON && state.rail().isPresent()) {
            return state.rail().get().sideOnCameraYaw();
        }
        return vanillaYaw;
    }

    private static float pitchFor(PlaneMode mode, CourseState state, float vanillaPitch) {
        if (mode == PlaneMode.SIDE_ON && state.rail().isPresent()) {
            return ClientCourseState.profileFor(PlaneMode.SIDE_ON).pitchDegrees();
        }
        return vanillaPitch;
    }

    private static float angleForTarget(TransitionSync sync, float vanillaYaw) {
        if (sync.toMode() == PlaneMode.SIDE_ON && sync.targetRail().isPresent()) {
            return sync.targetRail().get().sideOnCameraYaw();
        }
        return vanillaYaw;
    }

    public static void onCameraDistance(CalculateDetachedCameraDistanceEvent event) {
        CourseState state = ClientCourseState.get();
        PlaneMode presented = presentedCameraMode(state);
        if (presented == null) {
            return;
        }
        event.setDistance(ClientCourseState.profileFor(presented).distance());
    }

    public static void onComputeFov(ViewportEvent.ComputeFov event) {
        if (!event.usedConfiguredFov()) {
            return;
        }
        CourseState state = ClientCourseState.get();
        if (state.in2_5D()) {
            event.setFOV(ClientCourseState.profileFor(PlaneMode.SIDE_ON).fovDegrees());
        }
    }

    /** Ticked from the client: forces third person while PlaneShift owns the camera. */
    public static void tickCameraType() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft == null) {
            return;
        }
        CourseState state = ClientCourseState.get();
        boolean owns = state.inCourse() || state.transition().isPresent();
        if (owns) {
            if (minecraft.options.getCameraType() != CameraType.THIRD_PERSON_BACK) {
                if (restoreCameraType == null) {
                    restoreCameraType = minecraft.options.getCameraType();
                }
                minecraft.options.setCameraType(CameraType.THIRD_PERSON_BACK);
            }
        } else if (restoreCameraType != null) {
            minecraft.options.setCameraType(restoreCameraType);
            restoreCameraType = null;
        }
    }

    /** The mode whose camera profile should drive distance/FOV right now. */
    private static PlaneMode presentedCameraMode(CourseState state) {
        if (state.transition().isPresent()) {
            TransitionSync sync = state.transition().get();
            long gameTime = Minecraft.getInstance().level != null
                    ? Minecraft.getInstance().level.getGameTime() : 0L;
            return sync.progress(gameTime, 0.0F) < 0.5F ? sync.fromMode() : sync.toMode();
        }
        if (state.in2_5D()) {
            return PlaneMode.SIDE_ON;
        }
        if (state.inCourse()) {
            return PlaneMode.FREE_3D;
        }
        return null;
    }
}
