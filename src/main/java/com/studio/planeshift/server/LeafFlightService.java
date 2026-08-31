package com.studio.planeshift.server;

import com.studio.planeshift.PlaneShift;
import com.studio.planeshift.common.course.CourseState;
import com.studio.planeshift.common.registry.ModEffects;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.neoforged.neoforge.common.NeoForgeMod;

/**
 * Super Leaf flight/glide: while the custom leaf aura is active, the player can fly
 * and always has slow falling. When the aura expires, flight is revoked.
 */
public final class LeafFlightService {

    private static final AttributeModifier LEAF_FLIGHT = new AttributeModifier(
            PlaneShift.id("leaf_flight"), 1.0D, AttributeModifier.Operation.ADD_VALUE);

    private LeafFlightService() {
    }

    public static void tick(ServerPlayer player) {
        CourseState state = CourseStateAccess.get(player);
        if (!state.inCourse()) {
            return;
        }

        boolean hasLeaf = player.hasEffect(ModEffects.LEAF_AURA);
        AttributeInstance flight = player.getAttribute(NeoForgeMod.CREATIVE_FLIGHT);
        if (flight == null) {
            return;
        }

        if (hasLeaf) {
            if (!flight.hasModifier(LEAF_FLIGHT.id())) {
                flight.addTransientModifier(LEAF_FLIGHT);
            }
            if (!player.hasEffect(MobEffects.SLOW_FALLING)) {
                player.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 100, 0, false, false, true));
            }
        } else if (flight.hasModifier(LEAF_FLIGHT.id())) {
            flight.removeModifier(LEAF_FLIGHT.id());
            if (player.mayFly()) {
                player.getAbilities().flying = false;
                player.onUpdateAbilities();
            }
        }
    }
}
