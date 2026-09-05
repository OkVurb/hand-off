package com.studio.planeshift.common.entity;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * The defeat matrix, asserted as a design contract.
 *
 * <p>These run against the enum rather than against live entities, because constructing an
 * {@code EntityType} outside registration is not possible in a plain JUnit classloader. That limits
 * what can be checked here to the shape of the vocabulary — the per-enemy sets are verified by
 * reading them, and the rule they must satisfy is stated in {@link DefeatVector}.
 */
class DefeatVectorTest {

    @Test
    void theAlwaysAvailableSetIsExactlyWhatAPlayerHasWithNoPowerUp() {
        // If this ever grows to include FIRE or ICE, every "is this enemy beatable" argument in
        // the codebase silently becomes wrong, because those need a power-up the player may not
        // be carrying when the generator drops them next to one.
        assertTrue(DefeatVector.ALWAYS_AVAILABLE.contains(DefeatVector.STOMP));
        assertTrue(DefeatVector.ALWAYS_AVAILABLE.contains(DefeatVector.SPIN));
        assertTrue(DefeatVector.ALWAYS_AVAILABLE.contains(DefeatVector.GROUND_POUND));
        assertFalse(DefeatVector.ALWAYS_AVAILABLE.contains(DefeatVector.FIRE));
        assertFalse(DefeatVector.ALWAYS_AVAILABLE.contains(DefeatVector.ICE));
        assertFalse(DefeatVector.ALWAYS_AVAILABLE.contains(DefeatVector.SHELL));
        assertFalse(DefeatVector.ALWAYS_AVAILABLE.contains(DefeatVector.STAR),
                "star power is the rarest thing in the game; it cannot be an enemy's only answer");
    }

    @Test
    void everyVectorIsDistinctAndNamed() {
        assertTrue(DefeatVector.values().length >= 7);
        for (DefeatVector v : DefeatVector.values()) {
            assertFalse(v.name().isBlank());
        }
    }
}
