package com.studio.planeshift.common.form;

import com.mojang.serialization.Codec;
import net.minecraft.util.StringRepresentable;

/**
 * The trusted, code-defined behaviours a Form may bind to.
 *
 * <p>Design Bible, "Data-driven content": "Code defines safe behaviors; data defines
 * instances, tuning, presentation, and composition." Data may pick an action and tune
 * its numbers; it can never define new executable behaviour.
 */
public enum FormActionKind implements StringRepresentable {
    /** Launch an arcing ember bolt; ignites tagged targets, lights braziers. */
    EMBER_SHOT("ember_shot"),
    /** Horizontal air dash; one charge, refreshed on landing. */
    GALE_DASH("gale_dash"),
    /** Passive: absorbs one hit, then breaks. Action re-arms after cooldown. */
    BARRIER("barrier"),
    /** Aura pulse that attracts Glints and tagged item entities. */
    MAGNET_PULSE("magnet_pulse"),
    /** Launches a bouncing fireball that ignites enemies. */
    FIRE_SHOT("fire_shot"),
    /** Launches an ice ball that slows and freezes enemies. */
    ICE_SHOT("ice_shot"),
    /** Throws an arcing hammer forward. */
    HAMMER_THROW("hammer_throw"),
    /** Throws a boomerang that damages enemies and returns. */
    BOOMERANG_THROW("boomerang_throw"),
    /** Short-range tail swipe that damages enemies in front of the player. */
    TAIL_WHACK("tail_whack"),
    /** Propeller hat launch straight up. */
    PROPELLER_SPIN("propeller_spin"),
    /** Super Acorn forward glide boost. */
    ACORN_GLIDE("acorn_glide"),
    /** Cloud Flower upward hop with slow fall. */
    CLOUD_STEP("cloud_step"),
    /** No action; presentation/stat-only Form. */
    NONE("none");

    public static final Codec<FormActionKind> CODEC = StringRepresentable.fromEnum(FormActionKind::values);

    private final String name;

    FormActionKind(String name) {
        this.name = name;
    }

    @Override
    public String getSerializedName() {
        return this.name;
    }
}
