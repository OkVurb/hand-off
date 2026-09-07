package com.studio.planeshift.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.studio.planeshift.common.entity.ChainBallEntity;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;

/**
 * Renderer for {@link ChainBallEntity}.
 *
 * <p>Draws nothing, like the firebar and the saw. The chain is beads emitted client-side from the
 * pivot to the ball, which means the visual is derived from the same angle the damage uses and the
 * two cannot disagree -- a real model would need its own copy of the swing and a reason to trust it.
 */
public class ChainBallRenderer extends EntityRenderer<ChainBallEntity, EntityRenderState> {

    public ChainBallRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public EntityRenderState createRenderState() {
        return new EntityRenderState();
    }

    @Override
    public void submit(EntityRenderState state, PoseStack poseStack,
                       net.minecraft.client.renderer.SubmitNodeCollector collector,
                       net.minecraft.client.renderer.state.CameraRenderState camera) {
        // Intentionally empty; the chain beads are the visual.
    }
}
