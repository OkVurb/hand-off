package com.studio.planeshift.client.render;

import net.minecraft.client.renderer.entity.state.EntityRenderState;

/** Rotation snapshot consumed by bespoke projectile renderers. */
public final class ProjectileRenderState extends EntityRenderState {
    public float xRot;
    public float yRot;

    /**
     * Whether the prop should be drawn lit.
     *
     * <p>Only the clown car sets it, to glow while its flash winds up. It lives here rather than in
     * a second render state because a state class holding one unused boolean is cheaper than a
     * parallel hierarchy holding one used one.
     */
    public boolean glowing;

    /**
     * How far a lava jet's column currently reaches, in blocks.
     *
     * <p>Already multiplied by the jet's reach when it is extracted, so the renderer scales by one
     * number rather than holding two and multiplying them at draw time -- which is the sort of
     * arithmetic that ends up done differently in two places.
     */
    public float jetExtension;

    /** Which way that column points: 0 up, 1 east, 2 west. */
    public int jetFacing;
}
