package com.studio.planeshift.server;

import com.studio.planeshift.common.course.CourseState;
import com.studio.planeshift.common.registry.ModParticles;
import com.studio.planeshift.common.registry.ModSounds;
import java.util.Collections;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;

/**
 * Checkpoint activation and recovery (Design Bible, "Combat, stomp, damage, and
 * recovery"): "From ordinary death to controllable checkpoint spawn: under three
 * seconds on a warm client."
 */
public final class CheckpointService {

    private CheckpointService() {
    }

    /** @return true when this beacon became the player's checkpoint just now */
    private static void fallbackHub(ServerPlayer player) {
        CourseService.returnToHub(player);
    }

    public static boolean activate(ServerPlayer player, BlockPos beaconPos) {
        GlobalPos checkpoint = GlobalPos.of(player.level().dimension(), beaconPos);
        CourseState state = CourseStateAccess.get(player);
        if (state.checkpoint().map(checkpoint::equals).orElse(false)) {
            return false;
        }
        CourseStateAccess.update(player, s -> s
                .withCheckpoint(Optional.of(checkpoint))
                .withPips(CourseState.MAX_PIPS, s.invulnUntil()));
        player.level().playSound(null, beaconPos, ModSounds.CHECKPOINT.get(),
                SoundSource.BLOCKS, 0.7F, 1.5F);
        if (player.level() instanceof ServerLevel level) {
            level.sendParticles(ModParticles.PICKUP_GLOW.get(),
                    beaconPos.getX() + 0.5D, beaconPos.getY() + 0.5D, beaconPos.getZ() + 0.5D,
                    8, 0.2D, 0.4D, 0.2D, 0.05D);
        }
        return true;
    }

    /**
     * Returns the player to their checkpoint (or world spawn as documented fallback).
     * Fast, damage-free teleport — recovery must stay under the three-second budget.
     */
    public static void returnToCheckpoint(ServerPlayer player) {
        CourseState state = CourseStateAccess.get(player);
        Optional<GlobalPos> checkpoint = state.checkpoint();
        MinecraftServer server = player.level().getServer();
        if (server == null) {
            return;
        }

        if (checkpoint.isPresent()) {
            GlobalPos cp = checkpoint.get();
            ServerLevel level = server.getLevel(cp.dimension());
            if (level != null) {
                Vec3 target = Vec3.atBottomCenterOf(cp.pos().above());
                // Land on the committed rail if 2.5D play is active.
                Vec3 snapped = state.in2_5D() && state.rail().isPresent()
                        ? state.rail().get().snapToPlane(target)
                        : target;
                player.teleportTo(level, snapped.x, snapped.y, snapped.z,
                        Collections.emptySet(), 0.0F, 0.0F, false);
            } else {
                fallbackHub(player);
            }
        } else {
            fallbackHub(player);
        }

        if (player.level() instanceof ServerLevel level) {
            level.playSound(null, player.blockPosition(), ModSounds.WARP.get(), SoundSource.PLAYERS, 0.6F, 1.0F);
            level.sendParticles(ModParticles.RESPAWN_WARP.get(),
                    player.getX(), player.getY(0.5D), player.getZ(),
                    12, 0.3D, 0.4D, 0.3D, 0.06D);
        }
        player.setDeltaMovement(Vec3.ZERO);
        player.hurtMarked = true;
        player.resetFallDistance();
    }
}
