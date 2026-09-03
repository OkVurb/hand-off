package com.studio.planeshift.server;

import com.studio.planeshift.PlaneShift;
import com.studio.planeshift.common.PlaneShiftConfig;
import com.studio.planeshift.common.course.CourseState;
import com.studio.planeshift.common.mode.PlaneMode;
import java.lang.reflect.Method;
import java.util.Locale;
import java.util.Set;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.Event;
import net.neoforged.bus.api.ICancellableEvent;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.common.NeoForge;

/**
 * Restricts a few ParCool abilities while the player is inside a 2.5D course.
 *
 * <p>Per course, not globally. The abilities banned here are wanted everywhere else — in the hub,
 * and especially in 3D courses, where wall running and ledge grabs are the point. Switching them
 * off in ParCool's own config file would take them away from all three, so the rule lives here
 * where it can ask what the player is currently doing.
 *
 * <p>Only two things are actually restricted, and both for the same reason: they defeat authored
 * level geometry rather than interacting with it.
 *
 * <ul>
 *   <li><b>Charge jump</b> clears far more height than the generator plans for, so a designed
 *       route becomes a skip and the reachability proof describes a level nobody plays.</li>
 *   <li><b>Hide-in-block</b> puts the player inside course geometry, where the rail constraint
 *       will fight to pull them out and the result is a stutter that reads as a PlaneShift bug.</li>
 * </ul>
 *
 * <p>Everything else ParCool does is left alone deliberately. Wall running, cat leaps, vaults and
 * fast running all <em>interact</em> with a course rather than bypassing it, and the depth-axis
 * problem people expect from them is already handled: {@code MovementRuleService} folds cross-rail
 * momentum onto the travel axis, so a dodge aimed into the screen becomes a dodge along the lane.
 *
 * <h2>Why this is all reflection</h2>
 *
 * PlaneShift must run without ParCool installed, so it cannot compile against it. Everything is
 * resolved by name at startup, once; if ParCool is absent, or a future version renames the event,
 * this logs a single line and disables itself. A compatibility layer that throws every tick when
 * the other mod updates is worse than no compatibility layer.
 */
public final class ParCoolCompat {

    private static final String MOD_ID = "parcool";
    private static final String EVENT_CLASS =
            "com.alrex.parcool.api.unstable.action.ParCoolActionEvent$Start$Pre";

    /**
     * Actions refused inside a 2.5D course, matched case-insensitively against ParCool's own
     * action class name.
     */
    private static final Set<String> RESTRICTED_IN_LANE = Set.of("chargejump", "hideinblock");

    private static boolean active;
    /** Cached accessor for whichever getter exposes the action; resolved once. */
    private static Method actionGetter;
    private static Method playerGetter;

    private ParCoolCompat() {
    }

    /** Wires the listener if ParCool is present. Called once from mod construction. */
    public static void register() {
        if (!ModList.get().isLoaded(MOD_ID)) {
            return;
        }
        try {
            Class<?> eventClass = Class.forName(EVENT_CLASS);
            actionGetter = findGetter(eventClass, "getAction", "action");
            playerGetter = findGetter(eventClass, "getPlayer", "player", "getEntity");
            if (actionGetter == null || playerGetter == null) {
                PlaneShift.LOGGER.warn(
                        "ParCool is installed but its action event looks different from expected; "
                                + "course parkour restrictions are disabled. This is safe.");
                return;
            }
            registerListener(eventClass);
            active = true;
            PlaneShift.LOGGER.info("ParCool detected: charge jump and hide-in-block will be "
                    + "restricted inside 2.5D courses only.");
        } catch (ClassNotFoundException e) {
            PlaneShift.LOGGER.info("ParCool present but its unstable action API is absent; "
                    + "no course parkour restrictions applied.");
        } catch (RuntimeException e) {
            PlaneShift.LOGGER.warn("Could not hook ParCool ({}); continuing without restrictions.",
                    e.toString());
        }
    }

    @SuppressWarnings("unchecked")
    private static <T extends Event> void registerListener(Class<?> eventClass) {
        // The type parameter has to be bounded to Event for the bus to accept it, but the actual
        // class is only known by name at runtime — hence the unchecked cast. If ParCool ever
        // renames the event, Class.forName above fails first and this is never reached.
        NeoForge.EVENT_BUS.addListener((Class<T>) eventClass, ParCoolCompat::handle);
    }

    private static Method findGetter(Class<?> owner, String... names) {
        for (String name : names) {
            try {
                Method m = owner.getMethod(name);
                m.setAccessible(true);
                return m;
            } catch (NoSuchMethodException ignored) {
                // Try the next candidate name.
            }
        }
        return null;
    }

    /**
     * Cancels a restricted action when the player is in a 2.5D course.
     *
     * <p>Wrapped so a reflection failure can never take the game down: if anything about the event
     * is not what was expected, the action is allowed. Letting a charge jump through is a design
     * annoyance; throwing here every time a player moves is a crash.
     */
    private static void handle(Object event) {
        if (!active || !PlaneShiftConfig.SERVER.restrictParkourInCourses.get()) {
            return;
        }
        try {
            if (!(playerGetter.invoke(event) instanceof ServerPlayer player)) {
                return;
            }
            CourseState state = CourseStateAccess.get(player);
            // Only the rail-constrained mode is restricted. A 3D course wants all of this.
            if (!state.inCourse() || state.mode() != PlaneMode.SIDE_ON) {
                return;
            }
            Object action = actionGetter.invoke(event);
            if (action == null) {
                return;
            }
            String name = action.getClass().getSimpleName().toLowerCase(Locale.ROOT);
            if (RESTRICTED_IN_LANE.contains(name) && event instanceof ICancellableEvent cancellable) {
                cancellable.setCanceled(true);
            }
        } catch (ReflectiveOperationException | RuntimeException e) {
            // One warning, then stay quiet: this fires on every parkour action and a per-event log
            // would bury the server in noise.
            if (active) {
                active = false;
                PlaneShift.LOGGER.warn("ParCool integration failed and has been disabled: {}",
                        e.toString());
            }
        }
    }

    /** Whether the integration is live, for the debug HUD. */
    public static boolean isActive() {
        return active;
    }

    /** True when {@code player} is somewhere parkour should be unrestricted. */
    public static boolean allowsFullParkour(Player player) {
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return true;
        }
        CourseState state = CourseStateAccess.get(serverPlayer);
        return !state.inCourse() || state.mode() != PlaneMode.SIDE_ON;
    }
}
