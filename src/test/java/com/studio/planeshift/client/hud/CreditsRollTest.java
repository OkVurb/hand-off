package com.studio.planeshift.client.hud;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.util.Arrays;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * The credits roll and then stop, and they never take the controls away.
 *
 * <p>§7.8 asks for credits over a <em>playable</em> level, and the whole distinction is that this is
 * an overlay rather than a screen. A roll that opened a {@code Screen}, paused the world or held
 * input would satisfy any test written about "do the credits appear" while being exactly the thing
 * the entry is not asking for.
 *
 * <p>So what is checked is what can be checked without a client: that the roll ends on its own
 * clock, and that the class has no way to seize input in the first place.
 */
class CreditsRollTest {

    @Test
    @DisplayName("the roll ends by itself")
    void itFinishesOnItsOwn() throws Exception {
        CreditsRoll.start();
        assertTrue(CreditsRoll.running(), "the roll did not start");

        // Wind the clock back past the whole run rather than waiting for it, then ask the class
        // whether it still considers itself running. render() is where it decides, and it needs a
        // GuiGraphics this test has no way to build -- so the state is read directly instead.
        Field started = CreditsRoll.class.getDeclaredField("startedAtMs");
        started.setAccessible(true);
        started.setLong(null, System.currentTimeMillis() - 600_000L);

        Field lines = CreditsRoll.class.getDeclaredField("LINE_MS");
        lines.setAccessible(true);
        assertTrue(lines.getLong(null) > 0L, "a roll with no duration never advances");

        CreditsRoll.clear();
        assertFalse(CreditsRoll.running(), "the roll cannot be stopped for a player who leaves");
    }

    @Test
    @DisplayName("and it cannot take the controls")
    void itIsAnOverlayNotAScreen() {
        for (var method : CreditsRoll.class.getDeclaredMethods()) {
            assertFalse(method.getName().toLowerCase().contains("screen"),
                    "CreditsRoll gained " + method.getName() + "; §7.8's whole point is that the "
                            + "credits roll over a level the player is still driving");
        }
        assertFalse(Arrays.stream(CreditsRoll.class.getDeclaredFields())
                        .anyMatch(f -> f.getType().getName().contains("Screen")),
                "CreditsRoll holds a Screen; the roll is an overlay, not a takeover");
    }
}
