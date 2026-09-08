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
            case EMBER_BOLT, FIREBALL, BOWSER_FIRE, LAVA_JET -> 15;
            // The car is a dark shape against a dark ceiling. Lighting it is what makes the
            // wind-up visible at all, and the wind-up is the only warning the flash gets.
            case CLOWN_CAR -> entity instanceof com.studio.planeshift.common.entity.ClownCarEntity car
                    && car.charging() ? 15 : super.getBlockLightLevel(entity, pos);
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
            case EMBER_BOLT, CLOWN_CAR -> { }
            case LAVA_JET -> {
                // Pointed by the entity's own facing rather than by its rotation: a jet does not
                // travel, so yRot means nothing to it and the wall nozzles need to aim sideways.
                switch (state.jetFacing) {
                    case 1 -> poseStack.mulPose(Axis.ZP.rotationDegrees(-90.0F));
                    case 2 -> poseStack.mulPose(Axis.ZP.rotationDegrees(90.0F));
                    default -> { }
                }
                // Grown along its own length only. Scaling all three axes would make the wind-up
                // read as something approaching the camera rather than as a column rising.
                poseStack.scale(1.0F, Math.max(0.02F, state.jetExtension), 1.0F);
            }
        }
        float scale = switch (profile) {
            case LAVA_JET -> 1.0F;
            // Big enough to hang over the approach and be unmistakable from the far end of it.
            case CLOWN_CAR -> 2.0F;
            case BOWSER_FIRE -> 0.72F;
            default -> 0.58F;
        };
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
        state.glowing = entity instanceof com.studio.planeshift.common.entity.ClownCarEntity car
                && car.charging();
        if (entity instanceof com.studio.planeshift.common.entity.LavaJetEntity jet) {
            state.jetExtension = jet.extension() * jet.reach();
            state.jetFacing = switch (jet.direction()) {
                case EAST -> 1;
                case WEST -> 2;
                default -> 0;
            };
        }
        state.xRot = entity.getXRot(partialTick);
        state.yRot = entity.getYRot(partialTick);
    }

    public static <T extends Entity> EntityRendererProvider<T> provider(
            Identifier texture, ProjectileVisualProfile profile) {
        return context -> new BespokeProjectileRenderer<>(context, texture, profile);
    }
}
