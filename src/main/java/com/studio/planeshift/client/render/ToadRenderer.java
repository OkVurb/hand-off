package com.studio.planeshift.client.render;

import com.studio.planeshift.PlaneShift;
import com.studio.planeshift.common.entity.ToadEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.resources.Identifier;

/**
 * Renderer for the original lantern-cap shopkeeper.
 */
public class ToadRenderer extends MobRenderer<ToadEntity, LivingEntityRenderState, ToadModel> {

    private static final Identifier TEXTURE = PlaneShift.id("textures/entity/toad.png");

    public ToadRenderer(EntityRendererProvider.Context context) {
        super(context, new ToadModel(context.bakeLayer(ToadModel.LAYER_LOCATION)), 0.35F);
    }

    @Override
    public Identifier getTextureLocation(LivingEntityRenderState state) {
        return TEXTURE;
    }

    @Override
    public LivingEntityRenderState createRenderState() {
        return new LivingEntityRenderState();
    }

    @Override
    protected float getShadowRadius(LivingEntityRenderState state) {
        return 0.35F;
    }

    @Override
    protected void scale(LivingEntityRenderState state, PoseStack poseStack) {
        super.scale(state, poseStack);
        poseStack.scale(0.82F, 0.82F, 0.82F);
    }
}
