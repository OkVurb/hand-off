package com.studio.planeshift.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.studio.planeshift.common.entity.CourseEnemyEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.Identifier;

/**
 * Shared placeholder renderer for all {@link CourseEnemyEntity} children. The texture and
 * silhouette come from the archetype registration, keeping the rig simple while still producing
 * distinct enemy reads.
 *
 * <p>Also applies the stomp squish. Doing it here rather than in each model means every enemy
 * gets the squash for free, including ones added later.
 */
public class CourseEnemyRenderer<T extends CourseEnemyEntity>
        extends MobRenderer<T, CourseEnemyRenderState, AnimatedCourseEnemyModel> {

    private final Identifier texture;

    public CourseEnemyRenderer(EntityRendererProvider.Context context, Identifier texture,
                               float shadowRadius, EnemyRigProfile profile) {
        super(context, new AnimatedCourseEnemyModel(
                context.bakeLayer(AnimatedCourseEnemyModel.LAYER_LOCATION), profile), shadowRadius);
        this.texture = texture;
    }

    @Override
    public Identifier getTextureLocation(CourseEnemyRenderState state) {
        return texture;
    }

    @Override
    public CourseEnemyRenderState createRenderState() {
        return new CourseEnemyRenderState();
    }

    @Override
    public void extractRenderState(T entity, CourseEnemyRenderState state, float partialTick) {
        super.extractRenderState(entity, state, partialTick);
        state.squishY = entity.squishScaleY(partialTick);
        state.squishXZ = entity.squishScaleXZ(partialTick);
    }

    @Override
    protected void scale(CourseEnemyRenderState state, PoseStack poseStack) {
        super.scale(state, poseStack);
        if (state.squishY < 1.0F) {
            // Scale about the feet, not the centre, so a squashed enemy stays on the ground
            // instead of sinking into it.
            poseStack.scale(state.squishXZ, state.squishY, state.squishXZ);
        }
    }

    public static <T extends CourseEnemyEntity> EntityRendererProvider<T> provider(
            Identifier texture, float shadowRadius, EnemyRigProfile profile) {
        return context -> new CourseEnemyRenderer<>(context, texture, shadowRadius, profile);
    }
}
