package com.studio.planeshift.client.render;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Map;
import net.minecraft.client.model.geom.ModelPart;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class BespokeProjectileModelTest {

    private static final Map<ProjectileVisualProfile, Long> SOLID_PARTS = Map.ofEntries(
            Map.entry(ProjectileVisualProfile.EMBER_BOLT, 5L),
            Map.entry(ProjectileVisualProfile.HAMMER, 4L),
            Map.entry(ProjectileVisualProfile.FIREBALL, 4L),
            Map.entry(ProjectileVisualProfile.ICEBALL, 7L),
            Map.entry(ProjectileVisualProfile.BOOMERANG, 5L),
            Map.entry(ProjectileVisualProfile.BOWSER_FIRE, 7L),
            // Disc, hub and four teeth. Four rather than a full rim because at this size more
            // teeth stop reading as teeth and become a fuzzy edge.
            Map.entry(ProjectileVisualProfile.GRINDER, 6L),
            // Core plus a spike on each of the six faces.
            Map.entry(ProjectileVisualProfile.SPIKED_BALL, 7L),
            // One mass and two chips. Deliberately the smallest mesh here: it is debris, and
            // debris that is too regular reads as a falling block, which means something else
            // entirely in this game.
            Map.entry(ProjectileVisualProfile.FIRE_ROCK, 3L),
            // Hull, brim, two eyes, propeller and shaft. The eyes are separate boxes rather than
            // paint because the flash comes out of them and has to be locatable before it fires.
            Map.entry(ProjectileVisualProfile.CLOWN_CAR, 6L));

    @ParameterizedTest(name = "{0} bakes as a complete projectile mesh")
    @EnumSource(ProjectileVisualProfile.class)
    void everyProjectileLayerBakes(ProjectileVisualProfile profile) {
        ModelPart root = assertDoesNotThrow(() -> BespokeProjectileModel.createLayer(profile).bakeRoot());
        assertDoesNotThrow(() -> new BespokeProjectileModel(root));
        assertEquals(SOLID_PARTS.get(profile),
                root.getAllParts().stream().filter(part -> !part.isEmpty()).count());
    }
}
