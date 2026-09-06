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
 * <p>Comfort: reduced-motion shortens the perceived blend by snapping at 50%, and the blend
 * easing is the profile's authored {@link CameraProfile#damping} scaled by the client's comfort
 * multiplier -- capped at the authored value, so a player can calm the camera and never make it
 * floatier than the profile designed it.
 *
 * <p><b>Look-ahead is not implemented.</b> {@link CameraProfile#lookAhead} is authored per profile
 * and documented as "horizontal look-ahead toward velocity, in blocks", and nothing in this class
 * or anywhere else ever reads it -- the 2.5D branch below sets yaw, pitch and roll and never
 * touches the camera position. This comment used to claim look-ahead scaled from config, which
 * was the only description of the feature anywhere and was describing something that did not
 * exist. The authored field is left alone rather than deleted, because it is the specification
 * for the feature whenever someone builds it; the config slider that pretended to scale it has
 * been removed, because a comfort setting that silently does nothing is worse than no setting.
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
            // How much of the blend is eased rather than linear.
            //
            // Two controls were specified for this and neither was applied. CameraProfile.damping
            // is authored per profile and described as "0 = rigid, 1 = floaty"; the config option
            // cameraSmoothing carried word-for-word the same description. Two rival knobs for one
            // quantity, both dead, is how they stayed consistent with each other.
            //
            // Resolved as authored-value-times-comfort-multiplier rather than by picking a winner,
            // which is what the class contract above has always claimed: the profile decides the
            // camera, and the player may calm it but never exceed it. Capped at the authored
            // damping so "never past authored profile bounds" is actually true.
            float authored = ClientCourseState.profileFor(sync.toMode()).damping();
            float comfort = Mth.clamp(
                    (float) (double) PlaneShiftConfig.CLIENT.cameraSmoothing.get(), 0.0F, 1.0F);
            float smoothstep = progress * progress * (3.0F - 2.0F * progress);
            float eased = Mth.lerp(Mth.clamp(authored * comfort, 0.0F, 1.0F), progress, smoothstep);

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
