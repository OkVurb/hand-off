package com.studio.planeshift.client.render;

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

/** Original lantern-cap shopkeeper mesh used by the friendly Toad NPC. */
public final class ToadModel extends EntityModel<LivingEntityRenderState> {

    public static final ModelLayerLocation LAYER_LOCATION =
            new ModelLayerLocation(PlaneShift.id("toad_shopkeeper"), "main");

    private final ModelPart head;
    private final ModelPart body;
    private final ModelPart leftArm;
    private final ModelPart rightArm;
    private final ModelPart leftLeg;
    private final ModelPart rightLeg;
    private final ModelPart lantern;
    private final ModelPart coatTail;

    public ToadModel(ModelPart root) {
        super(root);
        head = root.getChild("head");
        body = root.getChild("body");
        leftArm = root.getChild("left_arm");
        rightArm = root.getChild("right_arm");
        leftLeg = root.getChild("left_leg");
        rightLeg = root.getChild("right_leg");
        lantern = root.getChild("lantern");
        coatTail = root.getChild("coat_tail");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        root.addOrReplaceChild("body", box(0, 0, -3.5F, -7, -2.5F, 7, 7, 5),
                PartPose.offset(0, 18, 0));
        root.addOrReplaceChild("coat_tail", box(64, 40, -4.5F, -2, -3, 9, 5, 6),
                PartPose.offset(0, 18, 0));
        root.addOrReplaceChild("head", box(64, 0, -3.5F, -6, -3.5F, 7, 6, 7),
                PartPose.offset(0, 11, 0));
        root.addOrReplaceChild("cap_brim", box(0, 80, -5.5F, -1, -5.5F, 11, 2, 11),
                PartPose.offset(0, 5, 0));
        root.addOrReplaceChild("cap_crown", box(0, 80, -4, -4, -4, 8, 4, 8),
                PartPose.offset(0, 5, 0));
        root.addOrReplaceChild("cap_lamp", box(64, 80, -2, -2.5F, -0.75F, 4, 3, 1),
                PartPose.offset(0, 2, -4));
        root.addOrReplaceChild("left_arm", box(0, 40, 0, 0, -1.5F, 3, 7, 3),
                PartPose.offsetAndRotation(3.5F, 12, 0, 0, 0, -0.12F));
        root.addOrReplaceChild("right_arm", box(0, 40, -3, 0, -1.5F, 3, 7, 3),
                PartPose.offsetAndRotation(-3.5F, 12, 0, 0, 0, 0.12F));
        root.addOrReplaceChild("left_leg", box(64, 40, -1.5F, 0, -2, 3, 5, 4),
                PartPose.offset(2, 18, 0));
        root.addOrReplaceChild("right_leg", box(64, 40, -1.5F, 0, -2, 3, 5, 4),
                PartPose.offset(-2, 18, 0));
        root.addOrReplaceChild("backpack", box(64, 40, -3, -5, 0, 6, 7, 3),
                PartPose.offset(0, 15, 2.5F));
        root.addOrReplaceChild("lantern", box(64, 80, -2, 0, -2, 4, 5, 4),
                PartPose.offset(5.5F, 17, 0));
        root.addOrReplaceChild("lantern_handle", box(64, 80, -2.5F, -2, -0.5F, 5, 2, 1),
                PartPose.offset(5.5F, 17, 0));
        return LayerDefinition.create(mesh, 128, 128);
    }

    @Override
    public void setupAnim(LivingEntityRenderState state) {
        super.setupAnim(state);
        float speed = Math.min(1.0F, state.walkAnimationSpeed);
        float stride = state.walkAnimationPos * 0.72F;
        float step = Mth.cos(stride) * 0.85F * speed;
        rightLeg.xRot = step;
        leftLeg.xRot = -step;
        rightArm.xRot = -step * 0.55F;
        leftArm.xRot = step * 0.35F;
        head.yRot = state.yRot * Mth.DEG_TO_RAD;
        head.xRot = state.xRot * Mth.DEG_TO_RAD;
        body.y = Mth.sin(state.ageInTicks * 0.11F) * 0.12F;
        coatTail.xRot = -0.08F + speed * 0.12F;
        lantern.zRot = Mth.sin(state.ageInTicks * 0.16F) * 0.12F + step * 0.08F;
    }

    private static CubeListBuilder box(int u, int v, float x, float y, float z,
                                       float width, float height, float depth) {
        return CubeListBuilder.create().texOffs(u, v).addBox(x, y, z, width, height, depth);
    }
}
