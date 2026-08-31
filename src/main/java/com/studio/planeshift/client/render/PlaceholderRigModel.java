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

/**
 * Tier-C placeholder rig: one readable box per enemy, textured per archetype.
 *
 * <p>Production plan (Design Bible, "Staffing and effort model"): "Use placeholder
 * blocks and procedural debug art through the foundation gate, then replace Tier A
 * assets first." Silhouette differences come from per-entity dimensions and texture
 * value contrast, which is what the readability rules actually require of a graybox.
 */
public class PlaceholderRigModel extends EntityModel<LivingEntityRenderState> {

    public static final ModelLayerLocation LAYER_LOCATION =
            new ModelLayerLocation(PlaneShift.id("placeholder_enemy"), "main");

    public PlaceholderRigModel(ModelPart root) {
        super(root);
    }

    /** A single centered box sized in model units (16 units = 1 block). */
    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        root.addOrReplaceChild("body", CubeListBuilder.create()
                        .texOffs(0, 0)
                        .addBox(-6.0F, 12.0F, -6.0F, 12.0F, 12.0F, 12.0F),
                PartPose.ZERO);
        return LayerDefinition.create(mesh, 64, 32);
    }
}
