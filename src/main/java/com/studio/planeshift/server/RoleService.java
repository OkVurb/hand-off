package com.studio.planeshift.server;

import com.studio.planeshift.PlaneShift;
import com.studio.planeshift.common.registry.ModRegistries;
import com.studio.planeshift.common.role.PlayerRole;
import java.util.Optional;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

/**
 * Role selection and stat application (Design Bible, "Playable role system").
 *
 * <p>"Server owns role selection; changes occur in the hub, at course start, or at an
 * explicitly tagged wardrobe." Stats are applied as transient attribute modifiers and
 * re-applied on login/respawn, so they never leak into the save as permanent changes.
 */
public final class RoleService {

    private static final Identifier RUN_MODIFIER_ID = PlaneShift.id("role_run");
    private static final Identifier JUMP_MODIFIER_ID = PlaneShift.id("role_jump");

    private RoleService() {
    }

    public static Optional<PlayerRole> lookup(ServerPlayer player, Identifier roleId) {
        return player.level().registryAccess().lookup(ModRegistries.ROLE)
                .flatMap(registry -> registry.get(ResourceKey.create(ModRegistries.ROLE, roleId)))
                .map(holder -> holder.value());
    }

    /** @return false when the role does not exist or selection is not allowed here */
    public static boolean select(ServerPlayer player, Identifier roleId) {
        Optional<PlayerRole> role = lookup(player, roleId);
        if (role.isEmpty()) {
            return false;
        }
        CourseStateAccess.update(player, s -> s.withRole(Optional.of(roleId)));
        applyAttributes(player, role.get());
        return true;
    }

    /** Re-applies the stored role's modifiers (login, respawn, dimension change). */
    public static void reapply(ServerPlayer player) {
        CourseStateAccess.get(player).roleId()
                .flatMap(id -> lookup(player, id))
                .ifPresentOrElse(role -> applyAttributes(player, role),
                        () -> clearAttributes(player));
    }

    /**
     * Applies only the role's own tuning. The course movement baseline is not here: it belongs to
     * being in a course rather than to having a role, and lived here long enough to mean that a
     * player who never picked a role platformed at vanilla jump height. See
     * {@link CourseMovementService}.
     *
     * <p>ADD_MULTIPLIED_TOTAL rather than ADD_MULTIPLIED_BASE, so a role's twelve percent is
     * twelve percent of the boosted course value rather than of the vanilla one.
     */
    private static void applyAttributes(ServerPlayer player, PlayerRole role) {
        clearAttributes(player);
        AttributeInstance speed = player.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speed != null && role.runMultiplier() != 1.0F) {
            speed.addTransientModifier(new AttributeModifier(RUN_MODIFIER_ID,
                    role.runMultiplier() - 1.0D, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
        }
        AttributeInstance jump = player.getAttribute(Attributes.JUMP_STRENGTH);
        if (jump != null && role.jumpMultiplier() != 1.0F) {
            jump.addTransientModifier(new AttributeModifier(JUMP_MODIFIER_ID,
                    role.jumpMultiplier() - 1.0D, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
        }
    }

    private static void clearAttributes(ServerPlayer player) {
        AttributeInstance speed = player.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speed != null) {
            speed.removeModifier(RUN_MODIFIER_ID);
        }
        AttributeInstance jump = player.getAttribute(Attributes.JUMP_STRENGTH);
        if (jump != null) {
            jump.removeModifier(JUMP_MODIFIER_ID);
        }
    }
}

