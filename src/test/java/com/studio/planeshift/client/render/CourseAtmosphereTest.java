package com.studio.planeshift.client.render;

import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.studio.planeshift.common.course.CourseTheme;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * The air is a different colour in different worlds, and that is the whole point of the entry.
 *
 * <p>Plan 2.4's complaint was that aerial perspective desaturated every theme toward one grey. A
 * tint that is present but identical everywhere would satisfy any test written as "is there a
 * tint", and would be the same bug with a warmer colour — so what is checked here is that the
 * themes with a key are actually distinguishable from each other.
 */
class CourseAtmosphereTest {

    private static float[] key(CourseTheme theme) throws Exception {
        Method method = CourseAtmosphere.class.getDeclaredMethod("keyFor", CourseTheme.class);
        method.setAccessible(true);
        return (float[]) method.invoke(null, theme);
    }

    @Test
    @DisplayName("the volcano's air is warm and the snow's is cold")
    void thePolesAreOppositeColours() throws Exception {
        float[] lava = key(CourseTheme.LAVA);
        float[] snow = key(CourseTheme.SNOW);
        assertTrue(lava[0] > lava[2], "the volcano's air should be redder than it is blue");
        assertTrue(snow[2] > snow[0], "the snow's air should be bluer than it is red");
    }

    @Test
    @DisplayName("and no two tinted themes share an air colour")
    void everyKeyIsDistinct() throws Exception {
        Set<String> seen = new HashSet<>();
        for (CourseTheme theme : CourseTheme.values()) {
            float[] tint = key(theme);
            if (tint == null) {
                continue;
            }
            String signature = tint[0] + "," + tint[1] + "," + tint[2];
            assertTrue(seen.add(signature),
                    theme + " shares its air colour with another theme; one tint for every world "
                            + "is the grey this entry was about, in a different hue");
        }
        assertNotEquals(0, seen.size(), "no theme tints the air at all");
    }
}
