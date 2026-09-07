package com.studio.planeshift.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.studio.planeshift.common.entity.VolcanicBombEntity;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;

/**
 * Renderer for {@link VolcanicBombEntity}.
 *
 * <p>Draws nothing, following the firebar, the saw and the chain ball. The rock is a lava-particle
 * trail emitted while it falls, which means it is visibly absent during the warning window -- and
 * that is correct, because during the warning there is no rock yet, only a marked landing spot.
 */
public class VolcanicBombRenderer extends EntityRenderer<VolcanicBombEntity, EntityRenderState> {

    public VolcanicBombRenderer(EntityRendererProvider.Context context) {
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
        // Intentionally empty; the falling trail is the visual.
    }
}
