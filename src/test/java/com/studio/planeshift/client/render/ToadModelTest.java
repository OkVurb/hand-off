package com.studio.planeshift.client.render;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import org.junit.jupiter.api.Test;

class ToadModelTest {

    @Test
    void shopkeeperLayerBakesAndAnimates() {
        ModelPart root = assertDoesNotThrow(() -> ToadModel.createBodyLayer().bakeRoot());
        ToadModel model = assertDoesNotThrow(() -> new ToadModel(root));
        LivingEntityRenderState state = new LivingEntityRenderState();
        state.ageInTicks = 14.0F;
        state.walkAnimationPos = 2.75F;
        state.walkAnimationSpeed = 0.7F;
        assertDoesNotThrow(() -> model.setupAnim(state));
        assertEquals(13L, root.getAllParts().stream().filter(part -> !part.isEmpty()).count());
    }
}
