package com.studio.planeshift.client;

import com.studio.planeshift.common.camera.CameraProfile;
import com.studio.planeshift.common.course.CourseState;
import com.studio.planeshift.common.registry.ModAttachments;
import com.studio.planeshift.common.registry.ModRegistries;
import com.studio.planeshift.common.role.PlayerRole;
import com.studio.planeshift.PlaneShift;
import java.util.Optional;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;

/**
 * Read-only client view of the local player's synced {@link CourseState} and the synced
 * datapack registries. The client never mutates gameplay state — it presents it.
 */
public final class ClientCourseState {

    private static final Identifier SIDE_PROFILE_ID = PlaneShift.id("side_standard");
    private static final Identifier FREE_PROFILE_ID = PlaneShift.id("free_standard");

    private ClientCourseState() {
    }

    public static CourseState get() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft == null) {
            return CourseState.DEFAULT;
        }
        LocalPlayer player = minecraft.player;
        return player != null ? player.getData(ModAttachments.COURSE_STATE) : CourseState.DEFAULT;
    }

    public static Optional<PlayerRole> localRole() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft == null) {
            return Optional.empty();
        }
        LocalPlayer player = minecraft.player;
        if (player == null) {
            return Optional.empty();
        }
        return get().roleId().flatMap(id -> player.registryAccess().lookup(ModRegistries.ROLE)
                .flatMap(registry -> registry.get(ResourceKey.create(ModRegistries.ROLE, id)))
                .map(holder -> holder.value()));
    }

    /** Authored camera profile for the mode, falling back to built-in constants. */
    public static CameraProfile profileFor(com.studio.planeshift.common.mode.PlaneMode mode) {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft != null ? minecraft.player : null;
        Identifier id = mode == com.studio.planeshift.common.mode.PlaneMode.SIDE_ON
                ? SIDE_PROFILE_ID : FREE_PROFILE_ID;
        CameraProfile fallback = mode == com.studio.planeshift.common.mode.PlaneMode.SIDE_ON
                ? CameraProfile.FALLBACK_SIDE : CameraProfile.FALLBACK_FREE;
        if (player == null) {
            return fallback;
        }
        return player.registryAccess().lookup(ModRegistries.CAMERA_PROFILE)
                .flatMap(registry -> registry.get(ResourceKey.create(ModRegistries.CAMERA_PROFILE, id)))
                .map(holder -> holder.value())
                .orElse(fallback);
    }

    /**
     * The P-meter, 0..{@code PMeter.STEPS}, as last sent by the server.
     *
     * <p>Client-side and transient, matching what it is: a display value that the server
     * recomputes every tick and that means nothing without a live connection. Deliberately not
     * part of {@link CourseState}, which is serialized to disk with the player - see
     * {@code PMeterPayload}.
     */
    private static int pMeter;

    public static void setPMeter(int step) {
        pMeter = step;
    }

    public static int pMeter() {
        return pMeter;
    }

}
