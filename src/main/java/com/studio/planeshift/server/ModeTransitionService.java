package com.studio.planeshift.server;

import com.studio.planeshift.PlaneShift;
import com.studio.planeshift.common.course.CourseState;
import com.studio.planeshift.common.mode.ModeTransaction;
import com.studio.planeshift.common.mode.PlaneMode;
import com.studio.planeshift.common.mode.PlaneRail;
import com.studio.planeshift.common.mode.PlayState;
import com.studio.planeshift.common.mode.TransitionSync;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * The four-step server-authorized mode transaction (Design Bible, "Mode transition
 * transaction"):
 *
 * <ol>
 *   <li><b>Map momentum</b> — vertical velocity is preserved; horizontal velocity is
 *       flattened onto the destination travel basis; unsafe impulses are clamped.</li>
 *   <li><b>Check clearance</b> — the destination capsule is swept; if blocked, the
 *       authored fallback socket (above the gate) is tried; otherwise the request is
 *       rejected before any state changes.</li>
 *   <li><b>Blend presentation</b> — the client receives the transaction through the
 *       synced attachment and blends the camera; collision stays in the old basis.</li>
 *   <li><b>Commit or roll back</b> — at the commit tick the server applies the new mode,
 *       rail and corrected transform. Damage, timeout or disconnect rolls back to the
 *       last stable state.</li>
 * </ol>
 */
public final class ModeTransitionService {

    /** Per-player cooldown between accepted gate triggers (prevents seam flapping). */
    private static final int GATE_COOLDOWN_TICKS = 30;
    /** Maximum distance squared between player and the gate that claims the trigger. */
    private static final double MAX_GATE_DISTANCE_SQ = 9.0D;

    private static final Map<UUID, ModeTransaction> ACTIVE = new HashMap<>();
    private static final Map<UUID, Long> LAST_ACCEPTED = new HashMap<>();
    private static long nextTransactionId = 1L;

    private ModeTransitionService() {
    }

    /** Entry point for shift gates. Every check happens here, server-side. */
    public static void requestGateShift(ServerPlayer player, BlockPos gatePos, PlaneMode target, Direction gateFacing) {
        long now = player.level().getGameTime();
        UUID id = player.getUUID();

        CourseState state = CourseStateAccess.get(player);
        if (!state.state().acceptsShiftRequests() || ACTIVE.containsKey(id)) {
            return;
        }
        if (state.mode() == target) {
            return;
        }
        if (now - LAST_ACCEPTED.getOrDefault(id, -(long) GATE_COOLDOWN_TICKS) < GATE_COOLDOWN_TICKS) {
            return;
        }
        if (player.distanceToSqr(Vec3.atCenterOf(gatePos)) > MAX_GATE_DISTANCE_SQ) {
            return;
        }
        if (gateFacing.getAxis() == Direction.Axis.Y) {
            PlaneShift.LOGGER.debug("Shift rejected for {}: vertical gate facing {}", player.getName().getString(), gateFacing);
            return;
        }

        Optional<PlaneRail> rail = target == PlaneMode.SIDE_ON
                ? Optional.of(PlaneRail.fromGate(Vec3.atCenterOf(gatePos), gateFacing))
                : Optional.empty();

        // Step 2: clearance. Destination position in the new basis, else fallback socket.
        Vec3 destination = rail.map(r -> r.snapToPlane(player.position())).orElse(player.position());
        if (!hasClearance(player, destination)) {
            Vec3 fallback = Vec3.atBottomCenterOf(gatePos.above());
            destination = rail.map(r -> r.snapToPlane(fallback)).orElse(fallback);
            if (!hasClearance(player, destination)) {
                PlaneShift.LOGGER.debug("Shift rejected for {}: no clearance at gate {}", player.getName().getString(), gatePos);
                return;
            }
        }

        LAST_ACCEPTED.put(id, now);
        long txId = nextTransactionId++;
        ModeTransaction tx = new ModeTransaction(
                txId, state.mode(), target, rail,
                player.position(), destination,
                player.level().dimension(),
                now, now + ModeTransaction.DEFAULT_DURATION_TICKS);
        ACTIVE.put(id, tx);

        CourseStateAccess.update(player, s -> s.withTransition(
                Optional.of(new TransitionSync(txId, s.mode(), target, rail, tx.requestTick(), tx.commitTick()))));
        player.level().playSound(null, gatePos, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.BLOCKS, 1.0F, 0.8F);
    }

    /** Debug/operator entry: an immediate transaction from the player's own position. */
    public static void requestManualShift(ServerPlayer player, PlaneMode target) {
        Direction facing = Direction.fromYRot(player.getYRot() + 90.0F);
        requestGateShift(player, player.blockPosition(), target, facing);
    }

    /** Ticked once per server tick: commits due transactions, aborts stale ones. */
    public static void tick(MinecraftServer server) {
        if (ACTIVE.isEmpty()) {
            return;
        }
        Iterator<Map.Entry<UUID, ModeTransaction>> iterator = ACTIVE.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, ModeTransaction> entry = iterator.next();
            ServerPlayer player = server.getPlayerList().getPlayer(entry.getKey());
            ModeTransaction tx = entry.getValue();

            if (player == null) {
                // Disconnect mid-transition: resolve to last stable state on the stored
                // snapshot; the login handler re-sanitizes.
                iterator.remove();
                continue;
            }
            if (!player.level().dimension().equals(tx.sourceLevel())) {
                CourseStateAccess.update(player, s -> s
                        .withTransition(Optional.empty())
                        .withMode(tx.from(), s.rail())
                        .withState(PlayState.playingFor(tx.from())));
                iterator.remove();
                continue;
            }
            long now = player.level().getGameTime();
            if (tx.timedOut(now)) {
                rollback(player, tx);
                iterator.remove();
            } else if (tx.readyToCommit(now)) {
                commit(player, tx);
                iterator.remove();
            }
        }
    }

    /** Damage or other invalidation during the blend aborts the transaction. */
    public static void abortIfActive(ServerPlayer player) {
        ModeTransaction tx = ACTIVE.remove(player.getUUID());
        if (tx != null) {
            rollback(player, tx);
        }
    }

    public static boolean isTransitioning(ServerPlayer player) {
        return ACTIVE.containsKey(player.getUUID());
    }

    private static void commit(ServerPlayer player, ModeTransaction tx) {
        if (!player.level().dimension().equals(tx.sourceLevel())) {
            CourseStateAccess.update(player, s -> s.withTransition(Optional.empty()));
            return;
        }
        // Step 1 applied at commit: preserve vertical velocity, flatten horizontal onto
        // the destination basis, clamp unsafe impulses.
        Vec3 velocity = player.getDeltaMovement();
        Vec3 mapped = tx.rail().map(r -> r.flattenVelocity(velocity)).orElse(velocity);
        if (mapped.lengthSqr() > 4.0D) {
            mapped = mapped.normalize().scale(2.0D);
        }

        Vec3 commitPos = tx.rail().map(r -> r.snapToPlane(player.position())).orElse(player.position());
        if (!hasClearance(player, commitPos)) {
            commitPos = tx.commitPos();
        }

        player.teleportTo(commitPos.x, commitPos.y, commitPos.z);
        player.setDeltaMovement(mapped);
        player.hurtMarked = true;

        CourseStateAccess.update(player, s -> s
                .withMode(tx.to(), tx.rail())
                .withState(PlayState.playingFor(tx.to())));
        PlaneShift.LOGGER.debug("Committed tx {} for {}: {} -> {}", tx.id(),
                player.getName().getString(), tx.from(), tx.to());
    }

    private static void rollback(ServerPlayer player, ModeTransaction tx) {
        if (!player.level().dimension().equals(tx.sourceLevel())) {
            CourseStateAccess.update(player, s -> s.withTransition(Optional.empty()));
            return;
        }
        player.teleportTo(tx.startPos().x, tx.startPos().y, tx.startPos().z);
        CourseStateAccess.update(player, s -> s
                .withTransition(Optional.empty())
                .withMode(tx.from(), s.rail())
                .withState(tx.from() == PlaneMode.SIDE_ON && s.rail().isPresent()
                        ? PlayState.PLAYING_2_5D
                        : (!s.isHub() ? PlayState.PLAYING_3D : PlayState.HUB)));
        PlaneShift.LOGGER.debug("Rolled back tx {} for {}", tx.id(), player.getName().getString());
    }

    private static boolean hasClearance(ServerPlayer player, Vec3 feetPos) {
        AABB capsule = player.getDimensions(player.getPose())
                .makeBoundingBox(feetPos)
                .inflate(-0.05D, 0.0D, -0.05D);
        return player.level().noCollision(player, capsule);
    }

    /** Server stopping / player logout hygiene. */
    public static void clear(UUID playerId) {
        ACTIVE.remove(playerId);
        LAST_ACCEPTED.remove(playerId);
    }
}
