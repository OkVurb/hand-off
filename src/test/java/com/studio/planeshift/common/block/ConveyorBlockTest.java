package com.studio.planeshift.common.block;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * The belt used to add a fixed impulse every tick while the entity was under a threshold, which
 * is an accelerator rather than a conveyor: it ramped anything standing on it up to the threshold
 * and pinned it there. These tests pin down the two properties that make it a belt instead.
 */
class ConveyorBlockTest {

    private static final double TARGET = 0.035D;

    @Test
    @DisplayName("a stationary entity is pulled toward the belt speed")
    void pullsFromRest() {
        double v = ConveyorBlock.ease(0.0D, TARGET);
        assertTrue(v > 0.0D, "the belt must actually move something at rest");
        assertTrue(v < TARGET, "and must not jump straight to full speed");
    }

    @ParameterizedTest
    @ValueSource(doubles = {-0.9D, -0.2D, 0.0D, 0.01D, 0.035D, 0.4D, 2.0D})
    @DisplayName("the belt converges on its target speed from any starting velocity")
    void convergesFromEitherSide(double start) {
        double v = start;
        for (int tick = 0; tick < 200; tick++) {
            v = ConveyorBlock.ease(v, TARGET);
        }
        assertEquals(TARGET, v, 1.0e-6D, "did not settle on the belt speed from " + start);
    }

    @ParameterizedTest
    @ValueSource(doubles = {-0.9D, 0.0D, 0.4D, 2.0D})
    @DisplayName("the belt never overshoots its target")
    void neverOvershoots(double start) {
        double v = start;
        boolean below = start < TARGET;
        for (int tick = 0; tick < 200; tick++) {
            v = ConveyorBlock.ease(v, TARGET);
            if (below) {
                assertTrue(v <= TARGET + 1.0e-9D, "overshot upward on tick " + tick);
            } else {
                assertTrue(v >= TARGET - 1.0e-9D, "overshot downward on tick " + tick);
            }
        }
    }

    /**
     * The property that fixes the reported bug. A player walking against the belt moves far faster
     * than the belt's target, so the belt can only ever slow them — it cannot reverse them, and it
     * cannot stop them reaching a block they are walking toward.
     */
    @Test
    @DisplayName("a player walking against the belt still wins")
    void walkingAgainstTheBeltWins() {
        // Course running speed is around 0.2 blocks per tick; the belt targets 0.035 the other way.
        double playerVelocity = -0.2D;
        double afterOneTick = ConveyorBlock.ease(playerVelocity, TARGET);

        assertTrue(afterOneTick < 0.0D,
                "one tick on the belt must not reverse a running player");
        assertTrue(Math.abs(afterOneTick) > TARGET,
                "a running player must still outpace the belt");
    }

    @Test
    @DisplayName("a belt configured to zero speed brings an entity to a stop rather than throwing it")
    void zeroSpeedBeltStops() {
        double v = 0.5D;
        for (int tick = 0; tick < 200; tick++) {
            v = ConveyorBlock.ease(v, 0.0D);
        }
        assertEquals(0.0D, v, 1.0e-6D);
    }
}
