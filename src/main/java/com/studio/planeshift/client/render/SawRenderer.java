package com.studio.planeshift.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.studio.planeshift.common.entity.SawEntity;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;

/**
 * Renderer for {@link SawEntity}.
 *
 * <p>Draws nothing, exactly like {@link FirebarRenderer} and for the same reason: the blade is a
 * ring of sparks the entity emits on the client, which already spins and costs nothing to light.
 * A model would duplicate that and then have to be kept in step with the position every tick.
 *
 * <p>It still has to exist. An entity type without a renderer crashes the client the moment one
 * comes into view.
 */
public class SawRenderer extends EntityRenderer<SawEntity, EntityRenderState> {

    public SawRenderer(EntityRendererProvider.Context context) {
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
        // Intentionally empty; the sparks are the visual.
    }
}
