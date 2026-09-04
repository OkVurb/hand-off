package com.studio.planeshift.client.render;

import com.studio.planeshift.common.entity.EnemyRigProfile;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Map;
import net.minecraft.client.model.geom.ModelPart;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class BespokeEnemyModelTest {

    private static final Map<EnemyRigProfile, Long> SOLID_PARTS = Map.ofEntries(
            Map.entry(EnemyRigProfile.SPROUTLING, 7L),
            Map.entry(EnemyRigProfile.GECKO, 12L),
            Map.entry(EnemyRigProfile.CRUSHER, 1L),
            Map.entry(EnemyRigProfile.FLYER, 10L),
            Map.entry(EnemyRigProfile.WISP, 10L),
            Map.entry(EnemyRigProfile.RIDER, 11L),
            Map.entry(EnemyRigProfile.WARRIOR, 11L),
            Map.entry(EnemyRigProfile.CRAWLER, 14L),
            Map.entry(EnemyRigProfile.BEETLE, 12L),
            Map.entry(EnemyRigProfile.PLANT, 13L),
            Map.entry(EnemyRigProfile.BOSS, 16L));

    @ParameterizedTest(name = "{0} bakes as a complete bespoke mesh")
    @EnumSource(value = EnemyRigProfile.class, names = "VILLAGER", mode = EnumSource.Mode.EXCLUDE)
    void everyEnemyLayerBakesAndAnimates(EnemyRigProfile profile) {
        ModelPart root = assertDoesNotThrow(() -> BespokeEnemyModel.createLayer(profile).bakeRoot());
        BespokeEnemyModel model = assertDoesNotThrow(() -> new BespokeEnemyModel(root, profile));

        CourseEnemyRenderState state = new CourseEnemyRenderState();
        state.ageInTicks = 17.25F;
        state.walkAnimationPos = 3.5F;
        state.walkAnimationSpeed = 0.8F;
        state.yRot = 22.0F;
        state.xRot = -8.0F;
        assertDoesNotThrow(() -> model.setupAnim(state));

        long solid = root.getAllParts().stream().filter(part -> !part.isEmpty()).count();
        assertEquals(SOLID_PARTS.get(profile), solid,
                () -> profile + " lost or unexpectedly gained a visible cuboid group");
    }
}
