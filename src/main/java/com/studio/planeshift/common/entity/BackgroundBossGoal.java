package com.studio.planeshift.common.entity;

import com.studio.planeshift.common.mode.PlaneRail;
import com.studio.planeshift.common.course.CourseState;
import com.studio.planeshift.server.CourseStateAccess;
import java.util.EnumSet;
import java.util.Optional;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

/**
 * Holds a boss behind the play plane and lets it reach forward into it.
 *
 * <p>The reference fights its last boss this way: it fills the screen, stands behind the lane the
 * player is running along, and reaches in with fire and grabs while the player works across small
 * platforms. Nothing about it is a 2D idea -- it is the one boss staging that a side-on 3D game can
 * do better than the flat game it is imitating, because the depth it uses is real depth rather than
 * a sprite drawn large.
 *
 * <p>Every other boss here has fought <em>on</em> the rail, which makes it an ordinary enemy with a
 * bigger health bar: the player and the boss occupy one corridor and the fight is a shoving match
 * along a line. Standing the boss off the rail changes the question from "can I get past it" to
 * "can I read what it is about to do", which is the question a platformer boss is supposed to ask.
 *
 * <p>This goal deliberately does not attack. It owns position only -- where the boss stands and
 * which way it faces -- so that the attack goals of whatever boss it is attached to keep working
 * unchanged. A boss with this goal is a boss that stands in the backdrop; what it throws from there
 * is still its own business.
 */
public class BackgroundBossGoal extends Goal {

    /**
     * How far behind the plane the boss stands, in blocks.
     *
     * <p>Far enough that it reads as scenery the player cannot reach, close enough that it is
     * clearly the thing attacking them. Under about three blocks it looks like a mob standing
     * slightly too far back; past about eight the sense that it is looming over the course is lost
     * and it becomes part of the skybox.
     */
    private static final double BACKSET = 5.0D;

    /** How fast the boss slides toward its held depth. A snap looks like a teleport. */
    private static final double DEPTH_EASE = 0.25D;

    /** How closely it tracks the player along the course. Below 1 it drifts behind, which reads. */
    private static final double FOLLOW_SPEED = 0.85D;

    private static final double DETECT_RANGE = 48.0D;

    private final Mob boss;

    public BackgroundBossGoal(Mob boss) {
        this.boss = boss;
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        return !boss.isDeadOrDying() && rail().isPresent();
    }

    @Override
    public boolean canContinueToUse() {
        return canUse();
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    /**
     * The rail the nearest player is committed to.
     *
     * <p>Taken from the player rather than stored on the boss because the rail is a property of
     * the course the player is running, and a boss that outlived a mode change would otherwise go
     * on holding a plane that no longer exists. No player on a rail means no background plane to
     * stand behind, and the goal simply stops applying.
     */
    private Optional<PlaneRail> rail() {
        Player nearest = boss.level().getNearestPlayer(boss, DETECT_RANGE);
        if (!(nearest instanceof ServerPlayer player) || !player.isAlive()) {
            return Optional.empty();
        }
        CourseState state = CourseStateAccess.get(player);
        return state.in2_5D() ? state.rail() : Optional.empty();
    }

    @Override
    public void tick() {
        if (boss.level().isClientSide()) {
            return;
        }
        Optional<PlaneRail> maybeRail = rail();
        Player target = boss.level().getNearestPlayer(boss, DETECT_RANGE);
        if (maybeRail.isEmpty() || target == null || !target.isAlive()) {
            return;
        }
        PlaneRail rail = maybeRail.get();

        // Track the player along the course, but never on the depth axis: the boss shadows where
        // they are without ever closing the gap the staging depends on.
        boss.getNavigation().moveTo(target, FOLLOW_SPEED);
        boss.getLookControl().setLookAt(target, 30.0F, 30.0F);

        // A boss that is reaching into the lane owns its own depth this tick. Without this the
        // hold below would drag it back into the backdrop mid-swing, and the attack would exist
        // in the code and never once be visible.
        if (boss instanceof ReachesIn reacher && reacher.reachingIn()) {
            return;
        }

        // Ease onto the held depth. Done on position rather than through pathing because the
        // navigator does not know about the plane and would happily walk the boss into the lane.
        Vec3 pos = boss.position();
        Vec3 held = rail.behindPlane(pos, BACKSET);
        boss.setPos(pos.add(held.subtract(pos).scale(DEPTH_EASE)));

        // Kill depth momentum so a knockback cannot shove the boss through the plane and leave it
        // standing in front of the course.
        boss.setDeltaMovement(rail.flattenVelocity(boss.getDeltaMovement()));
    }
}
