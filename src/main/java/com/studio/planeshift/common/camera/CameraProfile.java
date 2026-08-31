package com.studio.planeshift.common.camera;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.studio.planeshift.common.mode.PlaneMode;

/**
 * An authored camera profile (Design Bible, "2.5D camera specification" and
 * "3D camera specification").
 *
 * <p>2.5D: "Use a perspective rail camera — not true orthographic projection — to
 * preserve Minecraft rendering compatibility while creating a clean side-on read.
 * Start with 45-60 degree vertical FOV, 8-14 block distance, and 2-4 blocks of
 * horizontal look-ahead based on velocity."
 *
 * <p>3D: "Start at 8-11 blocks from the avatar with a 30-45 degree downward pitch."
 *
 * <p>Loaded from {@code data/<ns>/planeshift/camera_profile/*.json}; codec bounds keep
 * authored values inside the comfort ranges. Client comfort settings scale within these
 * profiles, never past them.
 *
 * @param mode          the perspective this profile drives
 * @param fovDegrees    vertical field of view
 * @param distance      camera distance from the avatar, in blocks
 * @param pitchDegrees  downward pitch (0 for a pure side-on read)
 * @param lookAhead     horizontal look-ahead toward velocity, in blocks
 * @param damping       position/target damping factor per tick (0 = rigid, 1 = floaty)
 */
public record CameraProfile(
        PlaneMode mode,
        float fovDegrees,
        float distance,
        float pitchDegrees,
        float lookAhead,
        float damping
) {
    public static final Codec<CameraProfile> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            PlaneMode.CODEC.fieldOf("mode").forGetter(CameraProfile::mode),
            Codec.floatRange(30.0F, 90.0F).optionalFieldOf("fov_degrees", 55.0F)
                    .forGetter(CameraProfile::fovDegrees),
            Codec.floatRange(2.0F, 24.0F).optionalFieldOf("distance", 10.0F)
                    .forGetter(CameraProfile::distance),
            Codec.floatRange(-15.0F, 60.0F).optionalFieldOf("pitch_degrees", 0.0F)
                    .forGetter(CameraProfile::pitchDegrees),
            Codec.floatRange(0.0F, 6.0F).optionalFieldOf("look_ahead", 3.0F)
                    .forGetter(CameraProfile::lookAhead),
            Codec.floatRange(0.0F, 1.0F).optionalFieldOf("damping", 0.35F)
                    .forGetter(CameraProfile::damping)
    ).apply(instance, CameraProfile::new));

    /** Built-in fallback when a course references a missing profile. */
    public static final CameraProfile FALLBACK_SIDE =
            new CameraProfile(PlaneMode.SIDE_ON, 55.0F, 11.0F, 0.0F, 3.0F, 0.35F);
    public static final CameraProfile FALLBACK_FREE =
            new CameraProfile(PlaneMode.FREE_3D, 70.0F, 9.0F, 35.0F, 0.0F, 0.25F);
}
