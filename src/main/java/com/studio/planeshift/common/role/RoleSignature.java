package com.studio.planeshift.common.role;

import com.mojang.serialization.Codec;
import net.minecraft.util.StringRepresentable;

/**
 * A role's signature trait (Design Bible, "Playable role system").
 *
 * <p>"Roles offer gentle specialization without creating mandatory characters."
 * Signatures are behaviour hooks the movement and Form services check; the numeric
 * stats live in {@link PlayerRole}.
 */
public enum RoleSignature implements StringRepresentable {
    /** Balanced; fastest Form swap recovery. */
    BALANCED("balanced"),
    /** Higher jump and slow aerial correction. */
    SKY_ARC("sky_arc"),
    /** Hold jump to float for a bounded time. */
    FLOAT_GLIDE("float_glide"),
    /** Rapid acceleration and a longer ground burst. */
    GROUND_BURST("ground_burst");

    public static final Codec<RoleSignature> CODEC = StringRepresentable.fromEnum(RoleSignature::values);

    private final String name;

    RoleSignature(String name) {
        this.name = name;
    }

    @Override
    public String getSerializedName() {
        return this.name;
    }
}
