package com.studio.planeshift.common.entity;

import java.util.EnumSet;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

/**
 * Holds a boss in the air above the arena floor and lets it come down at the player.
 *
 * <p>The reference fights its tower bosses from a hovering vehicle, and that is not decoration --
 * it is the whole shape of the fight. A boss on the ground occupies the same floor the player is
 * standing on, so the encounter becomes a shoving match along a line and the player's answer is to
 * walk into it. A boss in the air owns a different space: it is unreachable most of the time, and
 * the fight is about the moments it chooses to come down.
 *
 * <p>Ours were eight ground-walking mobs, which is the gap this closes.
 *
 * <h2>The dive is the whole mechanic</h2>
 *
 * <p>It hovers, tracks the player horizontally, and periodically drops. The drop is what makes it
 * hittable, so it is also what makes it fair: the player is not waiting for an opening the boss
 * withholds, they are reading a rhythm the boss keeps. A boss that dived only when it wanted to
 * would be a wall with a health bar.
 *
 * <p>Deliberately not a stomp-avoidance AI. Being stompable at the bottom of the dive is the point:
 * three stomps defeat a Koopaling, and the dive is the game handing the player their three chances
 * on a schedule they can learn.
 */
public class ClownCarGoal extends Goal {

    /**
     * How high above the floor it sits between dives, in blocks.
     *
     * <p>Above a jump, below the ceiling of a tower room. If the player could simply jump up and
     * hit it, the dive would be pointless; if it sat much higher it would leave the frame at this
     * camera distance and the fight would become a fight with something off screen.
     */
    private static final double HOVER_HEIGHT = 4.5D;

    /** Ticks between dives. Slow enough to read, quick enough not to be a waiting game. */
    private static final int DIVE_INTERVAL = 110;

    /** Ticks a dive lasts, down and back up. */
    private static final int DIVE_TICKS = 34;

    /** How fast it drifts to stay above the player. Below the player's run speed on purpose. */
    private static final double TRACK_SPEED = 0.06D;

    private static final double DETECT_RANGE = 40.0D;

    private final Mob boss;
    private double floorY = Double.NaN;
    private int timer;

    public ClownCarGoal(Mob boss) {
        this.boss = boss;
        setFlags(EnumSet.of(Flag.MOVE, Flag.JUMP));
    }

    @Override
    public boolean canUse() {
        return !boss.isDeadOrDying();
    }

    @Override
    public boolean canContinueToUse() {
        return canUse();
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void start() {
        // The floor is recorded once, where the boss was placed. Reading it every tick would let
        // the hover height creep downward as the boss descends, which ends with a flying boss
        // slowly sinking into the ground over the course of a fight.
        floorY = boss.getY();
        boss.setNoGravity(true);
    }

    @Override
    public void stop() {
        // Gravity back on, so a defeated boss falls rather than hanging in the air.
        boss.setNoGravity(false);
    }

    /** Height above the recorded floor at this point in the cycle. */
    private double heightNow() {
        int phase = timer % DIVE_INTERVAL;
        if (phase >= DIVE_TICKS) {
            return HOVER_HEIGHT;
        }
        // Down and back up on a single sine arch, so the lowest point is the middle of the dive
        // and the boss is slowest exactly where the player has to meet it.
        double t = (double) phase / DIVE_TICKS;
        return HOVER_HEIGHT * (1.0D - Math.sin(Math.PI * t));
    }

    @Override
    public void tick() {
        if (boss.level().isClientSide()) {
            return;
        }
        timer++;

        Player target = boss.level().getNearestPlayer(boss, DETECT_RANGE);
        if (target == null || !target.isAlive()) {
            return;
        }

        boss.getLookControl().setLookAt(target, 30.0F, 30.0F);

        // Track horizontally, slower than the player runs. Being outrunnable is what lets the
        // player choose where the next dive lands rather than being chased into a corner.
        double dx = Math.signum(target.getX() - boss.getX()) * TRACK_SPEED;
        double targetY = floorY + heightNow();
        double dy = (targetY - boss.getY()) * 0.4D;

        boss.setDeltaMovement(new Vec3(dx, dy, 0.0D));
        boss.hurtMarked = true;
    }

    /** Whether the boss is currently low enough to be hit. Exposed for tests and for tuning. */
    public boolean diving() {
        return timer % DIVE_INTERVAL < DIVE_TICKS;
    }
}
