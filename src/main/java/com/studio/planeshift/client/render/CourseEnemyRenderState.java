package com.studio.planeshift.client.render;

import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;

/**
 * Render state for course enemies, carrying the stomp squish.
 *
 * <p>1.21.11 renders entities from an extracted snapshot rather than from the entity itself, so
 * anything the renderer needs has to be copied across here. The squish is two numbers because a
 * convincing squash conserves volume: the model flattens vertically and widens to match.
 */
public class CourseEnemyRenderState extends LivingEntityRenderState {

    /** Vertical scale, 1 when not squished. */
    public float squishY = 1.0F;
    /** Horizontal scale, the inverse-root companion to {@link #squishY}. */
    public float squishXZ = 1.0F;
    /** True if this is a Koopa inside its shell. */
    public boolean inShell;

    /**
     * Whether a parked shell is rocking, about to stand up.
     *
     * <p>The only warning a player standing on a shell gets that it is about to become an enemy
     * again. Derived on the server from a counter it already ticks rather than synced as its own
     * flag, so there is one source of truth for when the Koopa comes back.
     */
    public boolean wobbling;

    /** Whether a shell is mid-slide, so the renderer can spin it rather than carry it. */
    public boolean sliding;

    /**
     * Which tower boss this is, or -1 for anything that is not one.
     *
     * <p>All eight siblings are one entity type with one rig, so the sheet is the only thing that
     * says which of them the player is looking at. See {@code Koopaling}.
     */
    public int koopalingVariant = -1;
}
