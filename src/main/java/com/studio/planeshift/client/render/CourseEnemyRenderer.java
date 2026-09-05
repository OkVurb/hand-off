package com.studio.planeshift.client.render;

import com.studio.planeshift.common.entity.EnemyRigProfile;
import com.mojang.blaze3d.vertex.PoseStack;
import com.studio.planeshift.common.entity.CourseEnemyEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.Identifier;

/**
 * Shared renderer plumbing for the bespoke course-enemy meshes. Geometry and animation come
 * from the archetype's independently baked {@link BespokeEnemyModel} layer; this class only owns
 * the texture, shadow and the cross-cast stomp squash.
 *
 * <p>Also applies the stomp squish. Doing it here rather than in each model means every enemy
 * gets the squash for free, including ones added later.
 */
public class CourseEnemyRenderer<T extends CourseEnemyEntity>
        extends MobRenderer<T, CourseEnemyRenderState, BespokeEnemyModel> {

    private final Identifier texture;
    private final float visualScale;

    public CourseEnemyRenderer(EntityRendererProvider.Context context, Identifier texture,
                               float shadowRadius, EnemyRigProfile profile) {
        super(context, new BespokeEnemyModel(
                context.bakeLayer(BespokeEnemyModel.layer(profile)), profile), shadowRadius);
        this.texture = texture;
        this.visualScale = profile.visualScale();
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
        state.inShell = (entity instanceof com.studio.planeshift.common.entity.KoopaEntity k) && k.inShell();
    }

    @Override
    protected void scale(CourseEnemyRenderState state, PoseStack poseStack) {
        super.scale(state, poseStack);
        // The side camera commonly frames 20-30 blocks, at which a collision-accurate small mob
        // is a couple of featureless pixels, so the art is drawn larger than it is built.
        //
        // The same factor is applied to the registered entity size in ModEntities. It has to be:
        // this used to scale the drawing only, so a Koopa had a visible quarter-block of shell
        // that no stomp could reach. See EnemyRigProfile.
        poseStack.scale(visualScale, visualScale, visualScale);
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
