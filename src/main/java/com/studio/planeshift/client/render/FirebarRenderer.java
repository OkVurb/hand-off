package com.studio.planeshift.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.studio.planeshift.common.entity.FirebarEntity;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;

/**
 * Renderer for {@link FirebarEntity}.
 *
 * <p>Draws nothing itself: the bar is made of flame particles the entity emits each tick on the
 * client, which already look like fire and cost nothing to light or animate. A model here would
 * duplicate that and then have to be kept in step with the rotation.
 *
 * <p>The renderer still has to exist — an entity type without one crashes the client the moment
 * it comes into view.
 */
public class FirebarRenderer extends EntityRenderer<FirebarEntity, EntityRenderState> {

    public FirebarRenderer(EntityRendererProvider.Context context) {
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
        // Intentionally empty; the flame particles are the visual.
    }
}
