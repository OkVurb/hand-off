package com.studio.planeshift.common.entity;

/**
 * A background boss that leans out of the backdrop and into the lane.
 *
 * <p>{@link BackgroundBossGoal} holds a boss at a fixed depth every tick, which is what makes the
 * staging work and also means nothing else can move it on that axis. An attack that crosses the
 * depth gap therefore has to be able to say so, or the goal simply undoes it — and the goal must
 * not have to know which bosses have such an attack.
 *
 * <p>Deliberately one boolean. The goal needs to know only whether to keep its hands off this tick;
 * where the boss is going and how far is the boss's own business.
 */
public interface ReachesIn {

    /** Whether the boss is mid-lunge and owns its own depth this tick. */
    boolean reachingIn();
}
