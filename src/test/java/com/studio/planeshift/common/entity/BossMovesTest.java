package com.studio.planeshift.common.entity;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * The two boss moves added for section 3, checked as timings rather than as behaviour.
 *
 * <p>Neither can be exercised without a level, so what is pinned here is the part that is a
 * judgement rather than a mechanism: how long the openings are. Those numbers are the fight. A stun
 * shorter than the time it takes to cross a room is the same as no stun; a claw with no wind-up is
 * not something the player dodges, it is something that happens to them.
 *
 * <p>Worth a test because they are the kind of constant that gets "tuned" later by someone reading
 * the code rather than playing the fight, and the reasoning lives in a comment two files away.
 */
class BossMovesTest {

    private static int constant(Class<?> owner, String name) throws Exception {
        Field field = owner.getDeclaredField(name);
        field.setAccessible(true);
        return field.getInt(null);
    }

    @Test
    @DisplayName("a stunned Koopaling stays down long enough to be reached")
    void theStunIsAnOpening() throws Exception {
        int stun = constant(KoopalingEntity.class, "STUN_TICKS");
        // Two seconds at twenty ticks. Long enough to cross an arena and land a stomp; the arena
        // is thirty-odd blocks and a running player covers it in about that.
        assertTrue(stun >= 30, "a " + stun + "-tick stun is not an opening, it is a stumble");
        assertTrue(stun <= 60, "a " + stun + "-tick stun lets the player stomp twice from one "
                + "mistake, which turns the fight into a wall the boss keeps running into");
    }

    @Test
    @DisplayName("and the claw is telegraphed for longer than it strikes")
    void theWindUpDominates() throws Exception {
        int windUp = constant(SuperBowserEntity.class, "WIND_UP");
        int strike = constant(SuperBowserEntity.class, "STRIKE");
        assertTrue(windUp > strike * 2,
                "a claw with a " + windUp + "-tick wind-up and a " + strike + "-tick strike is not "
                        + "something the player dodges; the warning has to dominate the blow");
    }

    @Test
    @DisplayName("and the boss is left alone by its goal for the whole swing")
    void theGoalYieldsForTheWholeMovement() throws Exception {
        int windUp = constant(SuperBowserEntity.class, "WIND_UP");
        int strike = constant(SuperBowserEntity.class, "STRIKE");
        int withdraw = constant(SuperBowserEntity.class, "WITHDRAW");
        int cycle = constant(SuperBowserEntity.class, "SWIPE_CYCLE");
        // If the movement were longer than its own cycle the boss would never finish withdrawing
        // before starting again, and would simply stand in the lane.
        assertTrue(windUp + strike + withdraw < cycle,
                "the swipe does not fit in its own cycle; the boss would live in the lane");
    }
}
