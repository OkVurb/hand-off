package com.studio.planeshift.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;

/** Shared render plumbing for six independently baked projectile props. */
public final class BespokeProjectileRenderer<T extends Entity>
        extends EntityRenderer<T, ProjectileRenderState> {

    private final BespokeProjectileModel model;
    private final Identifier texture;
    private final ProjectileVisualProfile profile;

    public BespokeProjectileRenderer(EntityRendererProvider.Context context, Identifier texture,
                                     ProjectileVisualProfile profile) {
        super(context);
        this.model = new BespokeProjectileModel(context.bakeLayer(BespokeProjectileModel.layer(profile)));
        this.texture = texture;
        this.profile = profile;
    }

    @Override
    protected int getBlockLightLevel(T entity, BlockPos pos) {
        return switch (profile) {
            case EMBER_BOLT, FIREBALL, BOWSER_FIRE -> 15;
            default -> super.getBlockLightLevel(entity, pos);
        };
    }

    @Override
    public void submit(ProjectileRenderState state, PoseStack poseStack,
                       SubmitNodeCollector collector, CameraRenderState camera) {
        poseStack.pushPose();
        poseStack.translate(0.0F, 0.15F, 0.0F);
        poseStack.mulPose(Axis.YP.rotationDegrees(state.yRot - 90.0F));
        poseStack.mulPose(Axis.ZP.rotationDegrees(state.xRot));
        switch (profile) {
            case HAMMER, BOOMERANG -> poseStack.mulPose(Axis.XP.rotationDegrees(state.ageInTicks * 28.0F));
            case FIREBALL, ICEBALL -> poseStack.mulPose(Axis.ZP.rotationDegrees(state.ageInTicks * 12.0F));
            case BOWSER_FIRE -> poseStack.mulPose(Axis.XP.rotationDegrees(state.ageInTicks * 8.0F));
            case EMBER_BOLT -> { }
        }
        float scale = profile == ProjectileVisualProfile.BOWSER_FIRE ? 0.72F : 0.58F;
        poseStack.scale(scale, scale, scale);
        collector.submitModel(model, state, poseStack, RenderTypes.entityCutout(texture),
                state.lightCoords, OverlayTexture.NO_OVERLAY, state.outlineColor, null);
        poseStack.popPose();
        super.submit(state, poseStack, collector, camera);
    }

    @Override
    public ProjectileRenderState createRenderState() {
        return new ProjectileRenderState();
    }

    @Override
    public void extractRenderState(T entity, ProjectileRenderState state, float partialTick) {
        super.extractRenderState(entity, state, partialTick);
        state.xRot = entity.getXRot(partialTick);
        state.yRot = entity.getYRot(partialTick);
    }

    public static <T extends Entity> EntityRendererProvider<T> provider(
            Identifier texture, ProjectileVisualProfile profile) {
        return context -> new BespokeProjectileRenderer<>(context, texture, profile);
    }
}
