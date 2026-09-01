package com.studio.planeshift.client.render;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Map;
import net.minecraft.client.model.geom.ModelPart;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class BespokeProjectileModelTest {

    private static final Map<ProjectileVisualProfile, Long> SOLID_PARTS = Map.of(
            ProjectileVisualProfile.EMBER_BOLT, 5L,
            ProjectileVisualProfile.HAMMER, 4L,
            ProjectileVisualProfile.FIREBALL, 4L,
            ProjectileVisualProfile.ICEBALL, 7L,
            ProjectileVisualProfile.BOOMERANG, 5L,
            ProjectileVisualProfile.BOWSER_FIRE, 7L);

    @ParameterizedTest(name = "{0} bakes as a complete projectile mesh")
    @EnumSource(ProjectileVisualProfile.class)
    void everyProjectileLayerBakes(ProjectileVisualProfile profile) {
        ModelPart root = assertDoesNotThrow(() -> BespokeProjectileModel.createLayer(profile).bakeRoot());
        assertDoesNotThrow(() -> new BespokeProjectileModel(root));
        assertEquals(SOLID_PARTS.get(profile),
                root.getAllParts().stream().filter(part -> !part.isEmpty()).count());
    }
}
