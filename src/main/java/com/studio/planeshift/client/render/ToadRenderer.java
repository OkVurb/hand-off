package com.studio.planeshift.client.render;

import com.studio.planeshift.PlaneShift;
import com.studio.planeshift.common.entity.ToadEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.resources.Identifier;

/**
 * Placeholder Toad renderer using the same blocky rig as the enemies.
 */
public class ToadRenderer extends MobRenderer<ToadEntity, LivingEntityRenderState, AnimatedCourseEnemyModel> {

    private static final Identifier TEXTURE = PlaneShift.id("textures/entity/toad.png");

    public ToadRenderer(EntityRendererProvider.Context context) {
        super(context, new AnimatedCourseEnemyModel(
                context.bakeLayer(AnimatedCourseEnemyModel.LAYER_LOCATION),
                EnemyRigProfile.VILLAGER), 0.5F);
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
        return 0.5F;
    }
}
