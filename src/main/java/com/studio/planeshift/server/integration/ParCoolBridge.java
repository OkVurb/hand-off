package com.studio.planeshift.server.integration;

import com.studio.planeshift.common.course.CourseState;
import java.util.Optional;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;

/**
 * Lets ParCool's parkour moves work the way this game needs them to, when ParCool is installed.
 *
 * <h2>Why lean on another mod at all</h2>
 *
 * <p>The plan's traversal entry lists ropes, poles, webs and beanstalks as missing, and the honest
 * reading of that list is that most of it is not terrain — it is <em>verbs</em>. ParCool already
 * ships the verbs: wall jump, wall slide, cling, pole climb, zipline, vault, roll. Rebuilding those
 * as blocks would produce a worse version of a mod the player already has running, and one of them
 * in particular, the wall jump, is a move the reference genuinely has and this mod does not.
 *
 * <p>So the division is: ParCool owns what the player can do, and PlaneShift owns the terrain that
 * asks for it. This class is the seam.
 *
 * <h2>Stamina is the part that has to go</h2>
 *
 * <p>ParCool gates its moves on a stamina bar, which is right for a survival world and wrong here.
 * Mario has never had one, and a wall kick that fails because a meter emptied is a death the player
 * cannot read as their own mistake -- the whole design rule this project keeps coming back to.
 *
 * <p>ParCool ships the exact answer as a registered effect, {@code parcool:inexhaustible}. Applying
 * it for the duration of a course leaves every move available and leaves ParCool's own balance
 * untouched everywhere else, which matters: the player's hub and any world they build in are not
 * this game and should not inherit its rules.
 *
 * <h2>Looked up, not linked</h2>
 *
 * <p>By registry id rather than against ParCool's API, so PlaneShift neither compiles against it
 * nor ships it. When ParCool is absent the lookup misses once, is remembered as absent, and every
 * call here is a no-op -- so the mod runs identically with or without it, and nobody has to ship
 * somebody else's jar in this repository to build.
 */
public final class ParCoolBridge {

    private static final Identifier INEXHAUSTIBLE = Identifier.fromNamespaceAndPath(
            "parcool", "inexhaustible");

    /**
     * How long the effect is topped up to, and how often.
     *
     * <p>Comfortably longer than the refresh interval so it never lapses between ticks, and short
     * enough that walking out of a course drops it within a couple of seconds rather than leaving
     * the player permanently tireless in their own world.
     */
    private static final int EFFECT_TICKS = 60;
    private static final int REFRESH_INTERVAL = 20;

    /** Resolved once. {@code null} means "not looked up yet"; empty means "ParCool is not here". */
    private static Optional<Holder.Reference<MobEffect>> effect;

    private ParCoolBridge() {
    }

    /**
     * Whether ParCool is installed and its parkour moves are available.
     *
     * <p>Asked by {@code AirMoveService}, which ships a wall jump of its own that is switched off
     * by default because -- its own comment says so -- in a course packed with blocks it fires on
     * almost any airborne moment and reads as a free double jump. ParCool's is a real one: it wants
     * the player actually against a wall and facing it. Where a better implementation of a mechanic
     * is already running, shipping a worse one alongside it is two things fighting over the same
     * key.
     */
    public static boolean parkourAvailable() {
        return inexhaustible().isPresent();
    }

    /** Keeps stamina out of the way while the player is running a course. */
    public static void tick(ServerPlayer player, CourseState state) {
        if (!state.inCourse() || player.tickCount % REFRESH_INTERVAL != 0) {
            return;
        }
        inexhaustible().ifPresent(held -> player.addEffect(
                new MobEffectInstance(held, EFFECT_TICKS, 0, true, false, false)));
    }

    /** The effect, if ParCool is installed. */
    private static Optional<Holder.Reference<MobEffect>> inexhaustible() {
        if (effect == null) {
            effect = BuiltInRegistries.MOB_EFFECT.get(
                    ResourceKey.create(BuiltInRegistries.MOB_EFFECT.key(), INEXHAUSTIBLE));
        }
        return effect;
    }
}
