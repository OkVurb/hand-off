package com.studio.planeshift.server;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.studio.planeshift.common.course.CourseTheme;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Every theme should have some ambience, and the compiler cannot say so.
 *
 * <p>The ambient particle pass is a chain of {@code else if} on theme. Adding a theme -- as water
 * was added tonight -- silently falls off the end of that chain, and the result is a course that is
 * completely still while every other theme drifts. Nothing fails; it simply looks dead.
 *
 * <p>So this reads the source and checks each theme is named there. That is a blunt instrument and
 * it is the right one here: the alternative is a headless client running a tick loop to observe
 * particles, which is a great deal of machinery to answer "did anyone remember the new theme".
 */
class ThemeAmbienceTest {

    private static Path root() {
        Path dir = Path.of("").toAbsolutePath();
        while (dir != null && !Files.isDirectory(dir.resolve("src"))) {
            dir = dir.getParent();
        }
        if (dir == null) {
            throw new IllegalStateException("could not find the repo root");
        }
        return dir;
    }

    @Test
    @DisplayName("no theme is left without ambient particles")
    void everyThemeIsNamed() throws IOException {
        Path src = root().resolve(
                "src/main/java/com/studio/planeshift/server/ServerEvents.java");
        String body = Files.readString(src, StandardCharsets.UTF_8);
        // Only the ambience pass, so an unrelated mention of a theme elsewhere cannot satisfy this.
        int from = body.indexOf("CourseTheme.LAVA");
        int to = body.indexOf("if (player.onGround())", from);
        String pass = body.substring(Math.max(0, from), to > from ? to : body.length());

        List<String> missing = new ArrayList<>();
        for (CourseTheme theme : CourseTheme.values()) {
            if (!pass.contains("CourseTheme." + theme.name())) {
                missing.add(theme.name());
            }
        }
        assertTrue(missing.isEmpty(),
                "themes with no ambient particles: " + missing
                        + " -- a course of that theme is completely still while every other "
                        + "theme drifts, and nothing fails to tell you");
    }
}
