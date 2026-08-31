package com.studio.planeshift.server;

import com.studio.planeshift.PlaneShift;
import com.studio.planeshift.common.course.CourseState;
import com.studio.planeshift.common.registry.ModEffects;
import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;
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
 * <p>The change is <em>animated</em> rather than snapped. A player who takes a hit shrinks over
 * {@link #TRANSITION_TICKS}, which is what sells the damage: an instant swap reads as a rendering
 * glitch, while a visible shrink reads as losing a power-up.
 *
 * <p>The ramp is driven server-side by rewriting the attribute modifier each tick. That is
 * deliberate — the scale attribute is synced, so the client interpolates the model for free and
 * there is no separate client animation to keep in step with the server's idea of the player's
 * size. Collision follows the same curve, so the hitbox never disagrees with what is drawn.
 *
 * <p>A transient attribute modifier is used so vanilla base scale is not permanently altered.
 */
public final class PlayerSizeService {

    public static final Identifier MODIFIER_ID = PlaneShift.id("course_size");

    /** Scale offset at full shrink. Matches the original snap value. */
    private static final double SMALL_OFFSET = -0.5D;
    /** Ticks the shrink or grow takes end to end. Around a third of a second. */
    private static final int TRANSITION_TICKS = 6;
    /** Below this the modifier is dropped entirely rather than left at a rounding artefact. */
    private static final double EPSILON = 1.0E-3D;

    /** Current animated offset per player. Weak keys so a disconnect cannot pin an entry. */
    private static final Map<UUID, Double> CURRENT = new WeakHashMap<>();

    private PlayerSizeService() {
    }

    public static void apply(ServerPlayer player, CourseState state) {
        AttributeInstance scale = player.getAttribute(Attributes.SCALE);
        if (scale == null) {
            return;
        }
        if (!state.inCourse()) {
            clear(player, scale);
            return;
        }

        double target = targetOffset(player, state);
        double current = CURRENT.getOrDefault(player.getUUID(), target);

        // Step toward the target rather than jumping to it.
        double step = Math.abs(SMALL_OFFSET) / TRANSITION_TICKS;
        double next;
        if (Math.abs(target - current) <= step) {
            next = target;
        } else {
            next = current + Math.signum(target - current) * step;
        }
        CURRENT.put(player.getUUID(), next);

        if (Math.abs(next) < EPSILON) {
            scale.removeModifier(MODIFIER_ID);
        } else {
            scale.addOrUpdateTransientModifier(
                    new AttributeModifier(MODIFIER_ID, next, AttributeModifier.Operation.ADD_VALUE));
        }
        player.refreshDimensions();
    }

    /**
     * Where the scale should end up. Mega and Mini auras own the player's size while active, so
     * the pip-driven shrink stands down rather than fighting them.
     */
    private static double targetOffset(ServerPlayer player, CourseState state) {
        if (player.hasEffect(ModEffects.MEGA_AURA) || player.hasEffect(ModEffects.MINI_AURA)) {
            return 0.0D;
        }
        return state.pips() == CourseState.MAX_PIPS ? 0.0D : SMALL_OFFSET;
    }

    private static void clear(ServerPlayer player, AttributeInstance scale) {
        CURRENT.remove(player.getUUID());
        scale.removeModifier(MODIFIER_ID);
        player.refreshDimensions();
    }

    /** Drops the cached ramp, so a rejoin starts at the correct size instead of mid-shrink. */
    public static void forget(UUID playerId) {
        CURRENT.remove(playerId);
    }
}
