package com.studio.planeshift.client.render;

import com.studio.planeshift.PlaneShift;
import java.util.EnumMap;
import java.util.Map;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;

/** Six real voxel projectile meshes replacing camera-facing item sprites. */
public final class BespokeProjectileModel extends EntityModel<ProjectileRenderState> {

    private static final Map<ProjectileVisualProfile, ModelLayerLocation> LAYERS =
            new EnumMap<>(ProjectileVisualProfile.class);

    static {
        for (ProjectileVisualProfile profile : ProjectileVisualProfile.values()) {
            LAYERS.put(profile, new ModelLayerLocation(
                    PlaneShift.id("projectile/" + profile.name().toLowerCase()), "main"));
        }
    }

    public BespokeProjectileModel(net.minecraft.client.model.geom.ModelPart root) {
        super(root);
    }

    public static ModelLayerLocation layer(ProjectileVisualProfile profile) {
        return LAYERS.get(profile);
    }

    public static LayerDefinition createLayer(ProjectileVisualProfile profile) {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        switch (profile) {
            case EMBER_BOLT -> emberBolt(root);
            case HAMMER -> hammer(root);
            case FIREBALL -> orb(root, false);
            case ICEBALL -> iceball(root);
            case BOOMERANG -> boomerang(root);
            case BOWSER_FIRE -> bowserFire(root);
        }
        return LayerDefinition.create(mesh, 64, 64);
    }

    private static void emberBolt(PartDefinition root) {
        add(root, "core", 0, 0, -3, -3, -3, 6, 6, 6, PartPose.ZERO);
        add(root, "nose", 32, 0, -5, -2, -2, 4, 4, 4, PartPose.ZERO);
        add(root, "tail", 32, 32, 3, -1.5F, -1.5F, 5, 3, 3, PartPose.ZERO);
        add(root, "band_y", 0, 32, -3.5F, -1, -3.5F, 7, 2, 7, PartPose.ZERO);
        add(root, "band_z", 0, 32, -3.5F, -3.5F, -1, 7, 7, 2, PartPose.ZERO);
    }

    private static void hammer(PartDefinition root) {
        add(root, "handle", 32, 0, -1.5F, -1.5F, -1.5F, 3, 11, 3,
                PartPose.offsetAndRotation(0, -4, 0, 0, 0, -0.42F));
        add(root, "head", 0, 0, -6, -3, -3, 12, 6, 6,
                PartPose.offsetAndRotation(-2.3F, -5.5F, 0, 0, 0, -0.42F));
        add(root, "face", 0, 32, -1, -3.5F, -3.5F, 2, 7, 7,
                PartPose.offsetAndRotation(-8.1F, -8, 0, 0, 0, -0.42F));
        add(root, "pommel", 32, 32, -2, -2, -2, 4, 4, 4,
                PartPose.offsetAndRotation(3.3F, 4, 0, 0, 0, -0.42F));
    }

    private static void orb(PartDefinition root, boolean large) {
        float core = large ? 8 : 6;
        float half = core / 2.0F;
        add(root, "core", 0, 0, -half, -half, -half, core, core, core, PartPose.ZERO);
        float spike = large ? 5 : 3;
        add(root, "flare_x", 32, 0, -half - spike, -1, -1, core + spike * 2, 2, 2, PartPose.ZERO);
        add(root, "flare_y", 32, 32, -1, -half - spike, -1, 2, core + spike * 2, 2, PartPose.ZERO);
        add(root, "flare_z", 0, 32, -1, -1, -half - spike, 2, 2, core + spike * 2, PartPose.ZERO);
    }

    private static void iceball(PartDefinition root) {
        add(root, "core", 0, 0, -3, -3, -3, 6, 6, 6, PartPose.ZERO);
        add(root, "shard_top", 32, 0, -1.5F, -7, -1.5F, 3, 5, 3,
                PartPose.offsetAndRotation(0, 0, 0, 0, 0, 0.18F));
        add(root, "shard_bottom", 32, 0, -1.5F, 2, -1.5F, 3, 5, 3,
                PartPose.offsetAndRotation(0, 0, 0, 0, 0, -0.18F));
        add(root, "shard_left", 0, 32, -7, -1.5F, -1.5F, 5, 3, 3,
                PartPose.offsetAndRotation(0, 0, 0, 0, 0, -0.18F));
        add(root, "shard_right", 0, 32, 2, -1.5F, -1.5F, 5, 3, 3,
                PartPose.offsetAndRotation(0, 0, 0, 0, 0, 0.18F));
        add(root, "shard_front", 32, 32, -1.5F, -1.5F, -7, 3, 3, 5, PartPose.ZERO);
        add(root, "shard_back", 32, 32, -1.5F, -1.5F, 2, 3, 3, 5, PartPose.ZERO);
    }

    private static void boomerang(PartDefinition root) {
        add(root, "left_arm", 0, 0, -2, -1.5F, -8, 4, 3, 10,
                PartPose.offsetAndRotation(-1.5F, 0, 0, 0, 0.18F, 0.62F));
        add(root, "right_arm", 32, 0, -2, -1.5F, -2, 4, 3, 10,
                PartPose.offsetAndRotation(1.5F, 0, 0, 0, -0.18F, -0.62F));
        add(root, "joint", 0, 32, -3, -2, -3, 6, 4, 6, PartPose.ZERO);
        add(root, "left_edge", 32, 32, -2.5F, -2, -8, 1, 4, 9,
                PartPose.offsetAndRotation(-1.5F, 0, 0, 0, 0.18F, 0.62F));
        add(root, "right_edge", 32, 32, 1.5F, -2, -1, 1, 4, 9,
                PartPose.offsetAndRotation(1.5F, 0, 0, 0, -0.18F, -0.62F));
    }

    private static void bowserFire(PartDefinition root) {
        orb(root, true);
        add(root, "wake_1", 32, 0, 4, -3, -3, 5, 6, 6, PartPose.ZERO);
        add(root, "wake_2", 32, 32, 8, -2, -2, 5, 4, 4, PartPose.ZERO);
        add(root, "wake_3", 0, 32, 12, -1, -1, 5, 2, 2, PartPose.ZERO);
    }

    private static void add(PartDefinition root, String name, int u, int v,
                            float x, float y, float z, float width, float height, float depth,
                            PartPose pose) {
        root.addOrReplaceChild(name, CubeListBuilder.create().texOffs(u, v)
                .addBox(x, y, z, width, height, depth), pose);
    }
}
