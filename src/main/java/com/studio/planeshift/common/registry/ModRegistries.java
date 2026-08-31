package com.studio.planeshift.common.registry;

import com.studio.planeshift.PlaneShift;
import com.studio.planeshift.common.camera.CameraProfile;
import com.studio.planeshift.common.course.CourseDefinition;
import com.studio.planeshift.common.form.FormDefinition;
import com.studio.planeshift.common.role.PlayerRole;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.neoforged.neoforge.registries.DataPackRegistryEvent;

/**
 * PlaneShift datapack registries (Design Bible, "Data-driven content and data
 * generation").
 *
 * <p>Course authors combine trusted components without recompiling Java. All three
 * registries use a network codec so clients always share the server's definitions —
 * roles, Forms and camera profiles are gameplay rules, not cosmetics.
 *
 * <p>Data lives at {@code data/<namespace>/planeshift/<registry>/*.json}.
 */
public final class ModRegistries {

    public static final ResourceKey<Registry<PlayerRole>> ROLE =
            ResourceKey.createRegistryKey(PlaneShift.id("role"));
    public static final ResourceKey<Registry<FormDefinition>> FORM =
            ResourceKey.createRegistryKey(PlaneShift.id("form"));
    public static final ResourceKey<Registry<CameraProfile>> CAMERA_PROFILE =
            ResourceKey.createRegistryKey(PlaneShift.id("camera_profile"));
    public static final ResourceKey<Registry<CourseDefinition>> COURSE =
            ResourceKey.createRegistryKey(PlaneShift.id("course"));

    private ModRegistries() {
    }

    public static void onNewDataPackRegistries(DataPackRegistryEvent.NewRegistry event) {
        event.dataPackRegistry(ROLE, PlayerRole.CODEC, PlayerRole.CODEC);
        event.dataPackRegistry(FORM, FormDefinition.CODEC, FormDefinition.CODEC);
        event.dataPackRegistry(CAMERA_PROFILE, CameraProfile.CODEC, CameraProfile.CODEC);
        event.dataPackRegistry(COURSE, CourseDefinition.CODEC, CourseDefinition.CODEC);
    }
}
