package com.studio.planeshift.server;

import com.studio.planeshift.PlaneShift;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

/**
 * Per-player cooldowns for the client-to-server payloads that do real work.
 *
 * <p>A C2S payload handler runs whatever the client sends it, as often as the client sends it.
 * Nothing in the vanilla networking layer throttles that, so a modified or scripted client can
 * call a handler every tick. The two that matter here both do more than flip a flag:
 * {@code CourseService.loadCourse} resolves a course definition and teleports across dimensions,
 * and {@code ToadShopService.purchase} moves currency. Neither should be reachable at packet rate.
 *
 * <p>Cooldowns are measured in server ticks from {@link MinecraftServer#getTickCount()} rather
 * than wall-clock time: it is monotonic, shared across dimensions (so a cooldown survives the
 * teleport {@code loadCourse} performs), and it stops advancing when the server does, so a
 * paused single-player world cannot bank up allowances.
 *
 * <p>This throttles abuse, it does not replace validation. Every handler still checks its own
 * preconditions — the limiter only bounds how often it is asked.
 */
public final class PayloadRateLimiter {

    /**
     * What each guarded payload costs. Values are deliberately loose enough that a player
     * clicking as fast as they can is never refused, and tight enough that a scripted client
     * gains nothing.
     */
    public enum Action {
        /** Course loads resolve a definition and teleport; one per second is generous. */
        LOAD_COURSE(20),
        /** Shop purchases move currency; four per second still outpaces any real click rate. */
        SHOP_PURCHASE(5);

        private final int cooldownTicks;

        Action(int cooldownTicks) {
            this.cooldownTicks = cooldownTicks;
        }
    }

    /** Player to the tick each action was last allowed, indexed by {@link Action#ordinal()}. */
    private static final Map<UUID, int[]> LAST_ALLOWED = new ConcurrentHashMap<>();

    private PayloadRateLimiter() {
    }

    /**
     * Whether {@code player} may perform {@code action} now, consuming the allowance if so.
     *
     * <p>Fails open when the server cannot be reached: a missing server means we are not in a
     * position to be handling a payload at all, and refusing every action in that state would
     * turn an unrelated fault into silent breakage.
     */
    public static boolean allow(ServerPlayer player, Action action) {
        MinecraftServer server = player.level().getServer();
        if (server == null) {
            return true;
        }
        int now = server.getTickCount();
        UUID id = player.getUUID();

        int[] slots = LAST_ALLOWED.computeIfAbsent(id, key -> {
            int[] fresh = new int[Action.values().length];
            // Far enough back that the first attempt of each action is always allowed, without
            // depending on 0 being in the past — getTickCount starts at 0.
            java.util.Arrays.fill(fresh, Integer.MIN_VALUE);
            return fresh;
        });

        synchronized (slots) {
            int last = slots[action.ordinal()];
            // Subtraction rather than comparison so this stays correct if the tick counter wraps.
            if (last != Integer.MIN_VALUE && now - last < action.cooldownTicks) {
                PlaneShift.LOGGER.debug("Rate-limited {} from {}", action, player.getName().getString());
                return false;
            }
            slots[action.ordinal()] = now;
            return true;
        }
    }

    /** Drops a player's cooldowns. Called from the logout path so the map cannot grow forever. */
    public static void forget(UUID id) {
        LAST_ALLOWED.remove(id);
    }
}
