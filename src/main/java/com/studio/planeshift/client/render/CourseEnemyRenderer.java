package com.studio.planeshift.client.render;

import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.resources.Identifier;

/**
 * Shared placeholder renderer for all {@link com.studio.planeshift.common.entity.CourseEnemyEntity}
 * children. The texture and silhouette come from the archetype registration, keeping the
 * rig simple while still producing distinct enemy reads.
 */
public class CourseEnemyRenderer<T extends com.studio.planeshift.common.entity.CourseEnemyEntity>
        extends MobRenderer<T, LivingEntityRenderState, AnimatedCourseEnemyModel> {

    private final Identifier texture;

    public CourseEnemyRenderer(EntityRendererProvider.Context context, Identifier texture,
                               float shadowRadius, EnemyRigProfile profile) {
        super(context, new AnimatedCourseEnemyModel(
                context.bakeLayer(AnimatedCourseEnemyModel.LAYER_LOCATION), profile), shadowRadius);
        this.texture = texture;
    }

    @Override
    public Identifier getTextureLocation(LivingEntityRenderState state) {
        return texture;
    }

    @Override
    public LivingEntityRenderState createRenderState() {
        return new LivingEntityRenderState();
    }

    public static <T extends com.studio.planeshift.common.entity.CourseEnemyEntity> EntityRendererProvider<T> provider(
            Identifier texture, float shadowRadius, EnemyRigProfile profile) {
        return context -> new CourseEnemyRenderer<>(context, texture, shadowRadius, profile);
    }
}
