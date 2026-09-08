package com.studio.planeshift.common.entity;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * A shell is a clock, and the clock has a warning on it.
 *
 * <p>A stomped Koopa that stays a shell forever is permanently neutralised, which turns a room full
 * of them into a checklist. The reference makes it a decision — kick it now, carry it, or leave it
 * and it walks again — and the whole value of that is the timer.
 *
 * <p>Checked as timings, because neither the recovery nor the wobble runs without a level. The
 * numbers are the mechanic: a recovery the player cannot outlast is not a resource, and a wobble
 * shorter than a reaction is not a warning.
 */
class ShellRecoveryTest {

    private static int constant(String name) throws Exception {
        Field field = KoopaEntity.class.getDeclaredField(name);
        field.setAccessible(true);
        return field.getInt(null);
    }

    @Test
    @DisplayName("a shell lasts long enough to be worth walking back for")
    void theShellIsAResourceNotAFlicker() throws Exception {
        int recovery = constant("RECOVERY_TICKS");
        assertTrue(recovery >= 120,
                "a " + recovery + "-tick shell is back before the player has dealt with what else "
                        + "is on screen, so it is never a thing they can go and use");
        assertTrue(recovery <= 600,
                "a " + recovery + "-tick shell never comes back in practice, which is the "
                        + "permanently-neutralised behaviour this replaced");
    }

    @Test
    @DisplayName("and it warns before it stands up")
    void theWobbleIsLongEnoughToRead() throws Exception {
        int wobble = constant("WOBBLE_TICKS");
        int recovery = constant("RECOVERY_TICKS");
        assertTrue(wobble >= 20,
                "a " + wobble + "-tick wobble is under a second; a shell that becomes an enemy "
                        + "under the player's feet with no warning is a death they cannot learn "
                        + "from");
        assertTrue(wobble < recovery,
                "the shell wobbles for its whole life, so the warning is indistinguishable from "
                        + "the resting state");
    }
}
