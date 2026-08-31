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
}
