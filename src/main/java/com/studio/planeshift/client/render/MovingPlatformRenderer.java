package com.studio.planeshift.client.render;

import com.studio.planeshift.PlaneShift;
import com.studio.planeshift.common.entity.MovingPlatformEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.resources.Identifier;

/**
 * Placeholder renderer for the moving platform; drawn as a flat, wide slab.
 */
public class MovingPlatformRenderer extends MobRenderer<MovingPlatformEntity, LivingEntityRenderState, PlaceholderRigModel> {

    public MovingPlatformRenderer(EntityRendererProvider.Context context) {
        super(context, new PlaceholderRigModel(context.bakeLayer(PlaceholderRigModel.LAYER_LOCATION)), 0.0F);
    }

    @Override
    public Identifier getTextureLocation(LivingEntityRenderState state) {
        return PlaneShift.id("textures/entity/moving_platform.png");
    }

    @Override
    public LivingEntityRenderState createRenderState() {
        return new LivingEntityRenderState();
    }
}
