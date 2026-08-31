package com.studio.planeshift.server;

import com.studio.planeshift.PlaneShift;
import com.studio.planeshift.common.course.CourseState;
import com.studio.planeshift.common.registry.ModEffects;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

/**
 * Maps the course pip state to player scale:
 * <ul>
 *   <li>2 pips (full / big) = normal size</li>
 *   <li>1 pip (hit once) = small size</li>
 * </ul>
 *
 * <p>A transient attribute modifier is used so vanilla base scale is not permanently altered.
 */
public final class PlayerSizeService {

    public static final Identifier MODIFIER_ID = PlaneShift.id("course_size");
    public static final AttributeModifier SMALL = new AttributeModifier(
            MODIFIER_ID, -0.5D, AttributeModifier.Operation.ADD_VALUE);

    private PlayerSizeService() {
    }

    public static void apply(ServerPlayer player, CourseState state) {
        AttributeInstance scale = player.getAttribute(Attributes.SCALE);
        if (scale == null) {
            return;
        }
        if (!state.inCourse()) {
            scale.removeModifier(MODIFIER_ID);
            player.refreshDimensions();
            return;
        }
        if (player.hasEffect(ModEffects.MEGA_AURA) || player.hasEffect(ModEffects.MINI_AURA)) {
            scale.removeModifier(MODIFIER_ID);
        } else if (state.pips() == CourseState.MAX_PIPS) {
            scale.removeModifier(MODIFIER_ID);
        } else {
            scale.addOrUpdateTransientModifier(SMALL);
        }
        player.refreshDimensions();
    }
}
