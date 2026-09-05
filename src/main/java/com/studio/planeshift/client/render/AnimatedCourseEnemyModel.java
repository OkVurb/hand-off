package com.studio.planeshift.client.render;

import com.studio.planeshift.common.entity.EnemyRigProfile;
import com.studio.planeshift.PlaneShift;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.util.Mth;

/**
 * Articulated low-poly course-enemy rig.
 *
 * <p>The profile changes silhouette and animation language while retaining one authored UV
 * layout. Every profile has real model parts instead of rotating a single placeholder cube.
 */
public final class AnimatedCourseEnemyModel extends EntityModel<LivingEntityRenderState> {

    public static final ModelLayerLocation LAYER_LOCATION =
            new ModelLayerLocation(PlaneShift.id("course_enemy"), "main");

    private final EnemyRigProfile profile;
    private final ModelPart head;
    private final ModelPart body;
    private final ModelPart rightArm;
    private final ModelPart leftArm;
    private final ModelPart rightLeg;
    private final ModelPart leftLeg;
    private final ModelPart rightWing;
    private final ModelPart leftWing;

    public AnimatedCourseEnemyModel(ModelPart root, EnemyRigProfile profile) {
        super(root);
        this.profile = profile;
        this.head = root.getChild("head");
        this.body = root.getChild("body");
        this.rightArm = root.getChild("right_arm");
        this.leftArm = root.getChild("left_arm");
        this.rightLeg = root.getChild("right_leg");
        this.leftLeg = root.getChild("left_leg");
        this.rightWing = root.getChild("right_wing");
        this.leftWing = root.getChild("left_wing");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        root.addOrReplaceChild("head", CubeListBuilder.create().texOffs(32, 0)
                        .addBox(-4.0F, -8.0F, -4.0F, 8.0F, 8.0F, 8.0F),
                PartPose.offset(0.0F, 9.0F, 0.0F));
        root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 0)
                        .addBox(-5.0F, -9.0F, -3.0F, 10.0F, 9.0F, 6.0F),
                PartPose.offset(0.0F, 18.0F, 0.0F));
        root.addOrReplaceChild("right_arm", CubeListBuilder.create().texOffs(0, 16)
                        .addBox(-3.0F, 0.0F, -1.5F, 3.0F, 8.0F, 3.0F),
                PartPose.offset(-5.0F, 10.0F, 0.0F));
        root.addOrReplaceChild("left_arm", CubeListBuilder.create().texOffs(0, 16).mirror()
                        .addBox(0.0F, 0.0F, -1.5F, 3.0F, 8.0F, 3.0F),
                PartPose.offset(5.0F, 10.0F, 0.0F));
        root.addOrReplaceChild("right_leg", CubeListBuilder.create().texOffs(12, 16)
                        .addBox(-2.0F, 0.0F, -2.0F, 4.0F, 6.0F, 4.0F),
                PartPose.offset(-2.5F, 18.0F, 0.0F));
        root.addOrReplaceChild("left_leg", CubeListBuilder.create().texOffs(12, 16).mirror()
                        .addBox(-2.0F, 0.0F, -2.0F, 4.0F, 6.0F, 4.0F),
                PartPose.offset(2.5F, 18.0F, 0.0F));
        root.addOrReplaceChild("right_wing", CubeListBuilder.create().texOffs(28, 16)
                        .addBox(-1.0F, -4.0F, 0.0F, 1.0F, 8.0F, 6.0F),
                PartPose.offset(-4.5F, 12.0F, 1.5F));
        root.addOrReplaceChild("left_wing", CubeListBuilder.create().texOffs(28, 16).mirror()
                        .addBox(0.0F, -4.0F, 0.0F, 1.0F, 8.0F, 6.0F),
                PartPose.offset(4.5F, 12.0F, 1.5F));
        return LayerDefinition.create(mesh, 64, 32);
    }

    @Override
    public void setupAnim(LivingEntityRenderState state) {
        super.setupAnim(state);
        configureProfile();

        float speed = Math.min(1.0F, state.walkAnimationSpeed);
        float stride = state.walkAnimationPos * 0.72F;
        float rightStep = Mth.cos(stride) * 1.15F * speed;
        float leftStep = Mth.cos(stride + (float) Math.PI) * 1.15F * speed;
        rightLeg.xRot += rightStep;
        leftLeg.xRot += leftStep;
        rightArm.xRot -= rightStep * 0.82F;
        leftArm.xRot -= leftStep * 0.82F;
        head.yRot = state.yRot * Mth.DEG_TO_RAD;
        head.xRot = state.xRot * Mth.DEG_TO_RAD;

        float idle = Mth.sin(state.ageInTicks * 0.11F);
        body.y += idle * 0.22F;
        head.y += idle * 0.18F;
        float flap = 0.58F + Mth.sin(state.ageInTicks * 0.78F) * 0.42F;
        rightWing.zRot = -flap;
        leftWing.zRot = flap;

        switch (profile) {
            case GOOMBA -> {
                float bounce = Math.abs(Mth.sin(state.ageInTicks * 0.17F));
                body.yScale *= 0.88F + bounce * 0.12F;
                head.y -= bounce * 0.6F;
            }
            case THWOMP -> {
                float slam = Math.max(0.0F, Mth.sin(state.ageInTicks * 0.14F) - 0.72F) * 2.8F;
                rightArm.y += slam * 4.0F;
                leftArm.y += slam * 4.0F;
                body.y += slam;
            }
            case BULLET_BILL -> {
                body.xRot = (float) (Math.PI / 2.0D);
                body.y += Mth.sin(state.ageInTicks * 0.22F) * 0.5F;
            }
            case BOO -> {
                head.y += Mth.sin(state.ageInTicks * 0.16F) * 0.8F;
                head.yRot += Mth.sin(state.ageInTicks * 0.08F) * 0.18F;
            }
            case PIRANHA_PLANT -> {
                body.zRot = Mth.sin(state.ageInTicks * 0.08F) * 0.08F;
                head.zRot = Mth.sin(state.ageInTicks * 0.08F + 0.7F) * 0.12F;
            }
            case BOWSER -> {
                rightArm.zRot = -0.14F + Mth.sin(state.ageInTicks * 0.07F) * 0.06F;
                leftArm.zRot = 0.14F - Mth.sin(state.ageInTicks * 0.07F) * 0.06F;
            }
            default -> {
                // Walk, look, idle and wing channels above fully drive this profile.
            }
        }
    }

    private void configureProfile() {
        head.visible = true;
        body.visible = true;
        rightArm.visible = true;
        leftArm.visible = true;
        rightLeg.visible = true;
        leftLeg.visible = true;
        rightWing.visible = false;
        leftWing.visible = false;

        switch (profile) {
            case GOOMBA -> {
                rightArm.visible = false;
                leftArm.visible = false;
                body.xScale = 1.1F;
                body.yScale = 0.85F;
                head.xScale = 0.82F;
                head.yScale = 0.7F;
                rightLeg.yScale = 0.55F;
                leftLeg.yScale = 0.55F;
            }
            case KOOPA -> body.zScale = 0.82F;
            case THWOMP -> {
                head.visible = false;
                body.xScale = 1.35F;
                body.yScale = 1.35F;
                body.zScale = 1.25F;
                rightArm.xScale = 1.45F;
                rightArm.zScale = 1.45F;
                leftArm.xScale = 1.45F;
                leftArm.zScale = 1.45F;
                rightLeg.yScale = 0.5F;
                leftLeg.yScale = 0.5F;
            }
            case BULLET_BILL -> {
                head.visible = false;
                rightArm.visible = false;
                leftArm.visible = false;
                rightLeg.visible = false;
                leftLeg.visible = false;
                rightWing.visible = true;
                leftWing.visible = true;
                body.xScale = 0.72F;
                body.yScale = 0.72F;
                body.zScale = 1.55F;
            }
            case BOO -> {
                rightArm.visible = false;
                leftArm.visible = false;
                rightLeg.visible = false;
                leftLeg.visible = false;
                body.visible = false;
                head.xScale = 1.15F;
                head.yScale = 1.15F;
                head.zScale = 1.15F;
            }
            case LAKITU -> {
                rightWing.visible = true;
                leftWing.visible = true;
                rightWing.yScale = 0.55F;
                leftWing.yScale = 0.55F;
            }
            case HAMMER_BRO, TOAD -> {
                // Standard articulated biped proportions.
            }
            case SPINY, BUZZY_BEETLE -> {
                body.yScale = 0.62F;
                body.zScale = 1.35F;
                head.yScale = 0.72F;
                rightArm.yScale = 0.55F;
                leftArm.yScale = 0.55F;
                rightLeg.yScale = 0.55F;
                leftLeg.yScale = 0.55F;
            }
            case PIRANHA_PLANT -> {
                rightArm.visible = false;
                leftArm.visible = false;
                rightLeg.visible = false;
                leftLeg.visible = false;
                body.xScale = 0.7F;
                body.yScale = 1.45F;
                body.zScale = 0.7F;
            }
            case BOWSER -> {
                head.xScale = 1.2F;
                head.yScale = 1.1F;
                body.xScale = 1.25F;
                body.yScale = 1.18F;
                body.zScale = 1.2F;
                rightArm.xScale = 1.2F;
                leftArm.xScale = 1.2F;
            }
        }
    }
}
